package dev.opentunnel.vpn.service

import dev.opentunnel.vpn.core.LocationInfo
import dev.opentunnel.vpn.core.VpnBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Resolves the public exit IP to a country + city by querying ip-api.com
 * (free, no key needed, 45 req/min limit).
 *
 * Called once after the tunnel comes up, from a background coroutine inside
 * [OpenTunnelVpnService]. Sets [VpnBus.setLocation] on success; clears it on
 * failure so the UI can hide the location row gracefully.
 */
object LocationResolver {

    private const val API_URL = "http://ip-api.com/json/?fields=status,country,countryCode,city"

    /**
     * Fetches the current public IP location and pushes the result to [VpnBus].
     * Must be called from a coroutine — does IO on [Dispatchers.IO].
     */
    suspend fun resolve() {
        val info = runCatching {
            withContext(Dispatchers.IO) {
                val json = URL(API_URL).readText()
                val obj = JSONObject(json)
                if (obj.optString("status") == "success") {
                    LocationInfo(
                        countryCode = obj.optString("countryCode").takeIf { it.isNotBlank() },
                        countryName = obj.optString("country").takeIf { it.isNotBlank() },
                        city = obj.optString("city").takeIf { it.isNotBlank() },
                    )
                } else null
            }
        }.getOrNull()
        VpnBus.setLocation(info)
    }
}
