package dev.opentunnel.vpn

import android.app.Application
import dev.opentunnel.vpn.core.NativeLibrary
import dev.opentunnel.vpn.core.VpnBus
import dev.opentunnel.vpn.service.Notifications

class OpenTunnelApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)

        // Touch the loader early so the UI can surface a clear message instead
        // of failing at the moment the user taps Connect.
        if (!NativeLibrary.isAvailable) {
            VpnBus.error(NativeLibrary.loadError ?: NativeLibrary.MISSING_MESSAGE)
        }
    }
}
