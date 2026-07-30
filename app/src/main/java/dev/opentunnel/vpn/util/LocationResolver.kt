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

    private const val TIMEOUT_MS = 5_000

    data class Location(
        val ip: String,
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
        resolveIpWhoIs() ?: resolveFreeIpApi() ?: resolveIpApiCo() ?: resolveIpInfo()
    }

    private fun fetchJson(urlString: String): JSONObject? = runCatching {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.useCaches = false
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "OpenTunnel/1.0")
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("Connection", "close")
        if (conn.responseCode != 200) return@runCatching null
        val body = conn.inputStream.bufferedReader().readText()
        JSONObject(body)
    }.getOrNull()

    private fun resolveIpWhoIs(): Location? {
        val obj = fetchJson("https://ipwho.is/") ?: return null
        if (!obj.optBoolean("success", false)) return null
        val country = obj.optString("country", "")
        val countryCode = obj.optString("country_code", "")
        val city = obj.optString("city", "")
        if (country.isBlank() || countryCode.isBlank()) return null
        return Location(
            ip = obj.optString("ip", ""),
            country = country,
            countryCode = countryCode,
            city = city,
        )
    }

    private fun resolveFreeIpApi(): Location? {
        val obj = fetchJson("https://freeipapi.com/api/json") ?: return null
        val country = obj.optString("countryName", "")
        val countryCode = obj.optString("countryCode", "")
        val city = obj.optString("cityName", "")
        if (country.isBlank() || countryCode.isBlank()) return null
        return Location(
            ip = obj.optString("ipAddress", ""),
            country = country,
            countryCode = countryCode,
            city = city,
        )
    }

    private fun resolveIpApiCo(): Location? {
        val obj = fetchJson("https://ipapi.co/json/") ?: return null
        val country = obj.optString("country_name", "")
        val countryCode = obj.optString("country_code", "")
        val city = obj.optString("city", "")
        if (country.isBlank() || countryCode.isBlank()) return null
        return Location(
            ip = obj.optString("ip", ""),
            country = country,
            countryCode = countryCode,
            city = city,
        )
    }

    private fun resolveIpInfo(): Location? {
        val obj = fetchJson("https://ipinfo.io/json") ?: return null
        val countryCode = obj.optString("country", "")
        val city = obj.optString("city", "")
        if (countryCode.isBlank()) return null
        return Location(
            ip = obj.optString("ip", ""),
            country = countryCode,
            countryCode = countryCode,
            city = city,
        )
    }
}
