package com.aetheraudio.pro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class AetherExtraColors(
    val glassPanel: Color,
    val glassBorder: Color,
    val waveformInactive: Color,
    val trueBlack: Boolean
)

private val LocalAetherColors = androidx.compose.runtime.staticCompositionLocalOf {
    AetherExtraColors(GlassPanelDark, GlassBorderDark, WaveformInactive, false)
}

val MaterialTheme.aether: AetherExtraColors
    @Composable get() = LocalAetherColors.current

private fun darkScheme(trueBlack: Boolean, seed: Color?) = darkColorScheme(
    primary = seed ?: AccentViolet,
    secondary = AccentCyan,
    tertiary = AccentAmber,
    background = if (trueBlack) ObsidianBgTrueBlack else ObsidianBg,
    surface = if (trueBlack) ObsidianBgTrueBlack else ObsidianBg,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = GlassPanelDark,
    onSurfaceVariant = TextSecondaryDark
)

private fun lightScheme(seed: Color?) = lightColorScheme(
    primary = seed ?: AccentViolet,
    secondary = AccentCyan,
    tertiary = AccentAmber,
    background = CrystalLightBg,
    surface = CrystalLightBg,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = GlassPanelLight,
    onSurfaceVariant = TextSecondaryLight
)

/**
 * @param dynamicSeed a color extracted from the current album art (via androidx.palette),
 *   used to lightly tint accents — the "Material You" style extraction called for in spec section 2.2.
 */
@Composable
fun AetherAudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledTrueBlack: Boolean = false,
    dynamicSeed: Color? = null,
    content: @Composable () -> Unit
) {
    val scheme: ColorScheme = if (darkTheme) darkScheme(amoledTrueBlack, dynamicSeed) else lightScheme(dynamicSeed)
    val extra = if (darkTheme) {
        AetherExtraColors(GlassPanelDark, GlassBorderDark, WaveformInactive, amoledTrueBlack)
    } else {
        AetherExtraColors(GlassPanelLight, GlassBorderLight, WaveformInactive, false)
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalAetherColors provides extra) {
        MaterialTheme(colorScheme = scheme, typography = AetherTypography, content = content)
    }
}
