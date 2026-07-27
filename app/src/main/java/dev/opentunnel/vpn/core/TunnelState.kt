package dev.opentunnel.vpn.core

import androidx.compose.runtime.Immutable

/** Coarse lifecycle of the tunnel, as the UI cares about it. */
enum class ConnectionStage {
    IDLE,
    PREPARING,
    AUTHENTICATING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING,
    ERROR;

    val isBusy: Boolean
        get() = this == PREPARING || this == AUTHENTICATING ||
            this == CONNECTING || this == RECONNECTING || this == DISCONNECTING

    val isActive: Boolean
        get() = this != IDLE && this != ERROR

    /** True when a disconnect/cancel action is meaningful from the UI. */
    val isInterruptible: Boolean
        get() = isActive
}

/**
 * Location resolved from the tunnel exit IP via an ip-geolocation API.
 *
 * Fields are nullable because the lookup may fail or be unavailable.
 */
@Immutable
data class LocationInfo(
    /** ISO 3166-1 alpha-2 country code, e.g. "DE", "US", "IR". */
    val countryCode: String? = null,
    /** Human-readable country name, e.g. "Germany". */
    val countryName: String? = null,
    /** City name, e.g. "Frankfurt am Main". */
    val city: String? = null,
) {
    /**
     * Best display name — city if available, otherwise country.
     * Falls back to the country code, then "Unknown".
     */
    val displayName: String
        get() = city?.takeIf { it.isNotBlank() }
            ?: countryName?.takeIf { it.isNotBlank() }
            ?: countryCode?.takeIf { it.isNotBlank() }
            ?: "Unknown"

    /**
     * Unicode flag emoji derived from the ISO country code.
     * Works on Android 7+ with any country code that has a flag.
     */
    val flagEmoji: String
        get() = countryCode
            ?.uppercase()
            ?.takeIf { it.length == 2 && it.all { c -> c.isLetter() } }
            ?.map { c -> 0x1F1E0 + (c - 'A') }
            ?.joinToString("") { String(Character.toChars(it)) }
            ?: ""
}

@Immutable
data class TunnelInfo(
    val server: String? = null,
    val ipv4: String? = null,
    val ipv6: String? = null,
    val dns: List<String> = emptyList(),
    val domain: String? = null,
    val mtu: Int = 0,
    val cstpCipher: String? = null,
    val dtlsCipher: String? = null,
    val cstpCompression: String? = null,
    val dtlsCompression: String? = null,
    val serverRoutes: List<String> = emptyList(),
    val excludedApps: Int = 0,
    val certFingerprint: String? = null,
)

@Immutable
data class TunnelStatus(
    val stage: ConnectionStage = ConnectionStage.IDLE,
    /** Short human-readable line shown under the connect button. */
    val detail: String? = null,
    /** Populated when [stage] is ERROR. */
    val error: String? = null,
    /** SystemClock.elapsedRealtime() at the moment the tunnel came up; 0 if down. */
    val connectedAtElapsed: Long = 0L,
    val info: TunnelInfo = TunnelInfo(),
    /** Location of the tunnel exit node, resolved asynchronously after connect. */
    val location: LocationInfo? = null,
)

@Immutable
data class TrafficStats(
    val rxBytes: Long = 0,
    val txBytes: Long = 0,
    val rxPackets: Long = 0,
    val txPackets: Long = 0,
    /** Bytes per second, smoothed over the last sampling window. */
    val rxRate: Long = 0,
    val txRate: Long = 0,
)

enum class LogLevel { ERROR, INFO, DEBUG, TRACE, APP }

@Immutable
data class LogLine(
    val timestamp: Long,
    val level: LogLevel,
    val text: String,
)
