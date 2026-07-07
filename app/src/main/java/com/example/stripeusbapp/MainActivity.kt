package com.example.stripeusbapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.EditText
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.stripe.stripeterminal.external.models.PaymentStatus
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.ReaderDisplayMessage
import com.stripe.stripeterminal.external.models.ReaderInputOptions
import com.stripe.stripeterminal.external.models.ReaderSoftwareUpdate
import com.stripe.stripeterminal.external.models.BatteryStatus
import com.stripe.stripeterminal.external.models.DisconnectReason
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.log.LogLevel
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var statusTextView: TextView
    private lateinit var discoverButton: Button
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var chargeButton: Button
    private lateinit var locationIdInput: EditText

    private var discoveredReader: Reader? = null

    private fun appendLog(msg: String) {
        runOnUiThread {
            statusTextView.append("\n$msg")
        }
    }

    private val httpClient = OkHttpClient()

    private val tokenProvider = object : ConnectionTokenProvider {
        override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
            try {
                // Using the specific backend server IP provided
                val request = Request.Builder()
                    .url("http://10.0.0.253:3000/connection_token")
                    .post("".toRequestBody(null))
                    .build()

                httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()
                            if (responseBody != null) {
                                try {
                                    val jsonObject = JSONObject(responseBody)
                                    val token = jsonObject.getString("secret")
                                    appendLog("Token fetched successfully: ${token.take(10)}...")
                                    callback.onSuccess(token)
                                } catch (e: Exception) {
                                    appendLog("Token parse error: ${e.message}")
                                    callback.onFailure(ConnectionTokenException("Failed to parse token", e))
                                }
                            } else {
                                appendLog("Empty token response")
                                callback.onFailure(ConnectionTokenException("Empty response body"))
                            }
                        } else {
                            appendLog("Token req failed: ${response.code}")
                            callback.onFailure(ConnectionTokenException("Request failed: ${response.code}"))
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        appendLog("Token network error: ${e.message}")
                        callback.onFailure(ConnectionTokenException("Network request failed", e))
                    }
                })
            } catch (e: Exception) {
                appendLog("Token provider exception: ${e.message}")
                callback.onFailure(ConnectionTokenException("Failed to fetch connection token", e))
            }
        }
    }

    private val terminalListener = object : TerminalListener {
        override fun onConnectionStatusChange(status: ConnectionStatus) {
            runOnUiThread {
                statusTextView.text = "Status: ${status.name}"
                updateButtons(status)
            }
        }
        override fun onPaymentStatusChange(status: PaymentStatus) {
            Log.d("StripeTerminal", "Payment status changed: ${status.name}")
        }
    }

    private val discoveryListener = object : DiscoveryListener {
        override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
            runOnUiThread {
                if (readers.isNotEmpty()) {
                    discoveredReader = readers.first()
                    statusTextView.text = "Discovered: ${discoveredReader?.serialNumber}"
                    connectButton.isEnabled = true
                } else {
                    discoveredReader = null
                    statusTextView.text = "No readers discovered"
                    connectButton.isEnabled = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        discoverButton = findViewById(R.id.discoverButton)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        chargeButton = findViewById(R.id.chargeButton)
        locationIdInput = findViewById(R.id.locationIdInput)

        val prefs = getSharedPreferences("StripePrefs", Context.MODE_PRIVATE)
        val savedLocationId = prefs.getString("location_id", "")
        if (!savedLocationId.isNullOrEmpty()) {
            locationIdInput.setText(savedLocationId)
        }

        if (!Terminal.isInitialized()) {
            Terminal.init(applicationContext, LogLevel.VERBOSE, tokenProvider, terminalListener, null)
        }

        discoverButton.setOnClickListener { checkPermissionsAndDiscover() }
        connectButton.setOnClickListener { discoveredReader?.let { connectToReader(it) } }
        disconnectButton.setOnClickListener { disconnectReader() }
        chargeButton.setOnClickListener { chargeCard() }
    }

    private fun checkPermissionsAndDiscover() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
        val ungranted = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }

        if (ungranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, ungranted.toTypedArray(), 1001)
        } else {
            discoverUsbReaders()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            discoverUsbReaders()
        } else {
            Toast.makeText(this, "Permissions required for Terminal SDK", Toast.LENGTH_SHORT).show()
        }
    }

    private fun discoverUsbReaders() {
        val config = DiscoveryConfiguration.UsbDiscoveryConfiguration(timeout = 0, isSimulated = false)
        
        Terminal.getInstance().discoverReaders(config, discoveryListener, object : Callback {
            override fun onSuccess() {
                runOnUiThread { statusTextView.text = "Discovering readers..." }
            }
            override fun onFailure(e: TerminalException) {
                runOnUiThread { statusTextView.text = "Discovery failed: ${e.errorMessage}" }
            }
        })
    }

    private fun connectToReader(reader: Reader) {
        val locId = locationIdInput.text.toString().trim()
        if (locId.isEmpty()) {
            Toast.makeText(this, "Please enter a valid Location ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Save to SharedPreferences for next time
        getSharedPreferences("StripePrefs", Context.MODE_PRIVATE).edit().putString("location_id", locId).apply()

        appendLog("Initiating connection to reader...")
        val listener = object : com.stripe.stripeterminal.external.callable.MobileReaderListener {
            override fun onStartInstallingUpdate(update: ReaderSoftwareUpdate, cancelable: com.stripe.stripeterminal.external.callable.Cancelable?) {
                appendLog("Update started: ${update.version}")
            }
            override fun onReportReaderSoftwareUpdateProgress(progress: Float) {
                appendLog("Update progress: ${progress * 100}%")
            }
            override fun onFinishInstallingUpdate(update: ReaderSoftwareUpdate?, e: TerminalException?) {
                appendLog("Update finished. Error: ${e?.errorMessage}")
            }
            override fun onRequestReaderDisplayMessage(message: ReaderDisplayMessage) {
                appendLog("Reader Msg: ${message.toString()}")
            }
            override fun onRequestReaderInput(options: ReaderInputOptions) {
                appendLog("Reader Input: ${options.toString()}")
            }
            override fun onBatteryLevelUpdate(batteryLevel: Float, batteryStatus: BatteryStatus, isCharging: Boolean) {
                appendLog("Battery: $batteryLevel, Charging: $isCharging")
            }
            override fun onDisconnect(reason: DisconnectReason) {
                appendLog("Disconnected: ${reason.name}")
            }
        }
        val connectionConfig = ConnectionConfiguration.UsbConnectionConfiguration(locId, false, listener)
        
        Terminal.getInstance().connectReader(reader, connectionConfig, object : ReaderCallback {
            override fun onSuccess(reader: Reader) {
                appendLog("Connected to reader!")
                runOnUiThread { Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show() }
            }
            override fun onFailure(e: TerminalException) {
                appendLog("Connection failed: ${e.errorMessage}")
                runOnUiThread { Toast.makeText(this@MainActivity, "Connection failed: ${e.errorMessage}", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun disconnectReader() {
        Terminal.getInstance().disconnectReader(object : Callback {
            override fun onSuccess() {
                runOnUiThread { Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show() }
            }
            override fun onFailure(e: TerminalException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Disconnect failed: ${e.errorMessage}", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    private fun updateButtons(status: ConnectionStatus) {
        when (status) {
            ConnectionStatus.NOT_CONNECTED -> {
                discoverButton.isEnabled = true
                connectButton.isEnabled = discoveredReader != null
                disconnectButton.isEnabled = false
                chargeButton.isEnabled = false
            }
            ConnectionStatus.CONNECTING, ConnectionStatus.DISCOVERING, ConnectionStatus.RECONNECTING -> {
                discoverButton.isEnabled = false
                connectButton.isEnabled = false
                disconnectButton.isEnabled = false
                chargeButton.isEnabled = false
            }
            ConnectionStatus.CONNECTED -> {
                discoverButton.isEnabled = false
                connectButton.isEnabled = false
                disconnectButton.isEnabled = true
                chargeButton.isEnabled = true
            }
        }
    }

    private fun chargeCard() {
        appendLog("Creating PaymentIntent on backend...")
        val request = Request.Builder()
            .url("http://10.0.0.253:3000/create_payment_intent")
            .post("".toRequestBody(null))
            .build()

        httpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    try {
                        val json = JSONObject(body)
                        val clientSecret = json.getString("client_secret")
                        appendLog("PI Created. Retrieving from Terminal SDK...")
                        
                        Terminal.getInstance().retrievePaymentIntent(clientSecret, object : PaymentIntentCallback {
                            override fun onSuccess(paymentIntent: PaymentIntent) {
                                appendLog("PI Retrieved. Collecting Payment Method (Please tap/insert card)...")
                                Terminal.getInstance().collectPaymentMethod(paymentIntent, object : PaymentIntentCallback {
                                    override fun onSuccess(paymentIntent: PaymentIntent) {
                                        appendLog("Card collected! Processing payment...")
                                        Terminal.getInstance().confirmPaymentIntent(paymentIntent, object : PaymentIntentCallback {
                                            override fun onSuccess(paymentIntent: PaymentIntent) {
                                                appendLog("Payment successful! Status: ${paymentIntent.status}")
                                            }
                                            override fun onFailure(e: TerminalException) {
                                                appendLog("Process failed: ${e.errorMessage}")
                                            }
                                        })
                                    }
                                    override fun onFailure(e: TerminalException) {
                                        appendLog("Collect failed: ${e.errorMessage}")
                                    }
                                })
                            }
                            override fun onFailure(e: TerminalException) {
                                appendLog("Retrieve failed: ${e.errorMessage}")
                            }
                        })
                    } catch (e: Exception) {
                        appendLog("Failed to parse PaymentIntent response: ${e.message}")
                    }
                } else {
                    appendLog("Backend create_payment_intent failed: ${response.code}")
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                appendLog("Network error calling backend: ${e.message}")
            }
        })
    }
}
