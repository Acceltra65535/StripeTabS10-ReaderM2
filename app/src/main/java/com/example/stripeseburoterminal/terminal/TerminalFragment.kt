package com.example.stripeseburoterminal.terminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.stripeseburoterminal.databinding.FragmentTerminalBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TerminalFragment : Fragment() {

    private var _binding: FragmentTerminalBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TerminalViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTerminalBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[TerminalViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.connectionStatus.observe(viewLifecycleOwner) {
            binding.textConnectionStatus.text = getString(
                com.example.stripeseburoterminal.R.string.status_connection_template,
                it,
            )
        }
        viewModel.paymentStatus.observe(viewLifecycleOwner) {
            binding.textPaymentStatus.text = getString(
                com.example.stripeseburoterminal.R.string.status_payment_template,
                it,
            )
        }
        viewModel.readerStatus.observe(viewLifecycleOwner) {
            binding.textReaderStatus.text = getString(
                com.example.stripeseburoterminal.R.string.status_reader_template,
                it,
            )
        }
        viewModel.logLines.observe(viewLifecycleOwner) {
            binding.textLogs.text = it.joinToString("\n")
        }
        viewModel.selectedReader.observe(viewLifecycleOwner) { reader ->
            binding.textReaderStatus.text = getString(
                com.example.stripeseburoterminal.R.string.status_reader_template,
                reader?.let { viewModelLabel(it) } ?: getString(com.example.stripeseburoterminal.R.string.status_reader_not_selected),
            )
        }

        viewModel.isBusy.observe(viewLifecycleOwner) { isBusy ->
            binding.buttonDiscoverUsb.isEnabled = !isBusy
            binding.buttonConnectReader.isEnabled = !isBusy
            binding.buttonLoadPaymentIntent.isEnabled = !isBusy
            binding.buttonCollectPayment.isEnabled = !isBusy
        }

        viewModel.usbDeviceConnected.observe(viewLifecycleOwner) { isConnected ->
            val status = if (isConnected) "USB device: ✓ Connected" else "USB device: ✗ Disconnected"
            binding.textUsbStatus?.let { it.text = status }
        }

        viewModel.paymentResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is PaymentResult.Success -> {
                    showPaymentSuccessDialog(result)
                }
                is PaymentResult.Failure -> {
                    showPaymentFailureDialog(result)
                }
                is PaymentResult.Cancelled -> {
                    showPaymentCancelledDialog()
                }
                null -> {
                    // No result to show
                }
            }
        }

        viewModel.activeConfig.value?.let { config ->
            binding.inputAmount.setText(config.defaultAmountCents.toString())
            binding.inputCurrency.setText(config.defaultCurrency)
        }

        binding.buttonDiscoverUsb.setOnClickListener {
            viewModel.discoverUsbReaders()
        }
        binding.buttonConnectReader.setOnClickListener {
            viewModel.connectSelectedReader()
        }
        binding.buttonLoadPaymentIntent.setOnClickListener {
            viewModel.createPaymentIntent(amountValue(), currencyValue())
        }
        binding.buttonCollectPayment.setOnClickListener {
            viewModel.chargePayment(amountValue(), currencyValue())
        }
        binding.buttonCancelOperation.setOnClickListener {
            viewModel.cancelCurrentOperation()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun amountValue(): Long = binding.inputAmount.text?.toString()?.trim()?.toLongOrNull() ?: 0L

    private fun currencyValue(): String = binding.inputCurrency.text?.toString().orEmpty()

    private fun viewModelLabel(reader: com.stripe.stripeterminal.external.models.Reader): String {
        return listOfNotNull(reader.serialNumber, reader.id).firstOrNull().orEmpty()
    }

    private fun showPaymentSuccessDialog(result: PaymentResult.Success) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Payment Approved ✓")
            .setMessage(buildString {
                appendLine("Amount: ${formatAmount(result.amount)} ${result.currency}")
                appendLine("Receipt: ${result.receiptNumber}")
                appendLine()
                appendLine("Payment successfully processed on Stripe Reader M2.")
            })
            .setPositiveButton("New Transaction") { _, _ ->
                viewModel.clearPaymentResult()
                binding.inputAmount.text?.clear()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPaymentFailureDialog(result: PaymentResult.Failure) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Payment Failed ✗")
            .setMessage(buildString {
                appendLine("Error: ${result.errorMessage}")
                result.errorCode?.let { appendLine("Code: $it") }
                appendLine()
                appendLine("Please try again or contact support.")
            })
            .setPositiveButton("Retry") { _, _ ->
                viewModel.clearPaymentResult()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPaymentCancelledDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Payment Cancelled")
            .setMessage("The payment operation was cancelled.")
            .setPositiveButton("OK") { _, _ ->
                viewModel.clearPaymentResult()
            }
            .setCancelable(false)
            .show()
    }

    private fun formatAmount(amountCents: Long): String {
        val dollars = amountCents / 100
        val cents = amountCents % 100
        return String.format("%.2f", dollars + cents / 100.0)
    }
}
