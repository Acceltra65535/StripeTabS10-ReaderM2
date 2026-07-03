package com.example.stripeseburoterminal.terminal

data class TerminalConfig(
    val merchantName: String,
    val locationId: String,
    val backendBaseUrl: String,
    val connectionTokenPath: String,
    val paymentIntentPath: String,
    val defaultCurrency: String,
    val defaultAmountCents: Long,
) {
    fun normalizedBackendBaseUrl(): String = backendBaseUrl.trim().trimEnd('/')

    companion object {
        fun defaults() = TerminalConfig(
            merchantName = "Seburo Pte Ltd",
            locationId = "",
            backendBaseUrl = "",
            connectionTokenPath = "/terminal/connection-token",
            paymentIntentPath = "/terminal/payment-intents",
            defaultCurrency = "SGD",
            defaultAmountCents = 1000L,
        )
    }
}
