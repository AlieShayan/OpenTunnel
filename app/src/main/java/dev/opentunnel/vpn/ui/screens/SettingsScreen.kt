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
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.BuildConfig
import dev.opentunnel.vpn.core.NativeLibrary
import dev.opentunnel.vpn.data.AppSettings
import dev.opentunnel.vpn.ui.components.SectionCard
import dev.opentunnel.vpn.ui.components.SettingRow
import dev.opentunnel.vpn.ui.components.SwitchRow
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.ui.theme.ThemeMode
import dev.opentunnel.vpn.util.Strings

import androidx.compose.material.icons.rounded.Vibration
import dev.opentunnel.vpn.util.RememberScrollHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onThemeMode: (ThemeMode) -> Unit,
    onAppLanguage: (AppLanguage) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onBypassLocalNetworks: (Boolean) -> Unit,
    onConnectOnBoot: (Boolean) -> Unit,
    onReconnectOnNetworkChange: (Boolean) -> Unit,
    onShowStatsInNotification: (Boolean) -> Unit,
    onVerboseLogging: (Boolean) -> Unit,
    onHapticFeedbackEnabled: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val lang = settings.appLanguage
    val scrollState = rememberScrollState()

    RememberScrollHaptic(scrollState, settings.hapticFeedbackEnabled)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Strings.settingsTitle(lang)) },
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
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SectionCard(title = Strings.appearance(lang)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(Strings.theme(lang), style = MaterialTheme.typography.bodyLarge)
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
                                modifier = Modifier.weight(1f),
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> Strings.themeSystem(lang)
                                            ThemeMode.DARK -> Strings.themeDark(lang)
                                            ThemeMode.LIGHT -> Strings.themeLight(lang)
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(Modifier.fillMaxWidth()) {
                        Text(Strings.appLanguageLabel(lang), style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        AppLanguage.entries.forEachIndexed { index, l ->
                            SegmentedButton(
                                selected = settings.appLanguage == l,
                                onClick = { onAppLanguage(l) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = AppLanguage.entries.size,
                                ),
                                modifier = Modifier.weight(1f),
                                label = {
                                    Text(
                                        text = when (l) {
                                            AppLanguage.SYSTEM -> Strings.langSystem(lang)
                                            AppLanguage.ENGLISH -> "English"
                                            AppLanguage.PERSIAN -> "فارسی"
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(
                        icon = Icons.Rounded.Palette,
                        title = Strings.dynamicColorTitle(lang),
                        subtitle = Strings.dynamicColorSub(lang),
                        checked = settings.dynamicColor,
                        hapticEnabled = settings.hapticFeedbackEnabled,
                        onCheckedChange = onDynamicColor,
                    )
                }
            }

            SectionCard(title = Strings.tunnelBehaviour(lang)) {
                SwitchRow(
                    icon = Icons.Rounded.Router,
                    title = Strings.bypassLocal(lang),
                    subtitle = Strings.bypassLocalSub(lang),
                    checked = settings.bypassLocalNetworks,
                    hapticEnabled = settings.hapticFeedbackEnabled,
                    onCheckedChange = onBypassLocalNetworks,
                )
                SwitchRow(
                    icon = Icons.Rounded.Sync,
                    title = Strings.reconnectNetwork(lang),
                    subtitle = Strings.reconnectNetworkSub(lang),
                    checked = settings.reconnectOnNetworkChange,
                    hapticEnabled = settings.hapticFeedbackEnabled,
                    onCheckedChange = onReconnectOnNetworkChange,
                )
                SwitchRow(
                    icon = Icons.Rounded.PowerSettingsNew,
                    title = Strings.connectOnBoot(lang),
                    subtitle = Strings.connectOnBootSub(lang),
                    checked = settings.connectOnBoot,
                    hapticEnabled = settings.hapticFeedbackEnabled,
                    onCheckedChange = onConnectOnBoot,
                )
            }

            SectionCard(title = Strings.systemSection(lang)) {
                SettingRow(
                    icon = Icons.Rounded.Lock,
                    title = Strings.alwaysOnVpn(lang),
                    subtitle = Strings.alwaysOnVpnSub(lang),
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
                    title = Strings.statsNotification(lang),
                    subtitle = Strings.statsNotificationSub(lang),
                    checked = settings.showStatsInNotification,
                    hapticEnabled = settings.hapticFeedbackEnabled,
                    onCheckedChange = onShowStatsInNotification,
                )
                SwitchRow(
                    icon = Icons.Rounded.Vibration,
                    title = Strings.hapticFeedbackTitle(lang),
                    subtitle = Strings.hapticFeedbackSub(lang),
                    checked = settings.hapticFeedbackEnabled,
                    hapticEnabled = settings.hapticFeedbackEnabled,
                    onCheckedChange = onHapticFeedbackEnabled,
                )
            }

            SectionCard(title = Strings.diagnostics(lang)) {
                SwitchRow(
                    icon = Icons.Rounded.BugReport,
                    title = Strings.verboseLogging(lang),
                    subtitle = Strings.verboseLoggingSub(lang),
                    checked = settings.verboseLogging,
                    hapticEnabled = settings.hapticFeedbackEnabled,
                    onCheckedChange = onVerboseLogging,
                )
                SettingRow(
                    icon = Icons.Rounded.Info,
                    title = Strings.about(lang),
                    subtitle = buildString {
                        append("OpenTunnel ${BuildConfig.VERSION_NAME}")
                        append(" · openconnect ")
                        append(NativeLibrary.version() ?: "${BuildConfig.OPENCONNECT_VERSION} (not loaded)")
                        append("\nGithub@AlieShayan")
                    },
                    onClick = {
                        runCatching {
                            uriHandler.openUri("https://github.com/AlieShayan/OpenTunnel")
                        }
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

            Text(
                text = "Github@AlieShayan",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        runCatching {
                            uriHandler.openUri("https://github.com/AlieShayan/OpenTunnel")
                        }
                    }
                    .padding(vertical = 4.dp),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
