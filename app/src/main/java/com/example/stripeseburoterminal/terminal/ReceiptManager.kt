package com.example.stripeseburoterminal.terminal

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReceiptManager(private val context: Context) {

    fun generateReceipt(
        merchantName: String,
        amount: Long,
        currency: String,
        paymentIntentId: String,
        cardLastFour: String? = null
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timestamp = dateFormat.format(Date())
        val amountFormatted = formatAmount(amount)

        return buildString {
            appendLine("═══════════════════════════════════")
            appendLine("           SEBURO PTE LTD")
            appendLine("        STRIPE READER M2 TERMINAL")
            appendLine("═══════════════════════════════════")
            appendLine()
            appendLine("Date: $timestamp")
            appendLine("Receipt: $paymentIntentId")
            appendLine()
            appendLine("───────────────────────────────────")
            appendLine("Payment Details:")
            appendLine("───────────────────────────────────")
            appendLine("Amount: $amountFormatted $currency")
            cardLastFour?.let {
                appendLine("Card: ••••••••••••$it")
            }
            appendLine()
            appendLine("───────────────────────────────────")
            appendLine("Status: Payment Approved")
            appendLine("Type: Card Present")
            appendLine("Method: Stripe Terminal M2 (USB)")
            appendLine()
            appendLine("═══════════════════════════════════")
            appendLine("Thank you for your purchase!")
            appendLine("═══════════════════════════════════")
        }
    }

    fun saveReceiptLocally(receipt: String, paymentIntentId: String): Boolean {
        return try {
            val filename = "receipt_${paymentIntentId}_${System.currentTimeMillis()}.txt"
            context.openFileOutput(filename, Context.MODE_PRIVATE).use { output ->
                output.write(receipt.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getReceiptHistory(): List<String> {
        return try {
            context.fileList()
                .filter { it.startsWith("receipt_") }
                .sorted()
                .reversed()
                .take(100)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun formatAmount(amountCents: Long): String {
        val dollars = amountCents / 100
        val cents = amountCents % 100
        return String.format("%.2f", dollars + cents / 100.0)
    }
}
