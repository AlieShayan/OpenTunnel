package dev.opentunnel.vpn.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import dev.opentunnel.vpn.core.VpnBus
import dev.opentunnel.vpn.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Brings the tunnel back up after a reboot, when the user asked for that. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val repository = Repository.get(appContext)
                val settings = repository.currentSettings()
                val profile = repository.currentProfile()

                if (!settings.connectOnBoot || !profile.isComplete) return@launch

                // prepare() returns null only when the user has already granted
                // VPN consent — we cannot show the consent dialog from here.
                if (VpnService.prepare(appContext) != null) {
                    VpnBus.info("Skipping connect-on-boot: VPN permission has not been granted yet")
                    return@launch
                }

                OpenTunnelVpnService.connect(appContext)
            } catch (t: Throwable) {
                VpnBus.error("Connect on boot failed: ${t.message}")
            } finally {
                pending.finish()
            }
        }
    }
}
