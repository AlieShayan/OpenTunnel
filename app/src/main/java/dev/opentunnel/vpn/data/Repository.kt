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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "opentunnel")

/**
 * Single access point for app settings and multi-profile management.
 * Profile data is delegated to [ProfileStore]; settings live in a separate DataStore.
 */
class Repository(context: Context) {

    private val store = context.applicationContext.dataStore
    private val profileStore = ProfileStore.get(context)

    private object Keys {
        // Legacy single-profile keys — kept for one-time migration only
        val legacyName = stringPreferencesKey("profile.name")
        val legacyServer = stringPreferencesKey("profile.server")
        val legacyUsername = stringPreferencesKey("profile.username")
        val legacyPassword = stringPreferencesKey("profile.password.enc")
        val legacySavePassword = booleanPreferencesKey("profile.savePassword")
        val legacyAuthGroup = stringPreferencesKey("profile.authGroup")
        val legacyProtocol = stringPreferencesKey("profile.protocol")
        val legacyReportedOs = stringPreferencesKey("profile.reportedOs")
        val legacyUserAgent = stringPreferencesKey("profile.userAgent")
        val legacyMtu = intPreferencesKey("profile.mtu")
        val legacyDtls = booleanPreferencesKey("profile.dtls")
        val legacyIpv6 = booleanPreferencesKey("profile.ipv6")
        val legacyAllowInsecure = booleanPreferencesKey("profile.allowInsecureCrypto")
        val legacyDpd = intPreferencesKey("profile.dpd")
        val legacyDisableXml = booleanPreferencesKey("profile.disableXmlPost")
        val legacyTrustedCert = stringPreferencesKey("profile.trustedCert")

        val themeMode = stringPreferencesKey("settings.themeMode")
        val appLanguage = stringPreferencesKey("settings.appLanguage")
        val dynamicColor = booleanPreferencesKey("settings.dynamicColor")
        val splitEnabled = booleanPreferencesKey("settings.split.enabled")
        val splitMode = stringPreferencesKey("settings.split.mode")
        val splitPackages = stringSetPreferencesKey("settings.split.packages")
        val bypassLocal = booleanPreferencesKey("settings.bypassLocalNetworks")
        val connectOnBoot = booleanPreferencesKey("settings.connectOnBoot")
        val reconnectOnNetwork = booleanPreferencesKey("settings.reconnectOnNetworkChange")
        val statsInNotification = booleanPreferencesKey("settings.statsInNotification")
        val verboseLogging = booleanPreferencesKey("settings.verboseLogging")
        val customDns = stringPreferencesKey("settings.customDns")
        val enableGeoIpLookup = booleanPreferencesKey("settings.enableGeoIpLookup")
        val hapticFeedbackEnabled = booleanPreferencesKey("settings.hapticFeedbackEnabled")
        val activeProfileId = stringPreferencesKey("settings.activeProfileId")
        val legacyMigrated = booleanPreferencesKey("settings.legacyMigrated")
    }

    private val prefs: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    // ── multi-profile ───────────────────────────────────────────────────────

    val profiles: Flow<List<VpnProfile>> = profileStore.profiles

    val activeProfile: Flow<VpnProfile> = combine(profileStore.profiles, prefs) { list, p ->
        val id = p[Keys.activeProfileId].orEmpty()
        list.firstOrNull { it.id == id } ?: list.firstOrNull() ?: VpnProfile()
    }

    /** Back-compat alias used by older callers that expect a single profile. */
    val profile: Flow<VpnProfile> get() = activeProfile

    suspend fun currentProfile(): VpnProfile = activeProfile.first()

    suspend fun getProfileById(id: String): VpnProfile? =
        profileStore.allProfiles().firstOrNull { it.id == id }

    /** Upsert and return the saved copy (which has an auto-assigned id if new). */
    suspend fun saveProfile(profile: VpnProfile): VpnProfile = profileStore.save(profile)

    suspend fun deleteProfile(profileId: String) = profileStore.delete(profileId)

    suspend fun setActiveProfile(profileId: String) {
        store.edit { it[Keys.activeProfileId] = profileId }
    }

    suspend fun exportProfilesJson(): String = profileStore.exportJson()

    suspend fun importProfilesJson(jsonStr: String): Int = profileStore.importJson(jsonStr)

    suspend fun pinCertificate(fingerprint: String) {
        val active = currentProfile()
        if (active.id.isNotBlank()) {
            saveProfile(active.copy(trustedCertificate = fingerprint))
        }
    }

    /**
     * One-time migration: if legacy single-profile keys exist and no profiles have been
     * saved yet, import that data as the first profile.
     */
    suspend fun migrateLegacyProfileIfNeeded() {
        val p = prefs.first()
        if (p[Keys.legacyMigrated] == true) return
        val server = p[Keys.legacyServer].orEmpty()
        if (server.isNotBlank() && profileStore.allProfiles().isEmpty()) {
            val defaults = VpnProfile()
            val legacy = VpnProfile(
                name = p[Keys.legacyName] ?: defaults.name,
                server = server,
                username = p[Keys.legacyUsername] ?: defaults.username,
                password = SecretBox.decrypt(p[Keys.legacyPassword].orEmpty()),
                savePassword = p[Keys.legacySavePassword] ?: defaults.savePassword,
                authGroup = p[Keys.legacyAuthGroup] ?: defaults.authGroup,
                protocol = p[Keys.legacyProtocol] ?: defaults.protocol,
                reportedOs = p[Keys.legacyReportedOs] ?: defaults.reportedOs,
                userAgent = p[Keys.legacyUserAgent] ?: defaults.userAgent,
                mtu = p[Keys.legacyMtu] ?: defaults.mtu,
                enableDtls = p[Keys.legacyDtls] ?: defaults.enableDtls,
                enableIpv6 = p[Keys.legacyIpv6] ?: defaults.enableIpv6,
                allowInsecureCrypto = p[Keys.legacyAllowInsecure] ?: defaults.allowInsecureCrypto,
                dpdSeconds = p[Keys.legacyDpd] ?: defaults.dpdSeconds,
                disableXmlPost = p[Keys.legacyDisableXml] ?: defaults.disableXmlPost,
                trustedCertificate = p[Keys.legacyTrustedCert] ?: defaults.trustedCertificate,
            )
            val saved = profileStore.save(legacy)
            store.edit { it[Keys.activeProfileId] = saved.id }
        }
        store.edit { it[Keys.legacyMigrated] = true }
    }

    // ── settings ─────────────────────────────────────────────────────────────

    val settings: Flow<AppSettings> = prefs.map { p ->
        val defaults = AppSettings()
        AppSettings(
            themeMode = p[Keys.themeMode]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: defaults.themeMode,
            appLanguage = p[Keys.appLanguage]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: defaults.appLanguage,
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
            customDns = p[Keys.customDns] ?: defaults.customDns,
            enableGeoIpLookup = p[Keys.enableGeoIpLookup] ?: defaults.enableGeoIpLookup,
            hapticFeedbackEnabled = p[Keys.hapticFeedbackEnabled] ?: defaults.hapticFeedbackEnabled,
            activeProfileId = p[Keys.activeProfileId].orEmpty(),
        )
    }

    suspend fun currentSettings(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.themeMode] = mode.name }
    suspend fun setAppLanguage(language: AppLanguage) = store.edit { it[Keys.appLanguage] = language.name }
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
    suspend fun setCustomDns(dns: String) = store.edit { it[Keys.customDns] = dns }
    suspend fun setEnableGeoIpLookup(enabled: Boolean) = store.edit { it[Keys.enableGeoIpLookup] = enabled }
    suspend fun setHapticFeedbackEnabled(enabled: Boolean) =
        store.edit { it[Keys.hapticFeedbackEnabled] = enabled }

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
