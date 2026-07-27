package dev.opentunnel.vpn.data

import androidx.compose.runtime.Immutable
import dev.opentunnel.vpn.ui.theme.ThemeMode
import kotlinx.serialization.Serializable

/**
 * Everything needed to reach one AnyConnect/openconnect gateway.
 *
 * [password] is held in memory in the clear but is always encrypted before it
 * touches disk — see [Repository].
 */
@Serializable
@Immutable
data class VpnProfile(
    /** Unique identifier for this profile. Auto-generated if blank. */
    val id: String = "",
    val name: String = "",
    /** Hostname, "host:port", or a full https:// URL including the group path. */
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val savePassword: Boolean = true,
    /** Auth group / tunnel group, when the gateway offers a dropdown. */
    val authGroup: String = "",

    // ── advanced ────────────────────────────────────────────────────────────
    val protocol: String = "anyconnect",
    /** What we tell the gateway we are: android, linux-64, win, mac-intel, apple-ios. */
    val reportedOs: String = "android",
    /** Blank uses the library default ("OpenConnect VPN Agent/<ver>"). */
    val userAgent: String = "",
    /** 0 means "use whatever the gateway proposes". */
    val mtu: Int = 0,
    val enableDtls: Boolean = true,
    val enableIpv6: Boolean = true,
    /** Re-enable 3DES/RC4-era ciphers for very old gateways. Off by default. */
    val allowInsecureCrypto: Boolean = false,
    /** Dead-peer-detection interval in seconds; 0 = gateway default. */
    val dpdSeconds: Int = 0,
    val disableXmlPost: Boolean = false,
    /** SHA-256 pin accepted on first use, in openconnect's "pin-sha256:…" form. */
    val trustedCertificate: String = "",

    // ── extended profile options ─────────────────────────────────────────────
    val caCertPath: String = "",
    val userCertPath: String = "",
    val privateKeyPath: String = "",
    /** Software token mode: 0=Disabled, 1=STOKEN, 2=TOTP, 3=HOTP */
    val softwareTokenMode: Int = 0,
    val tokenString: String = "",
    val disableCredentialCaching: Boolean = false,
    val batchMode: Boolean = false,
    val csdWrapper: String = "",
    val profileSplitTunnelMode: String = "auto",
    val splitTunnelNetworks: String = "",
    val requirePfs: Boolean = false,
    val overrideDpdTimeout: Boolean = false,

    /**
     * OpenSSL cipher string override for TLS negotiation.
     *
     * Useful when a Wi-Fi hotspot or carrier DPI middlebox drops the TLS
     * handshake because it does not recognise the default TLS 1.3 cipher
     * fingerprint. Setting this to a legacy-compatible string such as
     * "DEFAULT:\@SECLEVEL=1" forces OpenSSL to include older cipher suites
     * in ClientHello, which typically passes hotspot DPI filters.
     *
     * Leave blank to use the library default (recommended for most users).
     * Example values:
     *   - "DEFAULT:\@SECLEVEL=1"   — adds TLS 1.2 ciphers, passes most hotspot DPI
     *   - "HIGH:!aNULL:!eNULL"    — restrict to high-strength only
     */
    val openSSLCiphers: String = "",
) {
    val isComplete: Boolean
        get() = server.isNotBlank() && username.isNotBlank()

    /** What to show as the tunnel's name when the user has not set one. */
    val displayName: String
        get() = name.ifBlank { server.substringAfter("://").substringBefore('/').ifBlank { "New profile" } }
}

/** Behaviour of the per-app split tunnel. */
enum class SplitTunnelMode {
    /** Everything is tunnelled except the selected apps. (What most people want.) */
    EXCLUDE_SELECTED,

    /** Only the selected apps are tunnelled; everything else bypasses the VPN. */
    INCLUDE_SELECTED,
}

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    PERSIAN,
}

@Immutable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val dynamicColor: Boolean = false,

    val splitTunnelEnabled: Boolean = false,
    val splitTunnelMode: SplitTunnelMode = SplitTunnelMode.EXCLUDE_SELECTED,
    val selectedPackages: Set<String> = emptySet(),
    /** Keep LAN/private-range traffic off the tunnel. */
    val bypassLocalNetworks: Boolean = true,

    val connectOnBoot: Boolean = false,
    val reconnectOnNetworkChange: Boolean = true,
    val showStatsInNotification: Boolean = true,
    val verboseLogging: Boolean = false,

    /** ID of the currently active profile. Empty means use the first available. */
    val activeProfileId: String = "",
)
