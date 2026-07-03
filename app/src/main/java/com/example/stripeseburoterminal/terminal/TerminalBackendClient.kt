package com.example.stripeseburoterminal.terminal

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.Locale

class TerminalBackendClient {

    fun fetchConnectionToken(config: TerminalConfig): String {
        val response = postJson(
            url = urlFor(config, config.connectionTokenPath),
            body = JSONObject().put("merchant_name", config.merchantName),
        )
        return extractString(response, "secret", "connection_token", "connectionToken", "token")
    }

    fun createPaymentIntentClientSecret(config: TerminalConfig, amountCents: Long, currency: String): String {
        val response = postJson(
            url = urlFor(config, config.paymentIntentPath),
            body = JSONObject()
                .put("amount", amountCents)
                .put("currency", currency.lowercase(Locale.US))
                .put("merchant_name", config.merchantName)
                .put("location_id", config.locationId)
                .put("payment_method_types", JSONArray().put("card_present"))
                .put("capture_method", "automatic")
                .put("description", "Seburo Pte Ltd Terminal sale"),
        )
        return extractString(
            response,
            "client_secret",
            "payment_intent_client_secret",
            "clientSecret",
        )
    }

    private fun postJson(url: URL, body: JSONObject): JSONObject {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 15_000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }

        connection.outputStream.use { outputStream ->
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
                writer.flush()
            }
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        val responseBody = InputStreamReader(stream, Charset.forName("UTF-8")).use { reader ->
            reader.readText()
        }
        if (responseCode !in 200..299) {
            throw IllegalStateException("Backend request failed ($responseCode): $responseBody")
        }
        return JSONObject(responseBody)
    }

    private fun extractString(jsonObject: JSONObject, vararg keys: String): String {
        for (key in keys) {
            val value = jsonObject.optString(key)
            if (value.isNotBlank()) {
                return value
            }
        }
        throw IllegalStateException("Backend response is missing one of: ${keys.joinToString()}")
    }

    private fun urlFor(config: TerminalConfig, path: String): URL {
        val base = config.normalizedBackendBaseUrl()
        require(base.isNotBlank()) { "Set the backend base URL in Settings first." }
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return URL(base + normalizedPath)
    }
}
