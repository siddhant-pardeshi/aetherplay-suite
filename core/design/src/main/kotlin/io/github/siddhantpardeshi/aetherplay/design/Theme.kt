package io.github.siddhantpardeshi.aetherplay.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode { DARK, LIGHT, OLED }

enum class AppIdentity(val label: String) { LAB("Lab"), SPACE("Space") }

data class AccentColors(
    val accent: Color,
    val accentDeep: Color,
    val onAccent: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

data class Palette(
    val bg: Color,
    val base: Color,
    val raised: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
)

val LabAccent = AccentColors(
    accent = Color(0xFF65E3BE),
    accentDeep = Color(0xFF2FA98B),
    onAccent = Color(0xFF04241B),
    gradientStart = Color(0xFF65E3BE),
    gradientEnd = Color(0xFF3EC9C4),
)

val SpaceAccent = AccentColors(
    accent = Color(0xFFB5A1FF),
    accentDeep = Color(0xFF8A76E0),
    onAccent = Color(0xFF150B33),
    gradientStart = Color(0xFFB5A1FF),
    gradientEnd = Color(0xFF7F9BFF),
)

val AuroraWarn = Color(0xFFF0BF70)
val AuroraError = Color(0xFFFF8585)
val AuroraOk = Color(0xFF65E3BE)

private val DarkPalette = Palette(
    bg = Color(0xFF0B0D12),
    base = Color(0xFF141923),
    raised = Color(0xFF1D2532),
    textPrimary = Color(0xFFF2F5FA),
    textSecondary = Color(0xFFA8B3C4),
    outline = Color(0xFF2A3444),
)

private val OledPalette = DarkPalette.copy(bg = Color(0xFF000000), base = Color(0xFF0A0C10), raised = Color(0xFF12151C))

private val LightPalette = Palette(
    bg = Color(0xFFF2F4F9),
    base = Color(0xFFFFFFFF),
    raised = Color(0xFFEAEEF6),
    textPrimary = Color(0xFF171C26),
    textSecondary = Color(0xFF5A6577),
    outline = Color(0xFFD5DCE8),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }
val LocalAccent = staticCompositionLocalOf { LabAccent }
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun AetherTheme(
    mode: ThemeMode = ThemeMode.DARK,
    identity: AppIdentity = AppIdentity.LAB,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = when (mode) {
        ThemeMode.DARK -> DarkPalette
        ThemeMode.OLED -> OledPalette
        ThemeMode.LIGHT -> LightPalette
    }
    val accent = if (identity == AppIdentity.LAB) LabAccent else SpaceAccent
    val scheme = if (mode == ThemeMode.LIGHT) {
        lightColorScheme(
            primary = accent.accentDeep,
            onPrimary = Color.White,
            background = palette.bg,
            onBackground = palette.textPrimary,
            surface = palette.base,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.raised,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.outline,
            error = AuroraError,
        )
    } else {
        darkColorScheme(
            primary = accent.accent,
            onPrimary = accent.onAccent,
            background = palette.bg,
            onBackground = palette.textPrimary,
            surface = palette.base,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.raised,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.outline,
            error = AuroraError,
        )
    }
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalAccent provides accent,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = AetherTypography, content = content)
    }
}

object AetherObjects
