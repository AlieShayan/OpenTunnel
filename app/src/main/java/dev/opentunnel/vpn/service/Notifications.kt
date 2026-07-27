package dev.opentunnel.vpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.opentunnel.vpn.R
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.core.TrafficStats
import dev.opentunnel.vpn.core.TunnelStatus
import dev.opentunnel.vpn.ui.MainActivity
import dev.opentunnel.vpn.util.Formatters

object Notifications {

    const val STATUS_CHANNEL_ID = "tunnel_status"
    const val ALERT_CHANNEL_ID = "tunnel_alerts"

    const val STATUS_NOTIFICATION_ID = 1001
    const val ALERT_NOTIFICATION_ID = 1002

    fun createChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                STATUS_CHANNEL_ID,
                context.getString(R.string.channel_status),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_status_desc)
                setShowBadge(false)
                enableVibration(false)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                ALERT_CHANNEL_ID,
                context.getString(R.string.channel_alerts),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_alerts_desc)
            }
        )
    }

    private fun contentIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun disconnectIntent(context: Context): PendingIntent =
        PendingIntent.getService(
            context,
            1,
            Intent(context, OpenTunnelVpnService::class.java)
                .setAction(OpenTunnelVpnService.ACTION_DISCONNECT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun buildStatus(
        context: Context,
        status: TunnelStatus,
        stats: TrafficStats,
        showStats: Boolean,
    ): Notification {
        val titlePrefix = when (status.stage) {
            ConnectionStage.CONNECTED -> context.getString(R.string.notif_connected)
            ConnectionStage.RECONNECTING -> context.getString(R.string.notif_reconnecting)
            ConnectionStage.DISCONNECTING -> context.getString(R.string.notif_disconnecting)
            ConnectionStage.ERROR -> context.getString(R.string.notif_error)
            else -> context.getString(R.string.notif_connecting)
        }

        val serverName = status.info.profileDisplayName.takeIf { !it.isNullOrBlank() }
            ?: status.info.server
        val title = if (!serverName.isNullOrBlank()) "$titlePrefix · $serverName" else titlePrefix

        val subtitle = if (status.stage == ConnectionStage.CONNECTED && showStats) {
            "↓ ${Formatters.bytes(stats.rxBytes)}   ↑ ${Formatters.bytes(stats.txBytes)}"
        } else {
            status.detail ?: status.error ?: ""
        }

        val bigText = buildString {
            if (!status.info.server.isNullOrBlank()) append("Server: ${status.info.server}\n")
            if (status.stage == ConnectionStage.CONNECTED) {
                append("↓ Downloaded: ${Formatters.bytes(stats.rxBytes)}\n")
                append("↑ Uploaded: ${Formatters.bytes(stats.txBytes)}")
            } else if (!subtitle.isBlank()) {
                append(subtitle)
            }
        }.trim()

        return NotificationCompat.Builder(context, STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(contentIntent(context))
            .setOngoing(status.stage != ConnectionStage.ERROR)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                0,
                context.getString(R.string.action_disconnect),
                disconnectIntent(context),
            )
            .build()
    }

    /** Heads-up notification pulling the user back into the app for a prompt. */
    fun postActionRequired(context: Context, text: String) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_action_required))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
        manager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    fun clearActionRequired(context: Context) {
        context.getSystemService<NotificationManager>()?.cancel(ALERT_NOTIFICATION_ID)
    }
}
