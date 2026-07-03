package com.example.stripeseburoterminal.terminal

import android.content.Context

class TerminalConfigStore(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): TerminalConfig {
        val defaults = TerminalConfig.defaults()
        return TerminalConfig(
            merchantName = preferences.getString(KEY_MERCHANT_NAME, defaults.merchantName) ?: defaults.merchantName,
            locationId = preferences.getString(KEY_LOCATION_ID, defaults.locationId) ?: defaults.locationId,
            backendBaseUrl = preferences.getString(KEY_BACKEND_BASE_URL, defaults.backendBaseUrl) ?: defaults.backendBaseUrl,
            connectionTokenPath = preferences.getString(KEY_CONNECTION_TOKEN_PATH, defaults.connectionTokenPath)
                ?: defaults.connectionTokenPath,
            paymentIntentPath = preferences.getString(KEY_PAYMENT_INTENT_PATH, defaults.paymentIntentPath)
                ?: defaults.paymentIntentPath,
            defaultCurrency = preferences.getString(KEY_DEFAULT_CURRENCY, defaults.defaultCurrency)
                ?: defaults.defaultCurrency,
            defaultAmountCents = preferences.getLong(KEY_DEFAULT_AMOUNT_CENTS, defaults.defaultAmountCents),
        )
    }

    fun save(config: TerminalConfig) {
        preferences.edit()
            .putString(KEY_MERCHANT_NAME, config.merchantName)
            .putString(KEY_LOCATION_ID, config.locationId)
            .putString(KEY_BACKEND_BASE_URL, config.backendBaseUrl)
            .putString(KEY_CONNECTION_TOKEN_PATH, config.connectionTokenPath)
            .putString(KEY_PAYMENT_INTENT_PATH, config.paymentIntentPath)
            .putString(KEY_DEFAULT_CURRENCY, config.defaultCurrency)
            .putLong(KEY_DEFAULT_AMOUNT_CENTS, config.defaultAmountCents)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "seburo_terminal_config"
        private const val KEY_MERCHANT_NAME = "merchant_name"
        private const val KEY_LOCATION_ID = "location_id"
        private const val KEY_BACKEND_BASE_URL = "backend_base_url"
        private const val KEY_CONNECTION_TOKEN_PATH = "connection_token_path"
        private const val KEY_PAYMENT_INTENT_PATH = "payment_intent_path"
        private const val KEY_DEFAULT_CURRENCY = "default_currency"
        private const val KEY_DEFAULT_AMOUNT_CENTS = "default_amount_cents"
    }
}
