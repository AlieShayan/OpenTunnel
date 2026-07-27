package dev.opentunnel.vpn.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.opentunnel.vpn.core.VpnBus
import dev.opentunnel.vpn.service.OpenTunnelVpnService
import dev.opentunnel.vpn.ui.theme.OpenTunnelTheme

class MainActivity : ComponentActivity() {

    /** Result of the system VPN consent dialog. */
    private val vpnConsent = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            OpenTunnelVpnService.connect(this)
        } else {
            VpnBus.info("VPN permission was declined")
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The tunnel works either way; the notification is just nicer. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestNotificationPermissionIfNeeded()

        setContent {
            val viewModel: AppViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            OpenTunnelTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                OpenTunnelApp(
                    viewModel = viewModel,
                    onRequestConnect = ::requestConnect,
                    onRequestDisconnect = { OpenTunnelVpnService.disconnect(this) },
                )
            }
        }
    }

    /**
     * VpnService.prepare() returns an Intent the first time; once the user has
     * consented it returns null and the service can just be started.
     */
    private fun requestConnect() {
        val consentIntent: Intent? = runCatching { VpnService.prepare(this) }.getOrNull()
        if (consentIntent != null) {
            vpnConsent.launch(consentIntent)
        } else {
            OpenTunnelVpnService.connect(this)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
