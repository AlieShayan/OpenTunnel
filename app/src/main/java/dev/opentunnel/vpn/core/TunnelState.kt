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
    /**
     * Human-readable country/city name resolved after connection, e.g. "Netherlands, Amsterdam".
     * Null while resolving or if lookup failed.
     */
    val locationName: String? = null,
    /**
     * Unicode flag emoji for the country, e.g. "\uD83C\uDDF3\uD83C\uDDF1" for NL.
     * Derived from the ISO-3166-1 alpha-2 country code returned by the geo-IP lookup.
     * Null when [locationName] is null.
     */
    val locationFlag: String? = null,
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
