package dev.opentunnel.vpn.widget

sealed class WidgetState {
    data object Disconnected : WidgetState()
    data object Connecting : WidgetState()
    data class Connected(
        val profileName: String,
        val serverHost: String,
        val elapsed: String,
        val locationFlag: String = "",
        val locationName: String = "",
        val outboundIp: String = "",
        val downloadedFormatted: String = "",
        val uploadedFormatted: String = "",
        val pingMsFormatted: String = "",
    ) : WidgetState()
}
