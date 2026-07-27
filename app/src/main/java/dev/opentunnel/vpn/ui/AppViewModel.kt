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

    /** Active (selected) profile — used by the connect flow. */
    val profile: StateFlow<VpnProfile> = repository.profile
        .stateIn(viewModelScope, SharingStarted.Eagerly, VpnProfile())

    /** Full list of profiles — used by the profile-picker UI. */
    val profiles: StateFlow<List<VpnProfile>> = repository.profiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledApp>?>(null)
    val installedApps: StateFlow<List<InstalledApp>?> = _installedApps.asStateFlow()

    // ── profile management ──────────────────────────────────────────────────

    fun saveProfile(profile: VpnProfile) {
        viewModelScope.launch { repository.saveProfile(profile) }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch { repository.deleteProfile(profileId) }
    }

    fun setActiveProfile(profileId: String) {
        viewModelScope.launch { repository.setActiveProfile(profileId) }
    }

    fun forgetPinnedCertificate() {
        viewModelScope.launch { repository.pinCertificate("") }
    }

    // ── settings ────────────────────────────────────────────────────────────

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
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

    // ── split tunnelling ─────────────────────────────────────────────────────

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

    // ── prompts ─────────────────────────────────────────────────────────────

    fun submitPrompt(values: Map<String, String>) {
        Interaction.submit(PromptResult.Values(values))
    }

    fun acceptPrompt() {
        Interaction.submit(PromptResult.Accept)
    }

    fun cancelPrompt() {
        Interaction.submit(PromptResult.Cancel)
    }

    // ── logs ────────────────────────────────────────────────────────────────

    fun clearLogs() {
        VpnBus.clearLogs()
    }
}
