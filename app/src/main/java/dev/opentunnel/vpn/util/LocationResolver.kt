package dev.opentunnel.vpn.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resolves the public IP's geographic location using the ip-api.com free endpoint.
 * Must be called from a coroutine; uses Dispatchers.IO internally.
 *
 * Returns null on any network or parse failure.
 */
object LocationResolver {

    private const val API = "http://ip-api.com/json/?fields=status,country,countryCode,city"
    private const val TIMEOUT_MS = 5_000

    data class Location(
        /** e.g. "Netherlands" */
        val country: String,
        /** ISO-3166-1 alpha-2, e.g. "NL" */
        val countryCode: String,
        /** e.g. "Amsterdam" */
        val city: String,
    ) {
        /** Unicode flag emoji derived from the two-letter country code. */
        val flagEmoji: String
            get() = countryCode.uppercase()
                .map { ch -> 0x1F1E6 + (ch.code - 'A'.code) }
                .joinToString("") { String(Character.toChars(it)) }

        /** Human-readable single line, e.g. "\uD83C\uDDF3\uD83C\uDDF1 Netherlands, Amsterdam" */
        val displayLine: String get() = "$flagEmoji $country, $city"
    }

    suspend fun resolve(): Location? = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(API)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode != 200) return@runCatching null
            val body = conn.inputStream.bufferedReader().readText()
            val obj = JSONObject(body)
            if (obj.optString("status") != "success") return@runCatching null
            Location(
                country = obj.getString("country"),
                countryCode = obj.getString("countryCode"),
                city = obj.getString("city"),
            )
        }.getOrNull()
    }
}
