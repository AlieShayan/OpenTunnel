package dev.opentunnel.vpn.service

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dev.opentunnel.vpn.R
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.core.VpnBus
import dev.opentunnel.vpn.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Quick Settings toggle so the tunnel can be flipped without opening the app. */
class VpnTileService : TileService() {

    private var scope: CoroutineScope? = null
    private var job: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope = newScope
        job = newScope.launch {
            VpnBus.status.collectLatest { render(it.stage) }
        }
    }

    override fun onStopListening() {
        job?.cancel()
        job = null
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        val stage = VpnBus.status.value.stage
        if (stage.isActive) {
            OpenTunnelVpnService.disconnect(this)
            return
        }

        if (VpnService.prepare(this) != null) {
            // Consent has not been granted yet — that needs an activity.
            openApp()
            return
        }
        OpenTunnelVpnService.connect(this)
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun render(stage: ConnectionStage) {
        val tile = qsTile ?: return
        tile.state = when {
            stage == ConnectionStage.CONNECTED -> Tile.STATE_ACTIVE
            stage.isBusy -> Tile.STATE_UNAVAILABLE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when (stage) {
                ConnectionStage.CONNECTED -> getString(R.string.state_connected)
                ConnectionStage.ERROR -> getString(R.string.state_error)
                ConnectionStage.IDLE -> getString(R.string.state_disconnected)
                else -> getString(R.string.state_connecting)
            }
        }
        tile.updateTile()
    }
}
