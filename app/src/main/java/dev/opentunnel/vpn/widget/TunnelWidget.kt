package dev.opentunnel.vpn.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import dev.opentunnel.vpn.R
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.core.VpnBus
import dev.opentunnel.vpn.service.OpenTunnelVpnService
import java.util.concurrent.TimeUnit

/**
 * 2x2 (min) resizable home-screen widget for OpenTunnel.
 *
 * Shows current tunnel state, location info (flag + name), and a single
 * Connect/Disconnect/Cancel button. Tapping the orb icon opens the main activity.
 */
class TunnelWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val state = buildWidgetState()
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id, state)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE) {
            val stage = VpnBus.status.value.stage
            when {
                // Allow cancellation during connecting as well
                stage == ConnectionStage.CONNECTED || stage.isBusy ->
                    OpenTunnelVpnService.disconnect(context)
                else ->
                    OpenTunnelVpnService.connect(context)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_TOGGLE = "dev.opentunnel.vpn.WIDGET_TOGGLE"

        fun notifyAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, TunnelWidget::class.java)
            )
            if (ids.isEmpty()) return
            val state = buildWidgetState()
            ids.forEach { id -> updateWidget(context, manager, id, state) }
        }

        private fun buildWidgetState(): WidgetState {
            val status = VpnBus.status.value
            return when {
                status.stage == ConnectionStage.CONNECTED -> {
                    val elapsedMs = SystemClock.elapsedRealtime() - status.connectedAtElapsed
                    WidgetState.Connected(
                        serverHost = status.info.server ?: "VPN",
                        elapsed = formatElapsed(elapsedMs),
                        locationFlag = status.info.locationFlag.orEmpty(),
                        locationName = status.info.locationName.orEmpty(),
                    )
                }
                status.stage.isBusy -> WidgetState.Connecting
                else -> WidgetState.Disconnected
            }
        }

        private fun formatElapsed(ms: Long): String {
            val h = TimeUnit.MILLISECONDS.toHours(ms)
            val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
            val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
            return "%d:%02d:%02d".format(h, m, s)
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
            state: WidgetState,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_opentunnel)

            val launchPi = PendingIntent.getActivity(
                context,
                0,
                context.packageManager.getLaunchIntentForPackage(context.packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_orb, launchPi)

            val togglePi = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                Intent(context, TunnelWidget::class.java).setAction(ACTION_WIDGET_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_btn_toggle, togglePi)

            when (state) {
                is WidgetState.Disconnected -> {
                    views.setImageViewResource(R.id.widget_orb, R.drawable.ic_widget_orb_idle)
                    views.setTextViewText(
                        R.id.widget_status_label,
                        context.getString(R.string.widget_status_disconnected),
                    )
                    views.setTextColor(R.id.widget_status_label, 0xFFA9B4C9.toInt())
                    views.setViewVisibility(R.id.widget_timer, View.GONE)
                    views.setViewVisibility(R.id.widget_server, View.GONE)
                    views.setViewVisibility(R.id.widget_location, View.GONE)
                    views.setTextViewText(
                        R.id.widget_btn_toggle,
                        context.getString(R.string.widget_btn_connect),
                    )
                    views.setInt(
                        R.id.widget_btn_toggle,
                        "setBackgroundResource",
                        R.drawable.widget_btn_idle_bg,
                    )
                    views.setTextColor(R.id.widget_btn_toggle, 0xFFA9B4C9.toInt())
                    views.setInt(
                        R.id.widget_root,
                        "setBackgroundResource",
                        R.drawable.widget_bg,
                    )
                }

                is WidgetState.Connecting -> {
                    views.setImageViewResource(R.id.widget_orb, R.drawable.ic_widget_orb_connecting)
                    views.setTextViewText(
                        R.id.widget_status_label,
                        context.getString(R.string.widget_status_connecting),
                    )
                    views.setTextColor(R.id.widget_status_label, 0xFFFFC66B.toInt())
                    views.setViewVisibility(R.id.widget_timer, View.GONE)
                    views.setViewVisibility(R.id.widget_server, View.GONE)
                    views.setViewVisibility(R.id.widget_location, View.GONE)
                    // Show Cancel during connecting
                    views.setTextViewText(
                        R.id.widget_btn_toggle,
                        context.getString(R.string.widget_btn_cancel),
                    )
                    views.setInt(
                        R.id.widget_btn_toggle,
                        "setBackgroundResource",
                        R.drawable.widget_btn_idle_bg,
                    )
                    views.setTextColor(R.id.widget_btn_toggle, 0xFFFFC66B.toInt())
                    views.setInt(
                        R.id.widget_root,
                        "setBackgroundResource",
                        R.drawable.widget_bg,
                    )
                }

                is WidgetState.Connected -> {
                    views.setImageViewResource(R.id.widget_orb, R.drawable.ic_widget_orb_connected)
                    views.setTextViewText(
                        R.id.widget_status_label,
                        context.getString(R.string.widget_status_connected),
                    )
                    views.setTextColor(R.id.widget_status_label, 0xFF5EE7C4.toInt())
                    views.setViewVisibility(R.id.widget_server, View.VISIBLE)
                    views.setTextViewText(R.id.widget_server, state.serverHost)
                    views.setViewVisibility(R.id.widget_timer, View.VISIBLE)
                    views.setTextViewText(R.id.widget_timer, state.elapsed)
                    // Location badge
                    val locationText = buildString {
                        if (state.locationFlag.isNotBlank()) append("${state.locationFlag} ")
                        if (state.locationName.isNotBlank()) append(state.locationName)
                    }.trim()
                    if (locationText.isNotBlank()) {
                        views.setViewVisibility(R.id.widget_location, View.VISIBLE)
                        views.setTextViewText(R.id.widget_location, locationText)
                    } else {
                        views.setViewVisibility(R.id.widget_location, View.GONE)
                    }
                    views.setTextViewText(
                        R.id.widget_btn_toggle,
                        context.getString(R.string.widget_btn_disconnect),
                    )
                    views.setInt(
                        R.id.widget_btn_toggle,
                        "setBackgroundResource",
                        R.drawable.widget_btn_connected_bg,
                    )
                    views.setTextColor(R.id.widget_btn_toggle, 0xFF001F17.toInt())
                    views.setInt(
                        R.id.widget_root,
                        "setBackgroundResource",
                        R.drawable.widget_bg_connected,
                    )
                }
            }

            manager.updateAppWidget(appWidgetId, views)
        }
    }
}
