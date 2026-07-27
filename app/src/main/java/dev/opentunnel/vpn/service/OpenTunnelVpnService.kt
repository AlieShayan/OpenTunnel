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
import androidx.core.app.ServiceCompat
import androidx.core.content.getSystemService
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.core.Interaction
import dev.opentunnel.vpn.core.NativeLibrary
import dev.opentunnel.vpn.core.TunnelHost
import dev.opentunnel.vpn.core.TunnelRunner
import dev.opentunnel.vpn.core.VpnBus
import dev.opentunnel.vpn.data.Repository
import dev.opentunnel.vpn.util.SystemCaBundle
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
 */
class OpenTunnelVpnService : VpnService(), TunnelHost {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repository: Repository

    @Volatile
    private var runner: TunnelRunner? = null

    private var statsJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastNetworkId: Long = -1L
    private var reconnectOnNetworkChange = true
    private var showStatsInNotification = true

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
        runner?.requestDisconnect()
        scope.cancel()
        super.onDestroy()
    }

    // ── lifecycle ───────────────────────────────────────────────────────────

    private fun startTunnel() {
        if (runner != null) {
            VpnBus.info("Tunnel is already running")
            return
        }

        VpnBus.reset()
        VpnBus.setStage(ConnectionStage.PREPARING)
        goForeground()

        scope.launch {
            val profile = repository.currentProfile()
            val settings = repository.currentSettings()
            reconnectOnNetworkChange = settings.reconnectOnNetworkChange
            showStatsInNotification = settings.showStatsInNotification

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
            tunnel.start()
        }
    }

    private fun stopTunnel() {
        val tunnel = runner
        if (tunnel == null) {
            stopSelfSafely()
            return
        }
        tunnel.requestDisconnect()
        // onTunnelFinished() tears the service down once the thread unwinds.
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
        statsJob?.cancel()
        statsJob = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
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

    // ── observers ───────────────────────────────────────────────────────────

    private fun observeStatus() {
        scope.launch {
            combine(VpnBus.status, VpnBus.stats) { status, stats -> status to stats }
                .collect { (status, stats) ->
                    if (runner == null && !status.stage.isActive) return@collect
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
    }

    /** Nudge the user back into the app when the tunnel needs an answer. */
    private fun observePrompts() {
        scope.launch {
            Interaction.pending.collect { prompt ->
                if (prompt == null) {
                    Notifications.clearActionRequired(this@OpenTunnelVpnService)
                } else {
                    Notifications.postActionRequired(
                        this@OpenTunnelVpnService,
                        when (prompt) {
                            is dev.opentunnel.vpn.core.UserPrompt.Auth ->
                                "The VPN gateway is asking for more sign-in details."
                            is dev.opentunnel.vpn.core.UserPrompt.CertTrust ->
                                "The gateway's certificate needs to be reviewed before connecting."
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
            }

            override fun onLost(network: Network) {
                VpnBus.info("Network lost")
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

    // ── TunnelHost ──────────────────────────────────────────────────────────

    override fun newBuilder(): Builder = Builder()

    override fun protectSocket(socket: Int): Boolean = protect(socket)

    override fun caBundlePath(): String? = SystemCaBundle.ensure(this)

    override fun persistCertificatePin(fingerprint: String) {
        scope.launch { repository.pinCertificate(fingerprint) }
    }

    override fun onTunnelFinished(error: String?) {
        runner = null
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
        const val ACTION_CONNECT = "dev.opentunnel.vpn.CONNECT"
        const val ACTION_DISCONNECT = "dev.opentunnel.vpn.DISCONNECT"
        const val ACTION_RECONNECT = "dev.opentunnel.vpn.RECONNECT"

        private const val STATS_INTERVAL_MS = 2_000L
        private const val FORCE_STOP_AFTER_MS = 6_000L

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
