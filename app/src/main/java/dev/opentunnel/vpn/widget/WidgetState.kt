package dev.opentunnel.vpn.widget

sealed class WidgetState {
    data object Disconnected : WidgetState()
    data object Connecting : WidgetState()
    data class Connected(
        val serverHost: String,
        val elapsed: String,
        /** Unicode flag emoji, e.g. "\uD83C\uDDF3\uD83C\uDDF1" for NL. Empty if unknown. */
        val locationFlag: String = "",
        /** Human-readable location name, e.g. "Netherlands, Amsterdam". Empty if unknown. */
        val locationName: String = "",
    ) : WidgetState()
}
