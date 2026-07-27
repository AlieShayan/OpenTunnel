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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "opentunnel")

private val profileJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/**
 * DataStore holding the list of VPN profiles and app settings.
 *
 * Profiles are serialised as a JSON array in a single string key.
 * Passwords are encrypted per-profile via [SecretBox].
 *
 * Migration path from v1.0.x single-profile keys is handled in [migrateIfNeeded].
 */
class Repository(context: Context) {

    private val store = context.applicationContext.dataStore

    private object Keys {
        // ── multi-profile (v1.2+) ────────────────────────────────────────────
        /** JSON-encoded List<VpnProfile>, passwords already encrypted. */
        val profilesJson = stringPreferencesKey("profiles.v2.json")
        /** ID of the currently active profile. */
        val activeProfileId = stringPreferencesKey("settings.activeProfileId")

        // ── legacy single-profile keys (v1.0.x) — read-only for migration ───
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
        val legacyDisableXmlPost = booleanPreferencesKey("profile.disableXmlPost")
        val legacyTrustedCert = stringPreferencesKey("profile.trustedCert")

        // ── settings ─────────────────────────────────────────────────────────
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

    // ── profiles ─────────────────────────────────────────────────────────────

    /**
     * Emits the current list of profiles, migrating from the legacy single-profile
     * format on first run if needed.
     *
     * Passwords are decrypted before emission.
     */
    val profiles: Flow<List<VpnProfile>> = prefs.map { p ->
        val json = p[Keys.profilesJson]
        if (json != null) {
            // Normal path: decode stored JSON and decrypt passwords
            profileJson.decodeFromString<List<VpnProfile>>(json).map { profile ->
                profile.copy(password = SecretBox.decrypt(profile.password))
            }
        } else {
            // Migration: try to read the v1.0.x single-profile keys
            val server = p[Keys.legacyServer].orEmpty()
            if (server.isNotBlank()) {
                listOf(
                    VpnProfile(
                        id = UUID.randomUUID().toString(),
                        name = p[Keys.legacyName].orEmpty(),
                        server = server,
                        username = p[Keys.legacyUsername].orEmpty(),
                        password = SecretBox.decrypt(p[Keys.legacyPassword].orEmpty()),
                        savePassword = p[Keys.legacySavePassword] ?: true,
                        authGroup = p[Keys.legacyAuthGroup].orEmpty(),
                        protocol = p[Keys.legacyProtocol] ?: "anyconnect",
                        reportedOs = p[Keys.legacyReportedOs] ?: "android",
                        userAgent = p[Keys.legacyUserAgent].orEmpty(),
                        mtu = p[Keys.legacyMtu] ?: 0,
                        enableDtls = p[Keys.legacyDtls] ?: true,
                        enableIpv6 = p[Keys.legacyIpv6] ?: true,
                        allowInsecureCrypto = p[Keys.legacyAllowInsecure] ?: false,
                        dpdSeconds = p[Keys.legacyDpd] ?: 0,
                        disableXmlPost = p[Keys.legacyDisableXmlPost] ?: false,
                        trustedCertificate = p[Keys.legacyTrustedCert].orEmpty(),
                    )
                )
            } else {
                emptyList()
            }
        }
    }

    /** The currently-selected active profile (null if no profiles exist). */
    val activeProfile: Flow<VpnProfile?> = prefs.map { p ->
        val activeId = p[Keys.activeProfileId].orEmpty()
        val json = p[Keys.profilesJson]
        val list: List<VpnProfile> = if (json != null) {
            profileJson.decodeFromString<List<VpnProfile>>(json).map { profile ->
                profile.copy(password = SecretBox.decrypt(profile.password))
            }
        } else {
            emptyList()
        }
        list.firstOrNull { it.id == activeId } ?: list.firstOrNull()
    }

    /**
     * Backwards-compatible single profile accessor for the connect flow.
     * Returns the active profile, falling back to the first available profile.
     */
    val profile: Flow<VpnProfile> = activeProfile.map { it ?: VpnProfile() }

    suspend fun currentProfile(): VpnProfile = profile.first()

    suspend fun currentProfiles(): List<VpnProfile> = profiles.first()

    /** Persist (insert or update) one profile. Encrypts the password before writing. */
    suspend fun saveProfile(profile: VpnProfile) {
        store.edit { p ->
            val current = getStoredProfiles(p)
            val encryptedProfile = profile.copy(
                password = if (profile.savePassword) SecretBox.encrypt(profile.password) else ""
            )
            val updated = if (current.any { it.id == profile.id }) {
                current.map { if (it.id == profile.id) encryptedProfile else it }
            } else {
                current + encryptedProfile
            }
            p[Keys.profilesJson] = profileJson.encodeToString(updated)
            // If this is the first profile ever, mark it active
            if (p[Keys.activeProfileId].isNullOrBlank()) {
                p[Keys.activeProfileId] = profile.id
            }
        }
    }

    /** Remove a profile by ID. If it was active, fall through to the first remaining one. */
    suspend fun deleteProfile(profileId: String) {
        store.edit { p ->
            val updated = getStoredProfiles(p).filter { it.id != profileId }
            p[Keys.profilesJson] = profileJson.encodeToString(updated)
            if (p[Keys.activeProfileId] == profileId) {
                p[Keys.activeProfileId] = updated.firstOrNull()?.id.orEmpty()
            }
        }
    }

    /** Switch the active profile without modifying any profile data. */
    suspend fun setActiveProfile(profileId: String) {
        store.edit { it[Keys.activeProfileId] = profileId }
    }

    /** Persist a certificate the user accepted, without touching anything else. */
    suspend fun pinCertificate(fingerprint: String) {
        val active = profile.first()
        saveProfile(active.copy(trustedCertificate = fingerprint))
    }

    private fun getStoredProfiles(p: Preferences): List<VpnProfile> {
        val json = p[Keys.profilesJson] ?: return emptyList()
        return runCatching { profileJson.decodeFromString<List<VpnProfile>>(json) }.getOrElse { emptyList() }
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
            activeProfileId = p[Keys.activeProfileId].orEmpty(),
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
