package com.example.stripeseburoterminal.terminal

import com.stripe.stripeterminal.external.models.PaymentIntent

sealed class PaymentResult {
    data class Success(
        val paymentIntent: PaymentIntent,
        val amount: Long,
        val currency: String,
        val receiptNumber: String = paymentIntent.id ?: ""
    ) : PaymentResult()

    data class Failure(
        val errorMessage: String,
        val errorCode: String? = null
    ) : PaymentResult()

    object Cancelled : PaymentResult()
}
