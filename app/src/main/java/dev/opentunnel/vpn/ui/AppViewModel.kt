package dev.opentunnel.vpn.ui

import android.app.Application
import android.content.pm.PackageManager
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

    private val rxRingBuffer = LongRingBuffer(18000)
    private val txRingBuffer = LongRingBuffer(18000)

    private val _rxHistory = MutableStateFlow<List<Long>>(emptyList())
    val rxHistory: StateFlow<List<Long>> = _rxHistory.asStateFlow()

    private val _txHistory = MutableStateFlow<List<Long>>(emptyList())
    val txHistory: StateFlow<List<Long>> = _txHistory.asStateFlow()

    private var lastStage: dev.opentunnel.vpn.core.ConnectionStage? = null
    private var lastConnectedAt = 0L

    // ── app traffic monitor properties ───────────────────────────────────────

    private val trafficCollector = dev.opentunnel.vpn.core.AppTrafficCollector(
        context = application,
        dispatcher = kotlinx.coroutines.Dispatchers.Default,
        sampleIntervalMs = 1000L,
    )

    val appTrafficSummary: StateFlow<dev.opentunnel.vpn.data.AppTrafficSummary> = trafficCollector.summary

    private val _trafficSortBy = MutableStateFlow(dev.opentunnel.vpn.data.TrafficSortBy.TOTAL_TRAFFIC)
    val trafficSortBy: StateFlow<dev.opentunnel.vpn.data.TrafficSortBy> = _trafficSortBy.asStateFlow()

    private val _trafficSortDirection = MutableStateFlow(dev.opentunnel.vpn.data.SortDirection.DESCENDING)
    val trafficSortDirection: StateFlow<dev.opentunnel.vpn.data.SortDirection> = _trafficSortDirection.asStateFlow()

    private val _trafficFilterMode = MutableStateFlow(dev.opentunnel.vpn.data.TrafficFilterMode.ALL)
    val trafficFilterMode: StateFlow<dev.opentunnel.vpn.data.TrafficFilterMode> = _trafficFilterMode.asStateFlow()

    private val _trafficSearchQuery = MutableStateFlow("")
    val trafficSearchQuery: StateFlow<String> = _trafficSearchQuery.asStateFlow()

    private val _hasUsageAccessPermission = MutableStateFlow(
        dev.opentunnel.vpn.core.hasUsageStatsPermission(application),
    )
    val hasUsageAccessPermission: StateFlow<Boolean> = _hasUsageAccessPermission.asStateFlow()

    val appTrafficEntries: StateFlow<List<dev.opentunnel.vpn.data.AppTrafficEntry>> = kotlinx.coroutines.flow.combine(
        trafficCollector.entries,
        _trafficSortBy,
        _trafficSortDirection,
        _trafficFilterMode,
        _trafficSearchQuery,
    ) { rawEntries, sortBy, sortDir, filterMode, query ->
        val needle = query.trim().lowercase()
        val filtered = rawEntries.filter { entry ->
            val matchesQuery = needle.isEmpty() ||
                entry.label.lowercase().contains(needle) ||
                entry.packageName.lowercase().contains(needle)
            val matchesFilter = when (filterMode) {
                dev.opentunnel.vpn.data.TrafficFilterMode.ALL -> true
                dev.opentunnel.vpn.data.TrafficFilterMode.ACTIVE_ONLY -> entry.isActive || entry.totalBytes > 0L
            }
            matchesQuery && matchesFilter
        }

        val comparator: Comparator<dev.opentunnel.vpn.data.AppTrafficEntry> = when (sortBy) {
            dev.opentunnel.vpn.data.TrafficSortBy.TOTAL_TRAFFIC -> compareBy { it.totalBytes }
            dev.opentunnel.vpn.data.TrafficSortBy.DOWNLOAD -> compareBy { it.rxBytes }
            dev.opentunnel.vpn.data.TrafficSortBy.UPLOAD -> compareBy { it.txBytes }
            dev.opentunnel.vpn.data.TrafficSortBy.DOWNLOAD_SPEED -> compareBy { it.rxRate }
            dev.opentunnel.vpn.data.TrafficSortBy.UPLOAD_SPEED -> compareBy { it.txRate }
            dev.opentunnel.vpn.data.TrafficSortBy.APP_NAME -> compareBy(java.text.Collator.getInstance()) { it.label }
        }

        if (sortDir == dev.opentunnel.vpn.data.SortDirection.DESCENDING) {
            if (sortBy == dev.opentunnel.vpn.data.TrafficSortBy.APP_NAME) {
                filtered.sortedWith(comparator.reversed())
            } else {
                // Secondary sort by label so ties remain stable and don't flicker
                filtered.sortedWith(comparator.reversed().thenBy { it.label })
            }
        } else {
            filtered.sortedWith(comparator.thenBy { it.label })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearSpeedHistory() {
        rxRingBuffer.clear()
        txRingBuffer.clear()
        _rxHistory.value = emptyList()
        _txHistory.value = emptyList()
    }

    init {
        // Load installed apps immediately on startup
        loadInstalledApps()

        // One-time migration of legacy single-profile data.
        viewModelScope.launch { repository.migrateLegacyProfileIfNeeded() }

        viewModelScope.launch {
            installedApps.collect { list ->
                if (!list.isNullOrEmpty()) {
                    val pm = application.packageManager
                    val metaList = list.mapNotNull { app ->
                        val uid = runCatching {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                pm.getPackageUid(app.packageName, PackageManager.PackageInfoFlags.of(0))
                            } else {
                                @Suppress("DEPRECATION")
                                pm.getPackageUid(app.packageName, 0)
                            }
                        }.getOrDefault(0)
                        if (uid > 0) {
                            dev.opentunnel.vpn.core.AppMetadata(
                                packageName = app.packageName,
                                uid = uid,
                                label = app.label,
                                isSystem = app.isSystem,
                            )
                        } else null
                    }
                    if (metaList.isNotEmpty()) {
                        trafficCollector.loadApps(metaList)
                    }
                }
            }
        }

        viewModelScope.launch {
            status.collect { s ->
                val prevStage = lastStage
                val prevConnectedAt = lastConnectedAt
                lastStage = s.stage
                lastConnectedAt = s.connectedAtElapsed

                val isFreshConnection = (prevStage == null || prevStage == dev.opentunnel.vpn.core.ConnectionStage.IDLE || prevStage == dev.opentunnel.vpn.core.ConnectionStage.ERROR) &&
                    (s.stage == dev.opentunnel.vpn.core.ConnectionStage.PREPARING || s.stage == dev.opentunnel.vpn.core.ConnectionStage.CONNECTING)
                val newConnectionSession = s.connectedAtElapsed != 0L && s.connectedAtElapsed != prevConnectedAt

                if (isFreshConnection || newConnectionSession) {
                    clearSpeedHistory()
                }
            }
        }

        viewModelScope.launch {
            stats.collect { st ->
                if (status.value.stage == dev.opentunnel.vpn.core.ConnectionStage.CONNECTED) {
                    rxRingBuffer.add(st.rxRate)
                    txRingBuffer.add(st.txRate)

                    _rxHistory.value = rxRingBuffer.toSnapshot()
                    _txHistory.value = txRingBuffer.toSnapshot()
                }
            }
        }

        viewModelScope.launch {
            settings.collect { s ->
                trafficCollector.updateSettings(s)
            }
        }

        trafficCollector.start()
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

    // ── app traffic monitor ──────────────────────────────────────────────────

    fun setTrafficSortBy(sortBy: dev.opentunnel.vpn.data.TrafficSortBy) {
        _trafficSortBy.value = sortBy
    }

    fun toggleTrafficSortDirection() {
        _trafficSortDirection.value = if (_trafficSortDirection.value == dev.opentunnel.vpn.data.SortDirection.DESCENDING) {
            dev.opentunnel.vpn.data.SortDirection.ASCENDING
        } else {
            dev.opentunnel.vpn.data.SortDirection.DESCENDING
        }
    }

    fun setTrafficSortDirection(direction: dev.opentunnel.vpn.data.SortDirection) {
        _trafficSortDirection.value = direction
    }

    fun setTrafficFilterMode(mode: dev.opentunnel.vpn.data.TrafficFilterMode) {
        _trafficFilterMode.value = mode
    }

    fun setTrafficSearchQuery(query: String) {
        _trafficSearchQuery.value = query
    }

    fun resetTrafficStats() {
        viewModelScope.launch {
            trafficCollector.reset()
        }
    }

    fun pauseTrafficMonitoring() {
        trafficCollector.pause()
    }

    fun resumeTrafficMonitoring() {
        trafficCollector.resume()
    }

    fun checkUsageAccessPermission() {
        val granted = dev.opentunnel.vpn.core.hasUsageStatsPermission(getApplication())
        if (granted != _hasUsageAccessPermission.value) {
            _hasUsageAccessPermission.value = granted
            if (granted) {
                viewModelScope.launch {
                    trafficCollector.reset()
                }
            }
        }
    }

    fun openUsageAccessSettings() {
        dev.opentunnel.vpn.core.openUsageAccessSettings(getApplication())
    }

    override fun onCleared() {
        trafficCollector.destroy()
        super.onCleared()
    }
}

/**
 * Thread-safe fixed-capacity circular ring buffer backed by a primitive LongArray.
 * Generates unmodifiable snapshots as AbstractList without pre-allocating thousands of boxed Long objects,
 * reducing GC churn by >99% in high-frequency stats collection loops.
 */
class LongRingBuffer(private val capacity: Int) {
    private val buffer = LongArray(capacity)
    private var head = 0
    private var count = 0

    @Synchronized
    fun add(value: Long) {
        if (count < capacity) {
            buffer[(head + count) % capacity] = value
            count++
        } else {
            buffer[head] = value
            head = (head + 1) % capacity
        }
    }

    @Synchronized
    fun clear() {
        head = 0
        count = 0
    }

    @Synchronized
    fun toSnapshot(): List<Long> {
        if (count == 0) return emptyList()
        val result = LongArray(count)
        for (i in 0 until count) {
            result[i] = buffer[(head + i) % capacity]
        }
        return object : AbstractList<Long>() {
            override val size: Int get() = result.size
            override fun get(index: Int): Long = result[index]
        }
    }
}

