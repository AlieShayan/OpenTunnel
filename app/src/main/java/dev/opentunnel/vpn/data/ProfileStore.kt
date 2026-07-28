package dev.opentunnel.vpn.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.opentunnel.vpn.util.SecretBox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.UUID

private val Context.profileDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "opentunnel_profiles")

/**
 * Persists the ordered list of [VpnProfile] objects as a JSON array.
 * Passwords are individually encrypted with [SecretBox] before storage.
 */
class ProfileStore(context: Context) {

    private val store = context.applicationContext.profileDataStore

    private object Keys {
        val profilesJson = stringPreferencesKey("profiles.json")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── public API ───────────────────────────────────────────────────────────

    val profiles: Flow<List<VpnProfile>> = store.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { prefs -> decode(prefs[Keys.profilesJson].orEmpty()) }

    suspend fun allProfiles(): List<VpnProfile> = profiles.first()

    /** Upsert a profile. If [profile.id] is blank a new UUID is assigned. */
    suspend fun save(profile: VpnProfile): VpnProfile {
        val toSave = if (profile.id.isBlank()) profile.copy(id = UUID.randomUUID().toString()) else profile
        store.edit { prefs ->
            val current = decode(prefs[Keys.profilesJson].orEmpty()).toMutableList()
            val idx = current.indexOfFirst { it.id == toSave.id }
            if (idx >= 0) current[idx] = toSave else current.add(toSave)
            prefs[Keys.profilesJson] = encode(current)
        }
        return toSave
    }

    suspend fun saveAll(profiles: List<VpnProfile>) {
        store.edit { prefs ->
            prefs[Keys.profilesJson] = encode(profiles)
        }
    }

    suspend fun exportJson(): String {
        return json.encodeToString(allProfiles().map { it.copy(password = "") })
    }

    suspend fun importJson(jsonStr: String): Int {
        return runCatching {
            val imported = json.decodeFromString<List<VpnProfile>>(jsonStr)
            if (imported.isEmpty()) return 0
            var count = 0
            store.edit { prefs ->
                val current = decode(prefs[Keys.profilesJson].orEmpty()).toMutableList()
                imported.forEach { item ->
                    val p = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
                    val idx = current.indexOfFirst { it.id == p.id }
                    if (idx >= 0) current[idx] = p else current.add(p)
                    count++
                }
                prefs[Keys.profilesJson] = encode(current)
            }
            count
        }.getOrDefault(0)
    }

    suspend fun delete(profileId: String) {
        store.edit { prefs ->
            val current = decode(prefs[Keys.profilesJson].orEmpty())
            prefs[Keys.profilesJson] = encode(current.filter { it.id != profileId })
        }
    }

    // ── serialisation ────────────────────────────────────────────────────────

    private fun encode(profiles: List<VpnProfile>): String {
        val encrypted = profiles.map { p ->
            p.copy(
                password = if (p.savePassword && p.password.isNotBlank())
                    SecretBox.encrypt(p.password) else "",
                tokenString = if (p.tokenString.isNotBlank())
                    SecretBox.encrypt(p.tokenString) else "",
            )
        }
        return json.encodeToString(encrypted)
    }

    private fun decode(raw: String): List<VpnProfile> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<VpnProfile>>(raw).map { p ->
                val decryptedPassword = SecretBox.decrypt(p.password)
                val decryptedToken = if (p.tokenString.isNotBlank()) {
                    val d = SecretBox.decrypt(p.tokenString)
                    if (d.isEmpty()) p.tokenString else d
                } else ""
                p.copy(
                    password = decryptedPassword,
                    tokenString = decryptedToken,
                )
            }
        }.getOrElse { emptyList() }
    }

    companion object {
        @Volatile private var instance: ProfileStore? = null
        fun get(context: Context): ProfileStore =
            instance ?: synchronized(this) {
                instance ?: ProfileStore(context.applicationContext).also { instance = it }
            }
    }
}
