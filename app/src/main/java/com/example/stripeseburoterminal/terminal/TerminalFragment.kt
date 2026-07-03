package com.example.stripeseburoterminal.terminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.stripeseburoterminal.databinding.FragmentTerminalBinding

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
}
