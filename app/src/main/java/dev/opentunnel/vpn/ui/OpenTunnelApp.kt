package dev.opentunnel.vpn.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.opentunnel.vpn.ui.components.PromptHost
import dev.opentunnel.vpn.ui.screens.HomeScreen
import dev.opentunnel.vpn.ui.screens.LogScreen
import dev.opentunnel.vpn.ui.screens.ProfileScreen
import dev.opentunnel.vpn.ui.screens.SettingsScreen
import dev.opentunnel.vpn.ui.screens.SplitTunnelScreen

private object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val SPLIT = "split"
    const val LOGS = "logs"
    const val SETTINGS = "settings"
}

private const val SLIDE_MS = 260

@Composable
fun OpenTunnelApp(
    viewModel: AppViewModel,
    onRequestConnect: () -> Unit,
    onRequestDisconnect: () -> Unit,
) {
    val navController = rememberNavController()

    val status by viewModel.status.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val prompt by viewModel.pendingPrompt.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.safeDrawingPadding(),
            enterTransition = {
                slideInHorizontally(tween(SLIDE_MS)) { it / 6 } + fadeIn(tween(SLIDE_MS))
            },
            exitTransition = { fadeOut(tween(SLIDE_MS / 2)) },
            popEnterTransition = { fadeIn(tween(SLIDE_MS)) },
            popExitTransition = {
                slideOutHorizontally(tween(SLIDE_MS)) { it / 6 } + fadeOut(tween(SLIDE_MS))
            },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    status = status,
                    stats = stats,
                    profile = profile,
                    profiles = profiles,
                    settings = settings,
                    onToggleConnection = {
                        if (status.stage.isActive) onRequestDisconnect() else onRequestConnect()
                    },
                    onSelectProfile = viewModel::selectProfile,
                    onOpenProfile = { navController.navigate(Routes.PROFILE) },
                    onOpenSplitTunnel = { navController.navigate(Routes.SPLIT) },
                    onOpenLogs = { navController.navigate(Routes.LOGS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    profile = profile,
                    onSave = viewModel::saveProfile,
                    onForgetCertificate = viewModel::forgetPinnedCertificate,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SPLIT) {
                SplitTunnelScreen(
                    settings = settings,
                    apps = installedApps,
                    onLoadApps = viewModel::loadInstalledApps,
                    onToggleEnabled = viewModel::setSplitTunnelEnabled,
                    onChangeMode = viewModel::setSplitTunnelMode,
                    onTogglePackage = viewModel::togglePackage,
                    onClearAll = viewModel::clearSelectedPackages,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.LOGS) {
                LogScreen(
                    logs = logs,
                    onClear = viewModel::clearLogs,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    onThemeMode = viewModel::setThemeMode,
                    onDynamicColor = viewModel::setDynamicColor,
                    onBypassLocalNetworks = viewModel::setBypassLocalNetworks,
                    onConnectOnBoot = viewModel::setConnectOnBoot,
                    onReconnectOnNetworkChange = viewModel::setReconnectOnNetworkChange,
                    onShowStatsInNotification = viewModel::setShowStatsInNotification,
                    onVerboseLogging = viewModel::setVerboseLogging,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        PromptHost(
            prompt = prompt,
            onSubmit = viewModel::submitPrompt,
            onAccept = viewModel::acceptPrompt,
            onCancel = viewModel::cancelPrompt,
        )
    }
}
