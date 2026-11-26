package io.github.slopcommunistcooperative.meowmos.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

object MeowmosSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

/**
 * Semantic design tokens derived from the active Material 3 [ColorScheme] so both the static
 * Terminal Yellow Wayfinding palette and Android 12+ Material You dynamic color stay coherent.
 */
@Immutable
data class MeowmosExtendedColors(
    /** High-contrast signal badge (Schiphol yellow/ink in static mode; primary accent in dynamic). */
    val wayfindingContainer: Color,
    val onWayfindingContainer: Color,
    /** Inverted architectural badge (charcoal/white in light, ivory/ink in dark). */
    val beaconContainer: Color,
    val onBeaconContainer: Color,
    /** Card surface resting above the tinted page ground. */
    val cardSurface: Color,
    /** Pinned card container tint and outline. */
    val pinnedBorder: Color,
    /** Subtle panel surface for nested groups (stats, comment threads, sections). */
    val panelSurface: Color,
    /** Five-level activity heatmap ramp that works in light, dark, and dynamic themes. */
    val heatmapEmpty: Color,
    val heatmapLevel1: Color,
    val heatmapLevel2: Color,
    val heatmapLevel3: Color,
    val heatmapLevel4: Color,
)

internal fun meowmosExtendedColorsFor(
    colorScheme: ColorScheme,
    darkTheme: Boolean,
    isDynamic: Boolean,
): MeowmosExtendedColors {
    val wayfindingContainer = when {
        !isDynamic -> TerminalSignalYellow
        darkTheme -> colorScheme.primary
        else -> colorScheme.primaryContainer
    }
    val onWayfindingContainer = when {
        !isDynamic -> TerminalSignalInk
        darkTheme -> colorScheme.onPrimary
        else -> colorScheme.onPrimaryContainer
    }
    val cardSurface = if (darkTheme) {
        colorScheme.surfaceContainerLow
    } else {
        colorScheme.surfaceContainerLowest
    }
    val panelSurface = if (darkTheme) {
        colorScheme.surfaceContainer
    } else {
        colorScheme.surfaceContainerLow
    }
    val pinnedBorder = if (!isDynamic) {
        if (darkTheme) TerminalSignalYellow.copy(alpha = 0.85f) else Color(0xFFC4A200)
    } else {
        colorScheme.primary
    }

    val heatmapEmpty = if (darkTheme) {
        colorScheme.surfaceContainerHigh
    } else {
        colorScheme.surfaceContainer
    }
    val heatmapPeak = if (!isDynamic) {
        if (darkTheme) TerminalSignalYellow else Color(0xFF1A1C16)
    } else {
        colorScheme.primary
    }
    val heatmapMid = if (!isDynamic) {
        if (darkTheme) Color(0xFFA88C00) else TerminalSignalYellow
    } else {
        colorScheme.primaryContainer
    }

    return MeowmosExtendedColors(
        wayfindingContainer = wayfindingContainer,
        onWayfindingContainer = onWayfindingContainer,
        beaconContainer = colorScheme.inverseSurface,
        onBeaconContainer = colorScheme.inverseOnSurface,
        cardSurface = cardSurface,
        pinnedBorder = pinnedBorder,
        panelSurface = panelSurface,
        heatmapEmpty = heatmapEmpty,
        heatmapLevel1 = lerp(heatmapEmpty, heatmapMid, 0.35f),
        heatmapLevel2 = lerp(heatmapEmpty, heatmapMid, 0.75f),
        heatmapLevel3 = lerp(heatmapMid, heatmapPeak, 0.55f),
        heatmapLevel4 = heatmapPeak,
    )
}

val LocalMeowmosColors = staticCompositionLocalOf {
    meowmosExtendedColorsFor(
        colorScheme = LightColorScheme,
        darkTheme = false,
        isDynamic = false,
    )
}
