package dev.opentunnel.vpn.core

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import dev.opentunnel.vpn.data.AppSettings
import dev.opentunnel.vpn.data.AppTrafficEntry
import dev.opentunnel.vpn.data.AppTrafficSummary
import dev.opentunnel.vpn.data.SplitTunnelMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Checks whether the app has been granted [AppOpsManager.OPSTR_GET_USAGE_STATS] permission.
 */
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * Directs the user to the system Settings page for Usage Access.
 */
fun openUsageAccessSettings(context: Context) {
    val intentWithPackage = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(intentWithPackage)
    }.recoverCatching {
        context.startActivity(fallbackIntent)
    }
}

/**
 * Raw counter snapshot for a single UID.
 */
data class UidTrafficBytes(
    val rxBytes: Long,
    val txBytes: Long,
)

/**
 * Abstraction for querying UID traffic stats from the OS.
 * Allows deterministic unit testing without Android OS kernel dependencies.
 */
interface TrafficStatsProvider {
    fun getUidRxBytes(uid: Int): Long
    fun getUidTxBytes(uid: Int): Long
    fun getAllUidStats(): Map<Int, UidTrafficBytes>? = null
    fun getElapsedRealtime(): Long
    fun hasPermission(): Boolean = true
}

/**
 * Production implementation using [NetworkStatsManager] (API 23+) with bulk querying
 * and [TrafficStats] for the app's own process.
 */
class AndroidTrafficStatsProvider(
    private val context: Context? = null,
) : TrafficStatsProvider {

    override fun hasPermission(): Boolean {
        val ctx = context ?: return false
        return hasUsageStatsPermission(ctx)
    }

    override fun getAllUidStats(): Map<Int, UidTrafficBytes>? {
        val ctx = context ?: return null
        if (!hasUsageStatsPermission(ctx)) {
            val myUid = Process.myUid()
            val myRx = TrafficStats.getUidRxBytes(myUid).coerceAtLeast(0L)
            val myTx = TrafficStats.getUidTxBytes(myUid).coerceAtLeast(0L)
            return mapOf(myUid to UidTrafficBytes(myRx, myTx))
        }

        val nsm = ctx.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager ?: return null
        val now = System.currentTimeMillis()
        val results = mutableMapOf<Int, MutableBytes>()

        // 1. Wi-Fi (TYPE_WIFI)
        querySummaryInto(nsm, ConnectivityManager.TYPE_WIFI, now, results)

        // 2. Mobile Data (TYPE_MOBILE)
        querySummaryInto(nsm, ConnectivityManager.TYPE_MOBILE, now, results)

        // 3. Ethernet (TYPE_ETHERNET = 9)
        querySummaryInto(nsm, 9, now, results)

        // 4. VPN (TYPE_VPN = 17)
        querySummaryInto(nsm, 17, now, results)

        // Ensure own UID is tracked accurately even if NetworkStatsManager has a small flush lag
        val myUid = Process.myUid()
        val myRx = TrafficStats.getUidRxBytes(myUid)
        val myTx = TrafficStats.getUidTxBytes(myUid)
        if (myRx > 0L || myTx > 0L) {
            val entry = results.getOrPut(myUid) { MutableBytes() }
            if (myRx > entry.rx) entry.rx = myRx
            if (myTx > entry.tx) entry.tx = myTx
        }

        return results.mapValues { (_, v) -> UidTrafficBytes(v.rx, v.tx) }
    }

    private class MutableBytes(var rx: Long = 0L, var tx: Long = 0L)

    private fun querySummaryInto(
        nsm: NetworkStatsManager,
        networkType: Int,
        endTime: Long,
        outMap: MutableMap<Int, MutableBytes>,
    ) {
        runCatching {
            val stats: NetworkStats = nsm.querySummary(networkType, null, 0L, endTime)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val uid = bucket.uid
                if (uid > 0) {
                    val entry = outMap.getOrPut(uid) { MutableBytes() }
                    entry.rx += bucket.rxBytes
                    entry.tx += bucket.txBytes
                }
            }
            stats.close()
        }
    }

    override fun getUidRxBytes(uid: Int): Long {
        val myUid = Process.myUid()
        if (uid == myUid) {
            val bytes = TrafficStats.getUidRxBytes(uid)
            if (bytes != TrafficStats.UNSUPPORTED.toLong() && bytes >= 0) return bytes
        }
        return 0L
    }

    override fun getUidTxBytes(uid: Int): Long {
        val myUid = Process.myUid()
        if (uid == myUid) {
            val bytes = TrafficStats.getUidTxBytes(uid)
            if (bytes != TrafficStats.UNSUPPORTED.toLong() && bytes >= 0) return bytes
        }
        return 0L
    }

    override fun getElapsedRealtime(): Long = SystemClock.elapsedRealtime()
}

/**
 * Package metadata cached to avoid continuous PackageManager queries on every tick.
 */
data class AppMetadata(
    val packageName: String,
    val uid: Int,
    val label: String,
    val isSystem: Boolean,
)

/**
 * Internal tracking record for one UID across sampling ticks.
 */
private data class UidTracker(
    val packageName: String,
    val uid: Int,
    val label: String,
    val isSystem: Boolean,
    var baselineRx: Long = 0L,
    var baselineTx: Long = 0L,
    var prevRx: Long = 0L,
    var prevTx: Long = 0L,
    var prevSampleTime: Long = 0L,
    var rxRate: Long = 0L,
    var txRate: Long = 0L,
    var zeroRxCount: Int = 0,
    var zeroTxCount: Int = 0,
    var firstSeenTime: Long = 0L,
    var lastActiveTime: Long = 0L,
)

/**
 * Background engine responsible for polling per-UID network counters, calculating
 * smoothed bandwidth rates via Exponential Moving Average (EMA), and publishing
 * immutable [AppTrafficEntry] snapshots via [StateFlow].
 */
class AppTrafficCollector(
    private val context: Context? = null,
    private val statsProvider: TrafficStatsProvider = AndroidTrafficStatsProvider(context),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val sampleIntervalMs: Long = 1000L,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutex = Mutex()

    private var pollJob: Job? = null

    private val _entries = MutableStateFlow<List<AppTrafficEntry>>(emptyList())
    val entries: StateFlow<List<AppTrafficEntry>> = _entries.asStateFlow()

    private val _summary = MutableStateFlow(AppTrafficSummary())
    val summary: StateFlow<AppTrafficSummary> = _summary.asStateFlow()

    private val trackers = mutableMapOf<String, UidTracker>()
    private var sessionStartTime = statsProvider.getElapsedRealtime()
    private var isPaused = false

    @Volatile
    private var appSettings: AppSettings = AppSettings()

    companion object {
        private const val EMA_ALPHA = 0.70f // Weight given to current sample (responsive yet smooth)
    }

    /**
     * Updates the active settings (for split-tunneling routing tags).
     */
    fun updateSettings(settings: AppSettings) {
        appSettings = settings
    }

    /**
     * Initializes or reloads package metadata.
     */
    suspend fun loadApps(packages: List<AppMetadata>) = mutex.withLock {
        val now = statsProvider.getElapsedRealtime()
        val bulkStats = statsProvider.getAllUidStats()
        for (pkg in packages) {
            if (pkg.uid <= 0) continue
            if (!trackers.containsKey(pkg.packageName)) {
                val currentRx = bulkStats?.get(pkg.uid)?.rxBytes ?: statsProvider.getUidRxBytes(pkg.uid)
                val currentTx = bulkStats?.get(pkg.uid)?.txBytes ?: statsProvider.getUidTxBytes(pkg.uid)
                trackers[pkg.packageName] = UidTracker(
                    packageName = pkg.packageName,
                    uid = pkg.uid,
                    label = pkg.label,
                    isSystem = pkg.isSystem,
                    baselineRx = currentRx,
                    baselineTx = currentTx,
                    prevRx = currentRx,
                    prevTx = currentTx,
                    prevSampleTime = now,
                    firstSeenTime = now,
                    lastActiveTime = if (currentRx > 0 || currentTx > 0) now else 0L,
                )
            }
        }
    }

    /**
     * Discovers all installed apps via PackageManager without heavy metadata bundles to prevent Binder IPC limits.
     */
    suspend fun discoverInstalledApps(): List<AppMetadata> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext emptyList()
        val pm = ctx.packageManager
        val selfPkg = ctx.packageName

        val packages: List<ApplicationInfo> = runCatching {
            pm.getInstalledApplications(0)
        }.recoverCatching {
            pm.getInstalledPackages(0).map { it.applicationInfo }
        }.getOrElse { emptyList() }

        if (packages.isEmpty()) return@withContext emptyList()

        packages.asSequence()
            .filter { info ->
                info.packageName != selfPkg &&
                    info.uid > 0 &&
                    ((info.flags and ApplicationInfo.FLAG_SYSTEM) == 0 ||
                        (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
            }
            .map { info ->
                val label = runCatching { pm.getApplicationLabel(info).toString() }
                    .getOrDefault(info.packageName)
                val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                AppMetadata(
                    packageName = info.packageName,
                    uid = info.uid,
                    label = if (label.isBlank()) info.packageName else label,
                    isSystem = isSystem,
                )
            }
            .toList()
    }

    /**
     * Starts the periodic background collection loop.
     */
    fun start() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                if (trackers.isEmpty() && context != null) {
                    val apps = discoverInstalledApps()
                    if (apps.isNotEmpty()) {
                        loadApps(apps)
                    }
                }
                if (!isPaused && trackers.isNotEmpty()) {
                    tick()
                }
                delay(sampleIntervalMs)
            }
        }
    }

    /**
     * Pauses the collection loop without clearing accumulated statistics.
     */
    fun pause() {
        isPaused = true
    }

    /**
     * Resumes the collection loop.
     */
    fun resume() {
        isPaused = false
    }

    /**
     * Stops the collection engine and cancels the background coroutine.
     */
    fun stop() {
        pollJob?.cancel()
        pollJob = null
    }

    /**
     * Resets all session statistics to zero and establishes a new baseline from current counters.
     */
    suspend fun reset() = mutex.withLock {
        val now = statsProvider.getElapsedRealtime()
        sessionStartTime = now
        val bulkStats = statsProvider.getAllUidStats()
        for (tracker in trackers.values) {
            val currentRx = bulkStats?.get(tracker.uid)?.rxBytes ?: statsProvider.getUidRxBytes(tracker.uid)
            val currentTx = bulkStats?.get(tracker.uid)?.txBytes ?: statsProvider.getUidTxBytes(tracker.uid)
            tracker.baselineRx = currentRx
            tracker.baselineTx = currentTx
            tracker.prevRx = currentRx
            tracker.prevTx = currentTx
            tracker.prevSampleTime = now
            tracker.rxRate = 0L
            tracker.txRate = 0L
            tracker.zeroRxCount = 0
            tracker.zeroTxCount = 0
        }
        _entries.value = emptyList()
        _summary.value = AppTrafficSummary(sessionStartTime = now)
    }

    /**
     * Executes one sampling iteration. Visible for testing.
     */
    suspend fun tick() = mutex.withLock {
        val now = statsProvider.getElapsedRealtime()
        val currentSettings = appSettings
        val selectedPackages = currentSettings.selectedPackages
        val splitEnabled = currentSettings.splitTunnelEnabled
        val splitMode = currentSettings.splitTunnelMode

        var totalRxSession = 0L
        var totalTxSession = 0L
        var aggregateRxRate = 0L
        var aggregateTxRate = 0L
        var activeApps = 0

        val bulkStats = statsProvider.getAllUidStats()
        val snapshotList = ArrayList<AppTrafficEntry>(trackers.size)

        for (tracker in trackers.values) {
            val (currentRx, currentTx) = if (bulkStats != null) {
                val stats = bulkStats[tracker.uid]
                (stats?.rxBytes ?: 0L) to (stats?.txBytes ?: 0L)
            } else {
                statsProvider.getUidRxBytes(tracker.uid) to statsProvider.getUidTxBytes(tracker.uid)
            }

            val dtMs = (now - tracker.prevSampleTime).coerceAtLeast(1L)

            // If tracker had zero baseline (e.g. permission was just granted), establish initial baseline
            if (tracker.baselineRx == 0L && tracker.prevRx == 0L && currentRx > 0L) {
                tracker.baselineRx = currentRx
                tracker.prevRx = currentRx
            }
            if (tracker.baselineTx == 0L && tracker.prevTx == 0L && currentTx > 0L) {
                tracker.baselineTx = currentTx
                tracker.prevTx = currentTx
            }

            // Counter wrap or system reboot protection
            if (currentRx < tracker.prevRx) {
                tracker.baselineRx = currentRx
                tracker.prevRx = currentRx
            }
            if (currentTx < tracker.prevTx) {
                tracker.baselineTx = currentTx
                tracker.prevTx = currentTx
            }

            val deltaRx = (currentRx - tracker.prevRx).coerceAtLeast(0L)
            val deltaTx = (currentTx - tracker.prevTx).coerceAtLeast(0L)

            val rawRxRate = if (dtMs > 0) (deltaRx * 1000L) / dtMs else 0L
            val rawTxRate = if (dtMs > 0) (deltaTx * 1000L) / dtMs else 0L

            // Smooth RX Rate
            if (deltaRx == 0L) {
                tracker.zeroRxCount++
                if (tracker.zeroRxCount >= 2) {
                    tracker.rxRate = 0L
                } else {
                    tracker.rxRate = ((1f - EMA_ALPHA) * tracker.rxRate).toLong().coerceAtLeast(0L)
                }
            } else {
                tracker.zeroRxCount = 0
                tracker.rxRate = if (tracker.rxRate == 0L) {
                    rawRxRate
                } else {
                    (EMA_ALPHA * rawRxRate + (1f - EMA_ALPHA) * tracker.rxRate).toLong().coerceAtLeast(0L)
                }
            }

            // Smooth TX Rate
            if (deltaTx == 0L) {
                tracker.zeroTxCount++
                if (tracker.zeroTxCount >= 2) {
                    tracker.txRate = 0L
                } else {
                    tracker.txRate = ((1f - EMA_ALPHA) * tracker.txRate).toLong().coerceAtLeast(0L)
                }
            } else {
                tracker.zeroTxCount = 0
                tracker.txRate = if (tracker.txRate == 0L) {
                    rawTxRate
                } else {
                    (EMA_ALPHA * rawTxRate + (1f - EMA_ALPHA) * tracker.txRate).toLong().coerceAtLeast(0L)
                }
            }

            // Update tracker state
            tracker.prevRx = currentRx
            tracker.prevTx = currentTx
            tracker.prevSampleTime = now

            if (tracker.rxRate > 0 || tracker.txRate > 0) {
                tracker.lastActiveTime = now
                activeApps++
            }

            val sessionRx = (currentRx - tracker.baselineRx).coerceAtLeast(0L)
            val sessionTx = (currentTx - tracker.baselineTx).coerceAtLeast(0L)
            val appTotal = sessionRx + sessionTx

            totalRxSession += sessionRx
            totalTxSession += sessionTx
            aggregateRxRate += tracker.rxRate
            aggregateTxRate += tracker.txRate

            val isVpnRouted = when {
                !splitEnabled -> true
                splitMode == SplitTunnelMode.EXCLUDE_SELECTED -> tracker.packageName !in selectedPackages
                splitMode == SplitTunnelMode.INCLUDE_SELECTED -> tracker.packageName in selectedPackages
                else -> true
            }

            snapshotList.add(
                AppTrafficEntry(
                    packageName = tracker.packageName,
                    uid = tracker.uid,
                    label = tracker.label,
                    isSystem = tracker.isSystem,
                    isVpnRouted = isVpnRouted,
                    rxBytes = sessionRx,
                    txBytes = sessionTx,
                    totalBytes = appTotal,
                    rxRate = tracker.rxRate,
                    txRate = tracker.txRate,
                    totalRate = tracker.rxRate + tracker.txRate,
                    trafficSharePercent = 0f, // will be computed in second pass below
                    firstSeenTime = tracker.firstSeenTime,
                    lastActiveTime = tracker.lastActiveTime,
                    isActive = tracker.rxRate > 0 || tracker.txRate > 0,
                )
            )
        }

        val totalSessionAll = totalRxSession + totalTxSession

        // Second pass: compute traffic share percentage
        val finalizedEntries = if (totalSessionAll > 0L) {
            snapshotList.map { entry ->
                val share = ((entry.totalBytes.toDouble() / totalSessionAll.toDouble()) * 100.0).toFloat()
                    .coerceIn(0f, 100f)
                entry.copy(trafficSharePercent = share)
            }
        } else {
            snapshotList
        }

        _entries.value = finalizedEntries
        _summary.value = AppTrafficSummary(
            totalRxBytes = totalRxSession,
            totalTxBytes = totalTxSession,
            totalBytes = totalSessionAll,
            currentRxRate = aggregateRxRate,
            currentTxRate = aggregateTxRate,
            activeAppsCount = activeApps,
            totalAppsCount = trackers.size,
            sessionStartTime = sessionStartTime,
        )
    }

    /**
     * Cleans up resources when collector is destroyed.
     */
    fun destroy() {
        stop()
        scope.cancel()
    }
}
