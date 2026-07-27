package dev.opentunnel.vpn.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.opentunnel.vpn.ui.theme.ThemeMode
import dev.opentunnel.vpn.util.SecretBox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "opentunnel")

/**
 * One small DataStore holding both the (single) VPN profile and app settings.
 * Multi-profile support would slot in here by keying on a profile id.
 */
class Repository(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val name = stringPreferencesKey("profile.name")
        val server = stringPreferencesKey("profile.server")
        val username = stringPreferencesKey("profile.username")
        val password = stringPreferencesKey("profile.password.enc")
        val savePassword = booleanPreferencesKey("profile.savePassword")
        val authGroup = stringPreferencesKey("profile.authGroup")
        val protocol = stringPreferencesKey("profile.protocol")
        val reportedOs = stringPreferencesKey("profile.reportedOs")
        val userAgent = stringPreferencesKey("profile.userAgent")
        val mtu = intPreferencesKey("profile.mtu")
        val enableDtls = booleanPreferencesKey("profile.dtls")
        val enableIpv6 = booleanPreferencesKey("profile.ipv6")
        val allowInsecure = booleanPreferencesKey("profile.allowInsecureCrypto")
        val dpd = intPreferencesKey("profile.dpd")
        val disableXmlPost = booleanPreferencesKey("profile.disableXmlPost")
        val trustedCert = stringPreferencesKey("profile.trustedCert")

        val themeMode = stringPreferencesKey("settings.themeMode")
        val dynamicColor = booleanPreferencesKey("settings.dynamicColor")
        val splitEnabled = booleanPreferencesKey("settings.split.enabled")
        val splitMode = stringPreferencesKey("settings.split.mode")
        val splitPackages = stringSetPreferencesKey("settings.split.packages")
        val bypassLocal = booleanPreferencesKey("settings.bypassLocalNetworks")
        val connectOnBoot = booleanPreferencesKey("settings.connectOnBoot")
        val reconnectOnNetwork = booleanPreferencesKey("settings.reconnectOnNetworkChange")
        val statsInNotification = booleanPreferencesKey("settings.statsInNotification")
        val verboseLogging = booleanPreferencesKey("settings.verboseLogging")
    }

    private val prefs: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    // ── profile ─────────────────────────────────────────────────────────────

    val profile: Flow<VpnProfile> = prefs.map { p ->
        val defaults = VpnProfile()
        VpnProfile(
            name = p[Keys.name] ?: defaults.name,
            server = p[Keys.server] ?: defaults.server,
            username = p[Keys.username] ?: defaults.username,
            password = SecretBox.decrypt(p[Keys.password].orEmpty()),
            savePassword = p[Keys.savePassword] ?: defaults.savePassword,
            authGroup = p[Keys.authGroup] ?: defaults.authGroup,
            protocol = p[Keys.protocol] ?: defaults.protocol,
            reportedOs = p[Keys.reportedOs] ?: defaults.reportedOs,
            userAgent = p[Keys.userAgent] ?: defaults.userAgent,
            mtu = p[Keys.mtu] ?: defaults.mtu,
            enableDtls = p[Keys.enableDtls] ?: defaults.enableDtls,
            enableIpv6 = p[Keys.enableIpv6] ?: defaults.enableIpv6,
            allowInsecureCrypto = p[Keys.allowInsecure] ?: defaults.allowInsecureCrypto,
            dpdSeconds = p[Keys.dpd] ?: defaults.dpdSeconds,
            disableXmlPost = p[Keys.disableXmlPost] ?: defaults.disableXmlPost,
            trustedCertificate = p[Keys.trustedCert] ?: defaults.trustedCertificate,
        )
    }

    suspend fun currentProfile(): VpnProfile = profile.first()

    suspend fun saveProfile(profile: VpnProfile) {
        store.edit { p ->
            p[Keys.name] = profile.name
            p[Keys.server] = profile.server.trim()
            p[Keys.username] = profile.username.trim()
            p[Keys.password] =
                if (profile.savePassword) SecretBox.encrypt(profile.password) else ""
            p[Keys.savePassword] = profile.savePassword
            p[Keys.authGroup] = profile.authGroup
            p[Keys.protocol] = profile.protocol
            p[Keys.reportedOs] = profile.reportedOs
            p[Keys.userAgent] = profile.userAgent
            p[Keys.mtu] = profile.mtu
            p[Keys.enableDtls] = profile.enableDtls
            p[Keys.enableIpv6] = profile.enableIpv6
            p[Keys.allowInsecure] = profile.allowInsecureCrypto
            p[Keys.dpd] = profile.dpdSeconds
            p[Keys.disableXmlPost] = profile.disableXmlPost
            p[Keys.trustedCert] = profile.trustedCertificate
        }
    }

    /** Persist a certificate the user accepted, without touching anything else. */
    suspend fun pinCertificate(fingerprint: String) {
        store.edit { it[Keys.trustedCert] = fingerprint }
    }

    // ── settings ────────────────────────────────────────────────────────────

    val settings: Flow<AppSettings> = prefs.map { p ->
        val defaults = AppSettings()
        AppSettings(
            themeMode = p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: defaults.themeMode,
            dynamicColor = p[Keys.dynamicColor] ?: defaults.dynamicColor,
            splitTunnelEnabled = p[Keys.splitEnabled] ?: defaults.splitTunnelEnabled,
            splitTunnelMode = p[Keys.splitMode]
                ?.let { runCatching { SplitTunnelMode.valueOf(it) }.getOrNull() }
                ?: defaults.splitTunnelMode,
            selectedPackages = p[Keys.splitPackages] ?: defaults.selectedPackages,
            bypassLocalNetworks = p[Keys.bypassLocal] ?: defaults.bypassLocalNetworks,
            connectOnBoot = p[Keys.connectOnBoot] ?: defaults.connectOnBoot,
            reconnectOnNetworkChange = p[Keys.reconnectOnNetwork] ?: defaults.reconnectOnNetworkChange,
            showStatsInNotification = p[Keys.statsInNotification] ?: defaults.showStatsInNotification,
            verboseLogging = p[Keys.verboseLogging] ?: defaults.verboseLogging,
        )
    }

    suspend fun currentSettings(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.themeMode] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = store.edit { it[Keys.dynamicColor] = enabled }
    suspend fun setSplitTunnelEnabled(enabled: Boolean) = store.edit { it[Keys.splitEnabled] = enabled }
    suspend fun setSplitTunnelMode(mode: SplitTunnelMode) = store.edit { it[Keys.splitMode] = mode.name }
    suspend fun setBypassLocalNetworks(enabled: Boolean) = store.edit { it[Keys.bypassLocal] = enabled }
    suspend fun setConnectOnBoot(enabled: Boolean) = store.edit { it[Keys.connectOnBoot] = enabled }
    suspend fun setReconnectOnNetworkChange(enabled: Boolean) =
        store.edit { it[Keys.reconnectOnNetwork] = enabled }
    suspend fun setShowStatsInNotification(enabled: Boolean) =
        store.edit { it[Keys.statsInNotification] = enabled }
    suspend fun setVerboseLogging(enabled: Boolean) = store.edit { it[Keys.verboseLogging] = enabled }

    suspend fun setPackageSelected(packageName: String, selected: Boolean) {
        store.edit { p ->
            val current = p[Keys.splitPackages] ?: emptySet()
            p[Keys.splitPackages] =
                if (selected) current + packageName else current - packageName
        }
    }

    suspend fun setSelectedPackages(packages: Set<String>) {
        store.edit { it[Keys.splitPackages] = packages }
    }

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context.applicationContext).also { instance = it }
            }
    }
}
