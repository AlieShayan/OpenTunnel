package dev.opentunnel.vpn.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * "Aurora" — a deep-midnight base with a mint/aqua accent. Dark is the primary
 * design target; the light scheme is a faithful translation rather than an
 * afterthought.
 */

// ── Dark ────────────────────────────────────────────────────────────────────
val AuroraMint = Color(0xFF5EE7C4)
val AuroraMintDim = Color(0xFF2FBFA0)
val AuroraSky = Color(0xFF8FB8FF)
val AuroraViolet = Color(0xFFC7A6FF)
val AuroraRose = Color(0xFFFF6B7A)
val AuroraAmber = Color(0xFFFFC66B)

val MidnightBase = Color(0xFF070A12)
val MidnightSurface = Color(0xFF0C111C)
val MidnightContainer = Color(0xFF121927)
val MidnightContainerHigh = Color(0xFF192334)
val MidnightOutline = Color(0xFF2A3550)

val DarkColors = darkColorScheme(
    primary = AuroraMint,
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF005444),
    onPrimaryContainer = Color(0xFF8CFFE0),
    inversePrimary = Color(0xFF006A57),

    secondary = AuroraSky,
    onSecondary = Color(0xFF102A4D),
    secondaryContainer = Color(0xFF1D3C68),
    onSecondaryContainer = Color(0xFFD3E3FF),

    tertiary = AuroraViolet,
    onTertiary = Color(0xFF2E1B54),
    tertiaryContainer = Color(0xFF44306C),
    onTertiaryContainer = Color(0xFFEADDFF),

    error = AuroraRose,
    onError = Color(0xFF4A0011),
    errorContainer = Color(0xFF6E1225),
    onErrorContainer = Color(0xFFFFDAD8),

    background = MidnightBase,
    onBackground = Color(0xFFE4E9F4),
    surface = MidnightSurface,
    onSurface = Color(0xFFE4E9F4),
    surfaceVariant = MidnightContainer,
    onSurfaceVariant = Color(0xFFA9B4C9),

    surfaceContainerLowest = Color(0xFF05070D),
    surfaceContainerLow = Color(0xFF0A0F19),
    surfaceContainer = MidnightContainer,
    surfaceContainerHigh = Color(0xFF192334),
    surfaceContainerHighest = Color(0xFF212D41),

    outline = MidnightOutline,
    outlineVariant = Color(0xFF1C2438),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE4E9F4),
    inverseOnSurface = Color(0xFF11151F),
)

// ── Light ───────────────────────────────────────────────────────────────────
val LightColors = lightColorScheme(
    primary = Color(0xFF006A57),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA6F2DA),
    onPrimaryContainer = Color(0xFF00201A),
    inversePrimary = AuroraMint,

    secondary = Color(0xFF1F5FB8),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E3FF),
    onSecondaryContainer = Color(0xFF001B3D),

    tertiary = Color(0xFF6A4CA8),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEBDDFF),
    onTertiaryContainer = Color(0xFF250F52),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    background = Color(0xFFF5F8FB),
    onBackground = Color(0xFF11151C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF11151C),
    surfaceVariant = Color(0xFFE6EDF3),
    onSurfaceVariant = Color(0xFF48505C),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FAFC),
    surfaceContainer = Color(0xFFF0F4F8),
    surfaceContainerHigh = Color(0xFFE9EFF4),
    surfaceContainerHighest = Color(0xFFE2E9F0),

    outline = Color(0xFFC3CDD8),
    outlineVariant = Color(0xFFDDE4EC),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF1D2229),
    inverseOnSurface = Color(0xFFF1F4F8),
)

/**
 * Semantic colours for connection state. These deliberately live outside the
 * [androidx.compose.material3.ColorScheme] so they stay stable even when the
 * user turns on dynamic (wallpaper) colour.
 */
data class StatusPalette(
    val idle: Color,
    val connecting: Color,
    val connected: Color,
    val error: Color,
    val glow: Color,
)

val DarkStatusPalette = StatusPalette(
    idle = Color(0xFF7A879E),
    connecting = AuroraAmber,
    connected = AuroraMint,
    error = AuroraRose,
    glow = Color(0xFF5EE7C4),
)

val LightStatusPalette = StatusPalette(
    idle = Color(0xFF6B7686),
    connecting = Color(0xFFB07400),
    connected = Color(0xFF00785F),
    error = Color(0xFFC0303C),
    glow = Color(0xFF2FBFA0),
)
