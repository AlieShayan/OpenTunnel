package dev.opentunnel.vpn.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.util.Strings

/**
 * OpenTunnelWorld is the unified environment host for the 4 primary app viewports.
 *
 * It provides:
 * 1. WorldBackground: The persistent atmospheric foundation.
 * 2. WorldLighting: Physical world-space lighting synchronized with connection state.
 * 3. Pager Viewport Content: The 4 spaces rendered over the living atmosphere.
 * 4. FloatingIslandNavigation: The state-aware floating navigation bar.
 *
 * Optimized to achieve 120 FPS by providing pagePosition via a lambda provider,
 * eliminating full-tree recomposition passes during horizontal drag gestures.
 */
@Composable
fun OpenTunnelWorld(
    pagerState: PagerState,
    stage: ConnectionStage,
    lang: AppLanguage,
    onNavigateToPage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isRtl = Strings.isRtl(lang)
    val pagePositionProvider = { pagerState.currentPage + pagerState.currentPageOffsetFraction }

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: World Background Foundation
        WorldBackground(
            pagePositionProvider = pagePositionProvider,
            isRtl = isRtl,
        )

        // Layer 2: World Lighting (Luminary in World Space - zero recomposition on drag)
        WorldLighting(
            stage = stage,
            pagePositionProvider = pagePositionProvider,
            isRtl = isRtl,
        )

        // Layer 3: Viewport Content (Horizontal Pager)
        content()

        // Layer 4: State-Aware Floating Navigation
        FloatingIslandNavigation(
            currentPage = pagerState.currentPage,
            pagePositionProvider = pagePositionProvider,
            stage = stage,
            lang = lang,
            onNavigateToPage = onNavigateToPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )
    }
}
