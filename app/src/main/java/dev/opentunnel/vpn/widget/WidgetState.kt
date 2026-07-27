package dev.opentunnel.vpn.widget

/** Snapshot of tunnel state that the home-screen widget needs to render itself. */
sealed class WidgetState {
    object Disconnected : WidgetState()
    object Connecting : WidgetState()
    data class Connected(
        val serverHost: String,
        /** Formatted elapsed time, e.g. "1:04:22" */
        val elapsed: String,
    ) : WidgetState()
}
