package dev.opentunnel.vpn.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.opentunnel.vpn.core.Interaction
import dev.opentunnel.vpn.core.PromptResult
import dev.opentunnel.vpn.core.VpnBus
import dev.opentunnel.vpn.data.AppSettings
import dev.opentunnel.vpn.data.InstalledApp
import dev.opentunnel.vpn.data.InstalledApps
import dev.opentunnel.vpn.data.Repository
import dev.opentunnel.vpn.data.SplitTunnelMode
import dev.opentunnel.vpn.data.VpnProfile
import dev.opentunnel.vpn.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = Repository.get(application)

    val status = VpnBus.status
    val stats = VpnBus.stats
    val logs = VpnBus.logs
    val pendingPrompt = Interaction.pending

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** The currently active (selected) profile. */
    val profile: StateFlow<VpnProfile> = repository.activeProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, VpnProfile())

    /** All saved profiles, for the profile picker menu. */
    val profiles: StateFlow<List<VpnProfile>> = repository.profiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledApp>?>(null)
    val installedApps: StateFlow<List<InstalledApp>?> = _installedApps.asStateFlow()

    private val rxDeque = ArrayDeque<Long>(18000)
    private val txDeque = ArrayDeque<Long>(18000)

    private val _rxHistory = MutableStateFlow<List<Long>>(emptyList())
    val rxHistory: StateFlow<List<Long>> = _rxHistory.asStateFlow()

    private val _txHistory = MutableStateFlow<List<Long>>(emptyList())
    val txHistory: StateFlow<List<Long>> = _txHistory.asStateFlow()

    private var lastStage: dev.opentunnel.vpn.core.ConnectionStage? = null
    private var lastConnectedAt = 0L

    fun clearSpeedHistory() {
        rxDeque.clear()
        txDeque.clear()
        _rxHistory.value = emptyList()
        _txHistory.value = emptyList()
    }

    init {
        // One-time migration of legacy single-profile data.
        viewModelScope.launch { repository.migrateLegacyProfileIfNeeded() }

        viewModelScope.launch {
            status.collect { s ->
                val prevStage = lastStage
                val prevConnectedAt = lastConnectedAt
                lastStage = s.stage
                lastConnectedAt = s.connectedAtElapsed

                val stageReset = prevStage != s.stage && (
                    s.stage == dev.opentunnel.vpn.core.ConnectionStage.PREPARING ||
                    s.stage == dev.opentunnel.vpn.core.ConnectionStage.CONNECTING ||
                    s.stage == dev.opentunnel.vpn.core.ConnectionStage.RECONNECTING
                )
                val newConnectionSession = s.connectedAtElapsed != 0L && s.connectedAtElapsed != prevConnectedAt

                if (stageReset || newConnectionSession) {
                    clearSpeedHistory()
                }
            }
        }

        viewModelScope.launch {
            stats.collect { st ->
                if (status.value.stage == dev.opentunnel.vpn.core.ConnectionStage.CONNECTED) {
                    if (rxDeque.size >= 18000) rxDeque.removeFirst()
                    rxDeque.addLast(st.rxRate)

                    if (txDeque.size >= 18000) txDeque.removeFirst()
                    txDeque.addLast(st.txRate)

                    _rxHistory.value = rxDeque.toList()
                    _txHistory.value = txDeque.toList()
                }
            }
        }
    }

    private val _editingProfileId = MutableStateFlow<String?>(null)
    val editingProfileId: StateFlow<String?> = _editingProfileId.asStateFlow()

    fun setEditingProfileId(id: String?) {
        _editingProfileId.value = id
    }

    fun getProfile(id: String?): VpnProfile {
        if (id == null || id == "new" || id.isBlank()) return VpnProfile()
        return profiles.value.firstOrNull { it.id == id } ?: VpnProfile()
    }

    fun saveProfile(profile: VpnProfile) {
        viewModelScope.launch { repository.saveProfile(profile) }
    }

    fun reorderProfiles(profiles: List<VpnProfile>) {
        viewModelScope.launch { repository.reorderProfiles(profiles) }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch { repository.deleteProfile(profileId) }
    }

    fun selectProfile(profileId: String) {
        viewModelScope.launch { repository.setActiveProfile(profileId) }
    }

    fun exportProfiles(onResult: (String) -> Unit) {
        viewModelScope.launch { onResult(repository.exportProfilesJson()) }
    }

    fun importProfiles(jsonStr: String, onResult: (Int) -> Unit) {
        viewModelScope.launch { onResult(repository.importProfilesJson(jsonStr)) }
    }

    fun forgetPinnedCertificate() {
        viewModelScope.launch { repository.pinCertificate("") }
    }

    // ── settings ─────────────────────────────────────────────────────────────

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setAppLanguage(language: dev.opentunnel.vpn.data.AppLanguage) {
        viewModelScope.launch { repository.setAppLanguage(language) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun setSplitTunnelEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSplitTunnelEnabled(enabled) }
    }

    fun setSplitTunnelMode(mode: SplitTunnelMode) {
        viewModelScope.launch { repository.setSplitTunnelMode(mode) }
    }

    fun setBypassLocalNetworks(enabled: Boolean) {
        viewModelScope.launch { repository.setBypassLocalNetworks(enabled) }
    }

    fun setConnectOnBoot(enabled: Boolean) {
        viewModelScope.launch { repository.setConnectOnBoot(enabled) }
    }

    fun setReconnectOnNetworkChange(enabled: Boolean) {
        viewModelScope.launch { repository.setReconnectOnNetworkChange(enabled) }
    }

    fun setShowStatsInNotification(enabled: Boolean) {
        viewModelScope.launch { repository.setShowStatsInNotification(enabled) }
    }

    fun setVerboseLogging(enabled: Boolean) {
        viewModelScope.launch { repository.setVerboseLogging(enabled) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticFeedbackEnabled(enabled) }
    }

    // ── split tunnelling ──────────────────────────────────────────────────────

    fun loadInstalledApps() {
        if (_installedApps.value != null) return
        viewModelScope.launch {
            _installedApps.value = InstalledApps.load(getApplication())
        }
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = InstalledApps.load(getApplication())
        }
    }

    fun togglePackage(packageName: String, selected: Boolean) {
        viewModelScope.launch { repository.setPackageSelected(packageName, selected) }
    }

    fun clearSelectedPackages() {
        viewModelScope.launch { repository.setSelectedPackages(emptySet()) }
    }

    fun setSplitTunnelNetworksEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSplitTunnelNetworksEnabled(enabled) }
    }

    fun setSplitTunnelNetworksMode(mode: SplitTunnelMode) {
        viewModelScope.launch { repository.setSplitTunnelNetworksMode(mode) }
    }

    fun addSplitTunnelNetwork(network: String) {
        viewModelScope.launch { repository.addSplitTunnelNetwork(network) }
    }

    fun removeSplitTunnelNetwork(network: String) {
        viewModelScope.launch { repository.removeSplitTunnelNetwork(network) }
    }

    fun clearSplitTunnelNetworks() {
        viewModelScope.launch { repository.clearSplitTunnelNetworks() }
    }

    // ── prompts ──────────────────────────────────────────────────────────────

    fun submitPrompt(values: Map<String, String>) {
        Interaction.submit(PromptResult.Values(values))
    }

    fun acceptPrompt() {
        Interaction.submit(PromptResult.Accept)
    }

    fun cancelPrompt() {
        Interaction.submit(PromptResult.Cancel)
    }

    // ── logs ─────────────────────────────────────────────────────────────────

    fun clearLogs() {
        VpnBus.clearLogs()
    }
}
