package com.example.stripeseburoterminal.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.stripeseburoterminal.databinding.FragmentSettingsBinding
import com.example.stripeseburoterminal.terminal.TerminalConfig
import com.example.stripeseburoterminal.terminal.TerminalConfigStore
import com.example.stripeseburoterminal.terminal.TerminalViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var configStore: TerminalConfigStore
    private lateinit var terminalViewModel: TerminalViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        configStore = TerminalConfigStore(requireContext())
        terminalViewModel = ViewModelProvider(requireActivity())[TerminalViewModel::class.java]
        val config = configStore.load()
        bindConfig(config)

        binding.buttonSaveSettings.setOnClickListener {
            val savedConfig = TerminalConfig(
                merchantName = binding.inputMerchantName.text?.toString().orEmpty().ifBlank { "Seburo Pte Ltd" },
                locationId = binding.inputLocationId.text?.toString().orEmpty().trim(),
                backendBaseUrl = binding.inputBackendBaseUrl.text?.toString().orEmpty().trim(),
                connectionTokenPath = normalizePath(binding.inputConnectionTokenPath.text?.toString().orEmpty(), "/terminal/connection-token"),
                paymentIntentPath = normalizePath(binding.inputPaymentIntentPath.text?.toString().orEmpty(), "/terminal/payment-intents"),
                defaultCurrency = binding.inputDefaultCurrency.text?.toString().orEmpty().ifBlank { "SGD" }.uppercase(Locale.US),
                defaultAmountCents = binding.inputDefaultAmount.text?.toString()?.toLongOrNull() ?: 1000L,
            )
            configStore.save(savedConfig)
            terminalViewModel.refreshConfig()
            binding.textSettingsSummary.text = getString(
                com.example.stripeseburoterminal.R.string.settings_saved_summary,
                savedConfig.merchantName,
                savedConfig.defaultCurrency,
                savedConfig.defaultAmountCents,
            )
            Snackbar.make(binding.root, "Settings saved", Snackbar.LENGTH_SHORT).show()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindConfig(config: TerminalConfig) {
        binding.inputMerchantName.setText(config.merchantName)
        binding.inputLocationId.setText(config.locationId)
        binding.inputBackendBaseUrl.setText(config.backendBaseUrl)
        binding.inputConnectionTokenPath.setText(config.connectionTokenPath)
        binding.inputPaymentIntentPath.setText(config.paymentIntentPath)
        binding.inputDefaultCurrency.setText(config.defaultCurrency)
        binding.inputDefaultAmount.setText(config.defaultAmountCents.toString())
        binding.textSettingsSummary.text = getString(
            com.example.stripeseburoterminal.R.string.settings_saved_summary,
            config.merchantName,
            config.defaultCurrency,
            config.defaultAmountCents,
        )
    }

    private fun normalizePath(value: String, fallback: String): String {
        val trimmed = value.trim()
        return if (trimmed.isBlank()) fallback else if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    }
}
