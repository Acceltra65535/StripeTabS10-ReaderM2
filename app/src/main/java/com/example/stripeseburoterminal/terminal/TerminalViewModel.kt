package com.example.stripeseburoterminal.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.MobileReaderListener
import com.stripe.stripeterminal.external.callable.OfflineListener
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.stripe.stripeterminal.external.models.DisconnectReason
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.LocaleConfig
import com.stripe.stripeterminal.external.models.OfflineStatus
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentStatus
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.stripe.stripeterminal.external.models.ReaderInputOptions
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.stripe.stripeterminal.log.LogLevel

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val configStore = TerminalConfigStore(application)
    private val backendClient = TerminalBackendClient()
    private val receiptManager = ReceiptManager(application)
    private val usbMonitor = UsbDeviceMonitor(application)

    val connectionStatus = MutableLiveData("Not initialized")
    val paymentStatus = MutableLiveData("Idle")
    val readerStatus = MutableLiveData("No USB reader discovered")
    val discoveredReaders = MutableLiveData<List<Reader>>(emptyList())
    val selectedReader = MutableLiveData<Reader?>(null)
    val logLines = MutableLiveData(listOf("Configure your backend settings, then connect the USB reader."))
    val activeConfig = MutableLiveData(configStore.load())
    val isBusy = MutableLiveData(false)
    val paymentResult = MutableLiveData<PaymentResult?>(null)
    val usbDeviceConnected = MutableLiveData(usbMonitor.isUsbDeviceConnected())

    private var currentOperation: Cancelable? = null
    private var pendingPaymentIntent: PaymentIntent? = null

    private val connectionTokenProvider = object : ConnectionTokenProvider {
        override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
            Thread {
                try {
                    val token = backendClient.fetchConnectionToken(configStore.load())
                    callback.onSuccess(token)
                } catch (e: Exception) {
                    callback.onFailure(
                        ConnectionTokenException(
                            e.message ?: "Unable to fetch connection token.",
                            e,
                        )
                    )
                }
            }.start()
        }
    }

    private val terminalListener = object : TerminalListener {
        override fun onConnectionStatusChange(status: ConnectionStatus) {
            connectionStatus.postValue(status.name.lowercase().replace('_', ' '))
            appendLog("Terminal connection status: ${status.name}")
        }

        override fun onPaymentStatusChange(status: PaymentStatus) {
            paymentStatus.postValue(status.name.lowercase().replace('_', ' '))
            appendLog("Payment status: ${status.name}")
        }
    }

    private val readerListener = object : MobileReaderListener {
        override fun onDisconnect(reason: DisconnectReason) {
            connectionStatus.postValue("Reader disconnected: ${reason.name}")
            appendLog("USB reader disconnected (${reason.name})")
        }

        override fun onReaderReconnectStarted(reader: Reader, cancelReconnect: Cancelable, reason: DisconnectReason) {
            appendLog("Reconnecting to ${readerLabel(reader)} after ${reason.name}")
        }

        override fun onReaderReconnectSucceeded(reader: Reader) {
            connectionStatus.postValue("Connected to ${readerLabel(reader)}")
            appendLog("Reader reconnected: ${readerLabel(reader)}")
        }

        override fun onReaderReconnectFailed(reader: Reader) {
            appendLog("Reader reconnect failed: ${readerLabel(reader)}")
        }

        override fun onStartInstallingUpdate(update: ReaderSoftwareUpdate, cancelable: Cancelable?) {
            appendLog("Reader software update started.")
        }

        override fun onReportReaderSoftwareUpdateProgress(progress: Float) {
            appendLog("Reader software update progress: ${(progress * 100).toInt()}%")
        }

        override fun onFinishInstallingUpdate(update: ReaderSoftwareUpdate?, e: TerminalException?) {
            if (e == null) {
                appendLog("Reader software update finished.")
            } else {
                appendLog("Reader software update failed: ${e.errorMessage}")
            }
        }

        override fun onRequestReaderInput(options: ReaderInputOptions) {
            appendLog("Reader input requested: ${options.toString()}")
        }

        override fun onRequestReaderDisplayMessage(message: ReaderDisplayMessage) {
            appendLog("Reader message: ${message.toString()}")
        }
    }

    private val offlineListener = object : OfflineListener {
        override fun onOfflineStatusChange(offlineStatus: OfflineStatus) {
            appendLog("Offline status: ${offlineStatus.sdk.networkStatus.name}")
        }

        override fun onForwardingFailure(e: TerminalException) {
            appendLog("Offline forwarding failed: ${e.errorMessage}")
        }

        override fun onPaymentIntentForwarded(paymentIntent: PaymentIntent, e: TerminalException?) {
            appendLog("Offline payment forwarded: ${paymentIntent.id}")
        }
    }

    init {
        initializeTerminal()
        setupUsbMonitoring()
    }

    private fun setupUsbMonitoring() {
        usbMonitor.startMonitoring(
            onConnected = {
                appendLog("USB device connected")
                usbDeviceConnected.postValue(true)
            },
            onDisconnected = {
                appendLog("USB device disconnected")
                usbDeviceConnected.postValue(false)
            }
        )
    }

    fun refreshConfig() {
        activeConfig.postValue(configStore.load())
    }

    fun saveConfig(config: TerminalConfig) {
        configStore.save(config)
        activeConfig.postValue(config)
        appendLog("Settings saved for ${config.merchantName}.")
    }

    fun discoverUsbReaders() {
        val config = configStore.load()
        activeConfig.postValue(config)
        if (config.locationId.isBlank()) {
            appendLog("Set the Stripe Location ID in Settings first.")
            return
        }

        currentOperation?.cancel(object : com.stripe.stripeterminal.external.callable.Callback { override fun onSuccess() {} override fun onFailure(e: TerminalException) {} })
        isBusy.postValue(true)
        readerStatus.postValue("Discovering USB readers...")
        val discoveryConfig = DiscoveryConfiguration.UsbDiscoveryConfiguration(
            timeout = 0,
            isSimulated = false,
        )
        currentOperation = Terminal.getInstance().discoverReaders(
            discoveryConfig,
            object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    discoveredReaders.postValue(readers)
                    selectedReader.postValue(readers.firstOrNull())
                    readerStatus.postValue(
                        if (readers.isEmpty()) {
                            "No USB reader found"
                        } else {
                            "Discovered ${readers.size} reader(s)"
                        }
                    )
                    appendLog(
                        if (readers.isEmpty()) {
                            "No USB reader detected."
                        } else {
                            "USB reader ready: ${readers.joinToString { readerLabel(it) }}"
                        }
                    )
                }
            },
            object : com.stripe.stripeterminal.external.callable.Callback {
                override fun onSuccess() {
                    isBusy.postValue(false)
                    currentOperation = null
                    appendLog("USB discovery finished.")
                }

                override fun onFailure(e: TerminalException) {
                    isBusy.postValue(false)
                    currentOperation = null
                    readerStatus.postValue("Discovery failed")
                    appendLog("USB discovery failed: ${e.errorMessage}")
                }
            }
        )
    }

    fun connectSelectedReader() {
        val config = configStore.load()
        val reader = selectedReader.value ?: discoveredReaders.value?.firstOrNull()
        if (reader == null) {
            appendLog("Discover a USB reader first.")
            return
        }
        if (config.locationId.isBlank()) {
            appendLog("Set the Stripe Location ID in Settings first.")
            return
        }

        currentOperation?.cancel(object : com.stripe.stripeterminal.external.callable.Callback { override fun onSuccess() {} override fun onFailure(e: TerminalException) {} })
        isBusy.postValue(true)
        readerStatus.postValue("Connecting to ${readerLabel(reader)}...")
        Terminal.getInstance().connectReader(
            reader,
            ConnectionConfiguration.UsbConnectionConfiguration(
                locationId = config.locationId,
                autoReconnectOnUnexpectedDisconnect = true,
                usbReaderListener = readerListener,
            ),
            object : ReaderCallback {
                override fun onSuccess(reader: Reader) {
                    isBusy.postValue(false)
                    currentOperation = null
                    selectedReader.postValue(reader)
                    readerStatus.postValue("Connected to ${readerLabel(reader)}")
                    connectionStatus.postValue("Connected")
                    appendLog("Connected to ${readerLabel(reader)}.")
                }

                override fun onFailure(e: TerminalException) {
                    isBusy.postValue(false)
                    currentOperation = null
                    readerStatus.postValue("Connection failed")
                    appendLog("USB connection failed: ${e.errorMessage}")
                }
            }
        )
    }

    fun createPaymentIntent(amountCents: Long, currency: String) {
        val normalizedCurrency = currency.trim().ifBlank { activeConfig.value?.defaultCurrency ?: "SGD" }
        if (amountCents <= 0L) {
            appendLog("Enter a payment amount greater than zero.")
            return
        }
        isBusy.postValue(true)
        paymentStatus.postValue("Creating payment intent...")
        Thread {
            try {
                val config = configStore.load()
                val clientSecret = backendClient.createPaymentIntentClientSecret(config, amountCents, normalizedCurrency)
                Terminal.getInstance().retrievePaymentIntent(
                    clientSecret,
                    object : PaymentIntentCallback {
                        override fun onSuccess(paymentIntent: PaymentIntent) {
                            isBusy.postValue(false)
                            pendingPaymentIntent = paymentIntent
                            paymentStatus.postValue("Payment intent loaded")
                            appendLog("PaymentIntent loaded: ${paymentIntent.id}")
                        }

                        override fun onFailure(e: TerminalException) {
                            isBusy.postValue(false)
                            paymentStatus.postValue("Failed to load payment intent")
                            appendLog("PaymentIntent retrieval failed: ${e.errorMessage}")
                        }
                    }
                )
            } catch (e: Exception) {
                isBusy.postValue(false)
                paymentStatus.postValue("Failed to create payment intent")
                appendLog("Create PaymentIntent failed: ${e.message ?: "unknown error"}")
            }
        }.start()
    }

    fun collectAndConfirmPayment() {
        val paymentIntent = pendingPaymentIntent
        collectAndConfirm(paymentIntent)
    }

    fun chargePayment(amountCents: Long, currency: String) {
        val normalizedCurrency = currency.trim().ifBlank { activeConfig.value?.defaultCurrency ?: "SGD" }
        if (amountCents <= 0L) {
            appendLog("Enter a payment amount greater than zero.")
            return
        }
        isBusy.postValue(true)
        paymentStatus.postValue("Creating payment intent...")
        Thread {
            try {
                val config = configStore.load()
                val clientSecret = backendClient.createPaymentIntentClientSecret(config, amountCents, normalizedCurrency)
                Terminal.getInstance().retrievePaymentIntent(
                    clientSecret,
                    object : PaymentIntentCallback {
                        override fun onSuccess(paymentIntent: PaymentIntent) {
                            pendingPaymentIntent = paymentIntent
                            collectAndConfirm(paymentIntent)
                        }

                        override fun onFailure(e: TerminalException) {
                            isBusy.postValue(false)
                            paymentStatus.postValue("Failed to load payment intent")
                            appendLog("PaymentIntent retrieval failed: ${e.errorMessage}")
                        }
                    }
                )
            } catch (e: Exception) {
                isBusy.postValue(false)
                paymentStatus.postValue("Failed to create payment intent")
                appendLog("Create PaymentIntent failed: ${e.message ?: "unknown error"}")
            }
        }.start()
    }

    private fun collectAndConfirm(paymentIntent: PaymentIntent?) {
        if (paymentIntent == null) {
            appendLog("Load a payment intent first.")
            return
        }
        if (Terminal.getInstance().connectionStatus != ConnectionStatus.CONNECTED) {
            isBusy.postValue(false)
            appendLog("Connect the USB reader first.")
            return
        }

        isBusy.postValue(true)
        paymentStatus.postValue("Collecting payment method...")
        currentOperation?.cancel(object : com.stripe.stripeterminal.external.callable.Callback { override fun onSuccess() {} override fun onFailure(e: TerminalException) {} })
        currentOperation = Terminal.getInstance().collectPaymentMethod(
            paymentIntent,
            object : PaymentIntentCallback {
                override fun onSuccess(collectedPaymentIntent: PaymentIntent) {
                    paymentStatus.postValue("Confirming payment...")
                    currentOperation = Terminal.getInstance().confirmPaymentIntent(
                        collectedPaymentIntent,
                        object : PaymentIntentCallback {
                            override fun onSuccess(confirmedPaymentIntent: PaymentIntent) {
                                isBusy.postValue(false)
                                currentOperation = null
                                pendingPaymentIntent = confirmedPaymentIntent
                                paymentStatus.postValue("Payment completed")
                                appendLog("Payment completed: ${confirmedPaymentIntent.id}")

                                // Generate and save receipt
                                val receipt = receiptManager.generateReceipt(
                                    merchantName = configStore.load().merchantName,
                                    amount = paymentIntent.amount,
                                    currency = (paymentIntent.currency ?: "").uppercase(),
                                    paymentIntentId = confirmedPaymentIntent.id ?: "",
                                    cardLastFour = extractCardLastFour(confirmedPaymentIntent)
                                )
                                receiptManager.saveReceiptLocally(receipt, confirmedPaymentIntent.id ?: "")

                                // Update result
                                val result = PaymentResult.Success(
                                    paymentIntent = confirmedPaymentIntent,
                                    amount = paymentIntent.amount,
                                    currency = (paymentIntent.currency ?: "").uppercase(),
                                    receiptNumber = confirmedPaymentIntent.id ?: ""
                                )
                                paymentResult.postValue(result)
                                appendLog("Receipt generated and saved.")
                            }

                            override fun onFailure(e: TerminalException) {
                                isBusy.postValue(false)
                                currentOperation = null
                                pendingPaymentIntent = collectedPaymentIntent
                                paymentStatus.postValue("Payment confirmation failed")
                                appendLog("Payment confirmation failed: ${e.errorMessage}")
                                paymentResult.postValue(
                                    PaymentResult.Failure(
                                        errorMessage = e.errorMessage,
                                        errorCode = e.errorCode?.name
                                    )
                                )
                            }
                        }
                    )
                }

                override fun onFailure(e: TerminalException) {
                    isBusy.postValue(false)
                    currentOperation = null
                    paymentStatus.postValue("Payment collection failed")
                    appendLog("Payment collection failed: ${e.errorMessage}")
                    paymentResult.postValue(
                        PaymentResult.Failure(
                            errorMessage = e.errorMessage,
                            errorCode = e.errorCode?.name
                        )
                    )
                }
            }
        )
    }

    fun cancelCurrentOperation() {
        currentOperation?.cancel(object : com.stripe.stripeterminal.external.callable.Callback { override fun onSuccess() {} override fun onFailure(e: TerminalException) {} })
        currentOperation = null
        isBusy.postValue(false)
        appendLog("Current Stripe Terminal operation canceled.")
    }

    fun setSelectedReader(reader: Reader?) {
        selectedReader.postValue(reader)
        readerStatus.postValue(reader?.let { "Selected ${readerLabel(it)}" } ?: "No reader selected")
    }

    fun clearPaymentResult() {
        paymentResult.postValue(null)
    }

    override fun onCleared() {
        currentOperation?.cancel(object : com.stripe.stripeterminal.external.callable.Callback { override fun onSuccess() {} override fun onFailure(e: TerminalException) {} })
        usbMonitor.stopMonitoring()
        super.onCleared()
    }

    private fun initializeTerminal() {
        if (Terminal.isInitialized()) {
            connectionStatus.value = Terminal.getInstance().connectionStatus.name.lowercase().replace('_', ' ')
            paymentStatus.value = Terminal.getInstance().paymentStatus.name.lowercase().replace('_', ' ')
            appendLog("Stripe Terminal already initialized.")
            return
        }

        try {
            Terminal.init(
                getApplication(),
                LogLevel.VERBOSE,
                connectionTokenProvider,
                terminalListener,
                offlineListener,
                LocaleConfig.HardcodedLocale.Builder("en-US").build(),
            )
            connectionStatus.value = "Initialized"
            appendLog("Stripe Terminal initialized for Seburo Pte Ltd.")
        } catch (e: TerminalException) {
            connectionStatus.value = "Initialization failed"
            appendLog("Stripe Terminal init failed: ${e.errorMessage}")
        }
    }

    private fun appendLog(message: String) {
        val current = logLines.value.orEmpty()
        val updated = (current + message).takeLast(8)
        logLines.postValue(updated)
    }

    private fun readerLabel(reader: Reader): String {
        return listOfNotNull(reader.serialNumber, reader.id).firstOrNull().orEmpty()
    }

    private fun extractCardLastFour(paymentIntent: PaymentIntent): String? {
        return null
    }
}

