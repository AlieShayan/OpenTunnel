package dev.opentunnel.vpn.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.core.Interaction
import dev.opentunnel.vpn.core.NativeLibrary
import dev.opentunnel.vpn.core.TunnelHost
import dev.opentunnel.vpn.core.TunnelRunner
import dev.opentunnel.vpn.core.VpnBus
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.data.Repository
import dev.opentunnel.vpn.util.LocationResolver
import dev.opentunnel.vpn.util.Strings
import dev.opentunnel.vpn.util.SystemCaBundle
import dev.opentunnel.vpn.widget.TunnelWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the tunnel's lifetime: foreground notification, network monitoring, and
 * the worker thread that runs libopenconnect.
 *
 * Stability guarantees:
 * - A PARTIAL_WAKE_LOCK is held for the entire tunnel lifetime so the NDK thread
 *   never freezes when the screen turns off or the device enters deep sleep.
 * - Battery optimisation exemption is requested once after the tunnel first
 *   reaches CONNECTED so subsequent reconnects are not delayed by Doze.
 * - FORCE_STOP_AFTER_MS is generous (12 s) so TLS teardown can finish cleanly
 *   on slow networks before the hard kill path fires.
 */
class OpenTunnelVpnService : VpnService(), TunnelHost {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: Repository

    @Volatile
    private var runner: TunnelRunner? = null

    /** True while startTunnel() coroutine is in flight but before TunnelRunner is assigned. */
    @Volatile
    private var startingUp = false

    private var statsJob: Job? = null
    private var locationJob: Job? = null
    private var pingJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastNetworkId: Long = -1L
    private var reconnectOnNetworkChange = true
    private var showStatsInNotification = true

    /** Cached language preference so notifications can be localised without a suspend call. */
    private var appLanguage: AppLanguage = AppLanguage.SYSTEM

    /**
     * PARTIAL_WAKE_LOCK keeps the CPU running while the screen is off so the
     * NDK tunnel thread inside libopenconnect never freezes mid-session.
     * Acquired in startTunnel(), released in stopSelfSafely().
     */
    private var wakeLock: PowerManager.WakeLock? = null

    /** True once we've already asked the user for battery-opt exemption this session. */
    private var batteryOptRequested = false

    override fun onCreate() {
        super.onCreate()
        repository = Repository.get(this)
        Notifications.createChannels(this)
        observeStatus()
        observePrompts()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                stopTunnel()
                return START_NOT_STICKY
            }

            ACTION_RECONNECT -> {
                runner?.requestReconnect()
                return START_STICKY
            }

            else -> startTunnel()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? =
        if (VpnService.SERVICE_INTERFACE == intent?.action) super.onBind(intent) else null

    override fun onRevoke() {
        VpnBus.info("VPN permission was revoked by the system")
        stopTunnel()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopNetworkMonitoring()
        statsJob?.cancel()
        locationJob?.cancel()
        runner?.requestDisconnect()
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    // ── lifecycle ──────────────────────────────────────────────────────

    private fun startTunnel() {
        if (startingUp || runner != null) {
            VpnBus.info("Tunnel is already running or starting")
            return
        }

        VpnBus.reset()
        VpnBus.setStage(ConnectionStage.PREPARING)
        goForeground()
        acquireWakeLock()

        scope.launch {
            startingUp = true
            try {
                val profile = repository.currentProfile()
                val settings = repository.currentSettings()
                reconnectOnNetworkChange = settings.reconnectOnNetworkChange
                showStatsInNotification = settings.showStatsInNotification
                appLanguage = settings.appLanguage

                if (!profile.isComplete) {
                    VpnBus.setError("Add a server address and username before connecting.")
                    stopSelfSafely()
                    return@launch
                }
                if (!NativeLibrary.isAvailable) {
                    VpnBus.setError(NativeLibrary.MISSING_MESSAGE)
                    stopSelfSafely()
                    return@launch
                }

                val tunnel = TunnelRunner(this@OpenTunnelVpnService, profile, settings)
                runner = tunnel
                startNetworkMonitoring()
                startStatsPolling()
                startPingPolling()
                tunnel.start()
            } finally {
                startingUp = false
            }
        }
    }

    private fun stopTunnel() {
        val tunnel = runner
        if (tunnel == null) {
            stopSelfSafely()
            return
        }
        tunnel.requestDisconnect()
        scope.launch {
            delay(FORCE_STOP_AFTER_MS)
            if (runner === tunnel) {
                VpnBus.info("Tunnel did not stop cleanly; forcing shutdown")
                onTunnelFinished(null)
            }
        }
    }

    private fun stopSelfSafely() {
        stopNetworkMonitoring()
        statsJob?.cancel(); statsJob = null
        locationJob?.cancel(); locationJob = null
        pingJob?.cancel(); pingJob = null
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        TunnelWidget.notifyAll(this)
        stopSelf()
    }

    private fun goForeground() {
        val notification = Notifications.buildStatus(
            context = this,
            status = VpnBus.status.value,
            stats = VpnBus.stats.value,
            showStats = showStatsInNotification,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                Notifications.STATUS_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
            )
        } else {
            startForeground(Notifications.STATUS_NOTIFICATION_ID, notification)
        }
    }

    // ── WakeLock ──────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService<PowerManager>() ?: return
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "OpenTunnel:TunnelWakeLock",
        ).also {
            it.setReferenceCounted(false)
            it.acquire(6 * 60 * 60 * 1000L)   // 6 hours max, released in stopSelfSafely() / onDestroy()
        }
        VpnBus.info("WakeLock acquired — tunnel will stay active during screen-off (max 6h)")
    }

    private fun releaseWakeLock() {
        val wl = wakeLock ?: return
        wakeLock = null
        if (wl.isHeld) {
            runCatching { wl.release() }
            VpnBus.info("WakeLock released")
        }
    }

    // ── Battery optimisation ─────────────────────────────────────────────

    /**
     * Shows the system "Ignore battery optimisations?" dialog once, the first
     * time the tunnel reaches CONNECTED. We do this after CONNECTED (not at
     * startup) so the user already sees the VPN working and understands why
     * the permission matters.
     */
    private fun requestBatteryOptExemptionIfNeeded() {
        if (batteryOptRequested) return
        batteryOptRequested = true
        val pm = getSystemService<PowerManager>() ?: return
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            VpnBus.info("Battery optimisation already exempted")
            return
        }
        runCatching {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            VpnBus.info("Requested battery optimisation exemption")
        }.onFailure {
            VpnBus.info("Could not open battery optimisation dialog: ${it.message}")
        }
    }

    private var oemAutoStartRequested = false

    private fun requestOemAutoStart() {
        if (oemAutoStartRequested) return
        oemAutoStartRequested = true

        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                Intent().apply {
                    setComponent(android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
                }
            }
            manufacturer.contains("samsung") -> {
                Intent().apply {
                    setComponent(android.content.ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"))
                }
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                Intent().apply {
                    setComponent(android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"))
                }
            }
            else -> null
        }

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (packageManager.resolveActivity(intent, 0) != null) {
                runCatching {
                    startActivity(intent)
                    VpnBus.info("Opened OEM Auto-Start settings for $manufacturer")
                }.onFailure {
                    VpnBus.debug("Could not launch OEM Auto-Start screen: ${it.message}")
                }
            } else {
                VpnBus.debug("OEM Auto-Start screen not available on this ROM ($manufacturer)")
            }
        }
    }

    // ── observers ─────────────────────────────────────────────────────────

    private var lastNotifyTime = 0L
    private var lastStageNotified: ConnectionStage? = null

    private fun observeStatus() {
        scope.launch {
            combine(VpnBus.status, VpnBus.stats) { status, stats -> status to stats }
                .collect { (status, stats) ->
                    val now = android.os.SystemClock.elapsedRealtime()
                    val stageChanged = status.stage != lastStageNotified
                    if (stageChanged || (now - lastNotifyTime >= 2000L)) {
                        lastNotifyTime = now
                        lastStageNotified = status.stage
                        if (runner != null || status.stage.isActive) {
                            runCatching {
                                val manager = getSystemService<NotificationManager>()
                                manager?.notify(
                                    Notifications.STATUS_NOTIFICATION_ID,
                                    Notifications.buildStatus(
                                        this@OpenTunnelVpnService,
                                        status,
                                        stats,
                                        showStatsInNotification,
                                    ),
                                )
                            }
                        }
                    }
                    TunnelWidget.notifyAll(this@OpenTunnelVpnService)

                    if (status.stage == ConnectionStage.CONNECTED) {
                        // Ask once for battery-opt exemption so Doze never
                        // throttles the tunnel after the first screen-off.
                        requestBatteryOptExemptionIfNeeded()
                        requestOemAutoStart()

                        if (stageChanged) {
                            locationAttemptCount = 0
                            locationJob?.cancel()
                        }

                        if (status.info.locationName == null &&
                            repository.currentSettings().enableGeoIpLookup
                        ) {
                            startLocationLookup()
                        }
                    }
                }
        }
    }

    private var locationAttemptCount = 0

    private fun startLocationLookup() {
        if (locationAttemptCount >= MAX_LOCATION_RETRIES) return
        if (locationJob?.isActive == true) return
        locationJob = scope.launch {
            VpnBus.info("Resolving connection location\u2026")
            delay(800L)
            val loc = LocationResolver.resolve()
            if (loc != null) {
                locationAttemptCount = 0
                VpnBus.updateInfo { info ->
                    info.copy(
                        outboundIp = loc.ip.ifBlank { info.ipv4 ?: info.ipv6 },
                        locationName = "${loc.country}, ${loc.city}",
                        locationFlag = loc.flagEmoji,
                    )
                }
                VpnBus.info("Location: ${loc.displayLine}")
                TunnelWidget.notifyAll(this@OpenTunnelVpnService)
            } else {
                locationAttemptCount++
                VpnBus.info("Could not resolve connection location (attempt $locationAttemptCount/$MAX_LOCATION_RETRIES)")
                if (locationAttemptCount < MAX_LOCATION_RETRIES) {
                    delay(5_000L)
                    startLocationLookup()
                }
            }
        }
    }

    private fun observePrompts() {
        scope.launch {
            Interaction.pending.collect { prompt ->
                if (prompt == null) {
                    Notifications.clearActionRequired(this@OpenTunnelVpnService)
                } else {
                    val lang = appLanguage
                    Notifications.postActionRequired(
                        this@OpenTunnelVpnService,
                        when (prompt) {
                            is dev.opentunnel.vpn.core.UserPrompt.Auth ->
                                Strings.promptAuthNotification(lang)
                            is dev.opentunnel.vpn.core.UserPrompt.CertTrust ->
                                Strings.promptCertTrustNotification(lang)
                        },
                    )
                }
            }
        }
    }

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (isActive) {
                delay(STATS_INTERVAL_MS)
                if (VpnBus.status.value.stage == ConnectionStage.CONNECTED) {
                    runner?.pollStats()
                }
            }
        }
    }

    private fun startPingPolling() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                if (VpnBus.status.value.stage == ConnectionStage.CONNECTED) {
                    val pingMs = measurePing()
                    VpnBus.updateInfo { it.copy(pingMs = pingMs) }
                    TunnelWidget.notifyAll(this@OpenTunnelVpnService)
                }
                delay(PING_INTERVAL_MS)
            }
        }
    }

    private fun measurePing(): Long {
        val serverIp = VpnBus.status.value.info.ipv4
        val targets = buildList {
            if (!serverIp.isNullOrBlank()) {
                add(serverIp to 443)
                add(serverIp to 80)
            }
            add("1.1.1.1" to 443)
            add("1.1.1.1" to 53)
            add("8.8.8.8" to 53)
            add("1.0.0.1" to 443)
        }
        for ((host, port) in targets) {
            val ms = runCatching {
                val start = android.os.SystemClock.elapsedRealtime()
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress(host, port), 1500)
                }
                android.os.SystemClock.elapsedRealtime() - start
            }.getOrDefault(-1L)
            if (ms >= 0) return ms
        }

        // UDP fallback if TCP fails
        val udpTargets = listOf("1.1.1.1" to 53, "8.8.8.8" to 53)
        for ((host, port) in udpTargets) {
            val ms = runCatching {
                val start = android.os.SystemClock.elapsedRealtime()
                java.net.DatagramSocket().use { ds ->
                    ds.soTimeout = 1500
                    val address = java.net.InetAddress.getByName(host)
                    val dummyData = ByteArray(32)
                    val packet = java.net.DatagramPacket(dummyData, dummyData.size, address, port)
                    ds.send(packet)
                    val respPacket = java.net.DatagramPacket(ByteArray(32), 32)
                    ds.receive(respPacket)
                }
                android.os.SystemClock.elapsedRealtime() - start
            }.getOrDefault(-1L)
            if (ms >= 0) return ms
        }
        return -1L
    }

    // ── network monitoring ──────────────────────────────────────────────────

    private fun startNetworkMonitoring() {
        if (networkCallback != null) return
        val connectivity = getSystemService<ConnectivityManager>() ?: return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val id = network.networkHandle
                if (lastNetworkId != -1L && lastNetworkId != id && reconnectOnNetworkChange) {
                    VpnBus.info("Underlying network changed — re-establishing the tunnel")
                    runner?.requestReconnect()
                }
                lastNetworkId = id
                runCatching { setUnderlyingNetworks(arrayOf(network)) }
            }

            override fun onLost(network: Network) {
                VpnBus.info("Network lost")
                runCatching { setUnderlyingNetworks(null) }
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()

        runCatching { connectivity.registerNetworkCallback(request, callback) }
            .onSuccess { networkCallback = callback }
    }

    private fun stopNetworkMonitoring() {
        val callback = networkCallback ?: return
        networkCallback = null
        runCatching { getSystemService<ConnectivityManager>()?.unregisterNetworkCallback(callback) }
    }

    // ── TunnelHost ───────────────────────────────────────────────────────────

    override fun newBuilder(): Builder = Builder()

    override fun protectSocket(socket: Int): Boolean = protect(socket)

    override fun caBundlePath(): String? = SystemCaBundle.ensure(this)

    override fun persistCertificatePin(fingerprint: String) {
        scope.launch { repository.pinCertificate(fingerprint) }
    }

    override fun onTunnelFinished(error: String?) {
        runner = null
        locationAttemptCount = 0
        locationJob?.cancel()
        locationJob = null
        scope.launch {
            if (error != null) {
                VpnBus.setError(error)
                Notifications.postActionRequired(this@OpenTunnelVpnService, error)
            } else {
                VpnBus.reset()
                Notifications.clearActionRequired(this@OpenTunnelVpnService)
            }
            stopSelfSafely()
        }
    }

    companion object {
        const val ACTION_CONNECT    = "dev.opentunnel.vpn.CONNECT"
        const val ACTION_DISCONNECT = "dev.opentunnel.vpn.DISCONNECT"
        const val ACTION_RECONNECT  = "dev.opentunnel.vpn.RECONNECT"

        private const val STATS_INTERVAL_MS   = 1_000L
        private const val PING_INTERVAL_MS    = 4_000L

        /** Grace period before a hard kill when stopTunnel() is called. */
        private const val FORCE_STOP_AFTER_MS = 12_000L

        /** Maximum consecutive geo-IP lookup failures before giving up. */
        private const val MAX_LOCATION_RETRIES = 3

        fun connect(context: Context) {
            val intent = Intent(context, OpenTunnelVpnService::class.java).setAction(ACTION_CONNECT)
            context.startForegroundService(intent)
        }

        fun disconnect(context: Context) {
            val intent = Intent(context, OpenTunnelVpnService::class.java).setAction(ACTION_DISCONNECT)
            runCatching { context.startService(intent) }
        }

        fun reconnect(context: Context) {
            val intent = Intent(context, OpenTunnelVpnService::class.java).setAction(ACTION_RECONNECT)
            runCatching { context.startService(intent) }
        }
    }
}
