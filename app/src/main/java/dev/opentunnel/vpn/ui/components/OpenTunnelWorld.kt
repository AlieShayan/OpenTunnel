package dev.opentunnel.vpn.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import dev.opentunnel.vpn.core.ConnectionStage
import dev.opentunnel.vpn.data.AppLanguage
import dev.opentunnel.vpn.util.Strings

/**
 * CompositionLocal providing an anchor callback for ConnectOrb to report its actual
 * rendered layout coordinates in root world space.
 */
val LocalOrbPositionCallback = staticCompositionLocalOf<(Offset) -> Unit> { {} }

/**
 * OpenTunnelWorld is the unified continuous environment host for the 4 primary app viewports.
 *
 * Architecture:
 * 1. WorldBackground: Persistent atmospheric multi-layer foundation with subtle depth parallax.
 * 2. WorldLighting: Physical world-space luminary anchored to ConnectOrb's measured layout position.
 * 3. Viewport Content: HorizontalPager acting as the moving camera over the stationary world.
 * 4. FloatingIslandNavigation: Frosted, state-aware navigation bar inside the world environment.
 *
 * Optimized to achieve 120 FPS by evaluating camera transformations inside the Draw phase,
 * eliminating all recompositions during horizontal drag gestures.
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

    var orbCenterInWorld by remember { mutableStateOf(Offset.Unspecified) }
    val onOrbPositioned: (Offset) -> Unit = remember {
        { newOffset ->
            if (orbCenterInWorld != newOffset) {
                orbCenterInWorld = newOffset
            }
        }
    }

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
            orbCenterProvider = { orbCenterInWorld },
        )

        // Layer 3: Viewport Content with coordinate communication channel
        CompositionLocalProvider(LocalOrbPositionCallback provides onOrbPositioned) {
            content()
        }

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

