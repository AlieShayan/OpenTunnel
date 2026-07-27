package dev.opentunnel.vpn.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.BuildConfig
import dev.opentunnel.vpn.core.NativeLibrary
import dev.opentunnel.vpn.data.AppSettings
import dev.opentunnel.vpn.ui.components.SectionCard
import dev.opentunnel.vpn.ui.components.SettingRow
import dev.opentunnel.vpn.ui.components.SwitchRow
import dev.opentunnel.vpn.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onBypassLocalNetworks: (Boolean) -> Unit,
    onConnectOnBoot: (Boolean) -> Unit,
    onReconnectOnNetworkChange: (Boolean) -> Unit,
    onShowStatsInNotification: (Boolean) -> Unit,
    onVerboseLogging: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SectionCard(title = "Appearance") {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("Theme", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = settings.themeMode == mode,
                                onClick = { onThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ThemeMode.entries.size,
                                ),
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "System"
                                            ThemeMode.DARK -> "Dark"
                                            ThemeMode.LIGHT -> "Light"
                                        }
                                    )
                                },
                            )
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(
                        icon = Icons.Rounded.Palette,
                        title = "Wallpaper colours",
                        subtitle = "Tint the app with your Material You palette",
                        checked = settings.dynamicColor,
                        onCheckedChange = onDynamicColor,
                    )
                }
            }

            SectionCard(title = "Tunnel behaviour") {
                SwitchRow(
                    icon = Icons.Rounded.Router,
                    title = "Keep local network off the VPN",
                    subtitle = "Printers, NAS and casting keep working while connected",
                    checked = settings.bypassLocalNetworks,
                    onCheckedChange = onBypassLocalNetworks,
                )
                SwitchRow(
                    icon = Icons.Rounded.Sync,
                    title = "Reconnect on network change",
                    subtitle = "Re-establish the tunnel when moving between Wi-Fi and mobile data",
                    checked = settings.reconnectOnNetworkChange,
                    onCheckedChange = onReconnectOnNetworkChange,
                )
                SwitchRow(
                    icon = Icons.Rounded.PowerSettingsNew,
                    title = "Connect after restart",
                    subtitle = "Needs VPN permission to have been granted at least once",
                    checked = settings.connectOnBoot,
                    onCheckedChange = onConnectOnBoot,
                )
            }

            SectionCard(title = "System") {
                SettingRow(
                    icon = Icons.Rounded.Lock,
                    title = "Always-on VPN",
                    subtitle = "Open Android's VPN settings to make this the always-on VPN and block traffic when it drops",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_VPN_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                )
                SwitchRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Traffic counters in the notification",
                    subtitle = "Show total up/down in the ongoing notification",
                    checked = settings.showStatsInNotification,
                    onCheckedChange = onShowStatsInNotification,
                )
            }

            SectionCard(title = "Diagnostics") {
                SwitchRow(
                    icon = Icons.Rounded.BugReport,
                    title = "Verbose logging",
                    subtitle = "Include openconnect debug output in the connection log",
                    checked = settings.verboseLogging,
                    onCheckedChange = onVerboseLogging,
                )
                SettingRow(
                    icon = Icons.Rounded.Info,
                    title = "About",
                    subtitle = buildString {
                        append("OpenTunnel ${BuildConfig.VERSION_NAME}")
                        append(" · openconnect ")
                        append(NativeLibrary.version() ?: "${BuildConfig.OPENCONNECT_VERSION} (not loaded)")
                    },
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Tunnel engine: openconnect (LGPL 2.1) with OpenSSL. " +
                    "This app is not affiliated with Cisco.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
