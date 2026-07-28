package dev.opentunnel.vpn.ui

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.ui.components.PromptHost
import dev.opentunnel.vpn.ui.screens.HomeScreen
import dev.opentunnel.vpn.ui.screens.LogScreen
import dev.opentunnel.vpn.ui.screens.ProfileScreen
import dev.opentunnel.vpn.ui.screens.SettingsScreen
import dev.opentunnel.vpn.ui.screens.SplitTunnelScreen
import dev.opentunnel.vpn.util.HapticHelper

private object Routes {
    const val HOME = "home"
    const val PROFILES = "profiles"
    const val PROFILE = "profile"
    const val SPLIT = "split"
    const val SPLIT_NETWORKS = "split_networks"
    const val LOGS = "logs"
    const val SETTINGS = "settings"
}

private const val SLIDE_MS = 280

@Composable
fun OpenTunnelApp(
    viewModel: AppViewModel,
    onRequestConnect: () -> Unit,
    onRequestDisconnect: () -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val status by viewModel.status.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val prompt by viewModel.pendingPrompt.collectAsStateWithLifecycle()
    val editingProfileId by viewModel.editingProfileId.collectAsStateWithLifecycle()
    val rxHistory by viewModel.rxHistory.collectAsStateWithLifecycle()
    val txHistory by viewModel.txHistory.collectAsStateWithLifecycle()

    var previousStage by remember { mutableStateOf<ConnectionStage?>(null) }

    LaunchedEffect(status.stage) {
        val prev = previousStage
        val current = status.stage
        previousStage = current

        if (prev != null && prev != current) {
            if (current == ConnectionStage.CONNECTED) {
                HapticHelper.performConnect(context, settings.hapticFeedbackEnabled)
            } else if ((prev == ConnectionStage.CONNECTED || prev.isActive) &&
                (current == ConnectionStage.IDLE || current == ConnectionStage.ERROR)) {
                HapticHelper.performDisconnect(context, settings.hapticFeedbackEnabled)
            }
        }
    }

    val layoutDirection = if (settings.appLanguage == dev.opentunnel.vpn.data.AppLanguage.PERSIAN) {
        androidx.compose.ui.unit.LayoutDirection.Rtl
    } else {
        androidx.compose.ui.unit.LayoutDirection.Ltr
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.safeDrawingPadding(),
                enterTransition = {
                    slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it / 5 } +
                        fadeIn(tween(SLIDE_MS, easing = FastOutSlowInEasing))
                },
                exitTransition = {
                    slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { -it / 5 } +
                        fadeOut(tween(SLIDE_MS / 2))
                },
                popEnterTransition = {
                    slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { -it / 5 } +
                        fadeIn(tween(SLIDE_MS, easing = FastOutSlowInEasing))
                },
                popExitTransition = {
                    slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it / 5 } +
                        fadeOut(tween(SLIDE_MS))
                },
            ) {
                composable(Routes.HOME) {
                    dev.opentunnel.vpn.ui.screens.MainPagerScreen(
                        status = status,
                        stats = stats,
                        logs = logs,
                        profile = profile,
                        profiles = profiles,
                        settings = settings,
                        rxHistoryList = rxHistory,
                        txHistoryList = txHistory,
                        onToggleConnection = {
                            if (status.stage.isActive) onRequestDisconnect() else onRequestConnect()
                        },
                        onSelectProfile = viewModel::selectProfile,
                        onOpenProfile = {
                            viewModel.setEditingProfileId(profile.id)
                            navController.navigate(Routes.PROFILE)
                        },
                        onOpenProfileManagement = {
                            navController.navigate(Routes.PROFILES)
                        },
                        onOpenSplitTunnel = { navController.navigate(Routes.SPLIT) },
                        onOpenSplitNetworks = { navController.navigate(Routes.SPLIT_NETWORKS) },
                        onClearLogs = viewModel::clearLogs,
                        onThemeMode = viewModel::setThemeMode,
                        onAppLanguage = viewModel::setAppLanguage,
                        onDynamicColor = viewModel::setDynamicColor,
                        onBypassLocalNetworks = viewModel::setBypassLocalNetworks,
                        onConnectOnBoot = viewModel::setConnectOnBoot,
                        onReconnectOnNetworkChange = viewModel::setReconnectOnNetworkChange,
                        onShowStatsInNotification = viewModel::setShowStatsInNotification,
                        onVerboseLogging = viewModel::setVerboseLogging,
                        onHapticFeedbackEnabled = viewModel::setHapticFeedbackEnabled,
                    )
                }

                composable(Routes.PROFILES) {
                    dev.opentunnel.vpn.ui.screens.ProfileManagementScreen(
                        activeProfileId = settings.activeProfileId,
                        profiles = profiles,
                        appLanguage = settings.appLanguage,
                        hapticFeedbackEnabled = settings.hapticFeedbackEnabled,
                        onSelectProfile = viewModel::selectProfile,
                        onEditProfile = { id ->
                            viewModel.setEditingProfileId(id)
                            navController.navigate(Routes.PROFILE)
                        },
                        onAddProfile = {
                            viewModel.setEditingProfileId("new")
                            navController.navigate(Routes.PROFILE)
                        },
                        onDeleteProfile = viewModel::deleteProfile,
                        onExportProfiles = viewModel::exportProfiles,
                        onImportProfiles = viewModel::importProfiles,
                        onReorderProfiles = viewModel::reorderProfiles,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.PROFILE) {
                    val targetProfile = viewModel.getProfile(editingProfileId)
                    ProfileScreen(
                        profile = targetProfile,
                        appLanguage = settings.appLanguage,
                        hapticFeedbackEnabled = settings.hapticFeedbackEnabled,
                        onSave = viewModel::saveProfile,
                        onDelete = { id ->
                            viewModel.deleteProfile(id)
                            navController.popBackStack()
                        },
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

                composable(Routes.SPLIT_NETWORKS) {
                    dev.opentunnel.vpn.ui.screens.SplitTunnelNetworksScreen(
                        settings = settings,
                        onToggleEnabled = viewModel::setSplitTunnelNetworksEnabled,
                        onChangeMode = viewModel::setSplitTunnelNetworksMode,
                        onAddNetwork = viewModel::addSplitTunnelNetwork,
                        onRemoveNetwork = viewModel::removeSplitTunnelNetwork,
                        onClearAll = viewModel::clearSplitTunnelNetworks,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.LOGS) {
                    LogScreen(
                        logs = logs,
                        appLanguage = settings.appLanguage,
                        hapticFeedbackEnabled = settings.hapticFeedbackEnabled,
                        // Fixed: was an empty lambda — now actually clears the log buffer.
                        onClear = viewModel::clearLogs,
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        settings = settings,
                        onThemeMode = viewModel::setThemeMode,
                        onAppLanguage = viewModel::setAppLanguage,
                        onDynamicColor = viewModel::setDynamicColor,
                        onBypassLocalNetworks = viewModel::setBypassLocalNetworks,
                        onConnectOnBoot = viewModel::setConnectOnBoot,
                        onReconnectOnNetworkChange = viewModel::setReconnectOnNetworkChange,
                        onShowStatsInNotification = viewModel::setShowStatsInNotification,
                        onVerboseLogging = viewModel::setVerboseLogging,
                        onHapticFeedbackEnabled = viewModel::setHapticFeedbackEnabled,
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
}
