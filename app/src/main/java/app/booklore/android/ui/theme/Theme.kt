package app.booklore.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import app.booklore.android.core.model.DisplaySettings
import app.booklore.android.core.model.FontOption
import app.booklore.android.core.model.ThemeColorOption

fun themeColorPreview(themeColor: ThemeColorOption): Color = when (themeColor) {
    ThemeColorOption.FOREST -> Color(0xFF285747)
    ThemeColorOption.OCEAN -> Color(0xFF006782)
    ThemeColorOption.CORAL -> Color(0xFF9C3C50)
}

/** Builds the app color scheme; dark palettes are used when the system is in dark mode. */
fun bookLoreColorScheme(themeColor: ThemeColorOption, dark: Boolean): ColorScheme {
    val palette = if (dark) darkPaletteFor(themeColor) else lightPaletteFor(themeColor)
    return if (dark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.onSurfaceVariant,
            background = palette.surface,
            onBackground = palette.onSurface,
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.onPrimaryContainer,
            secondary = palette.secondary,
            tertiary = palette.tertiary,
            surface = palette.surface,
            onSurface = palette.onSurface,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.onSurfaceVariant,
            background = palette.surface,
            onBackground = palette.onSurface,
        )
    }
}

@Composable
fun BookLoreTheme(
    displaySettings: DisplaySettings = DisplaySettings(),
    content: @Composable () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * displaySettings.fontScale.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE),
        ),
    ) {
        MaterialTheme(
            colorScheme = bookLoreColorScheme(displaySettings.themeColor, dark),
            typography = Typography().withFontFamily(displaySettings.font.toFontFamily()),
            content = content,
        )
    }
}

private fun lightPaletteFor(themeColor: ThemeColorOption): ThemePalette = when (themeColor) {
    ThemeColorOption.FOREST -> ThemePalette(
        primary = Color(0xFF285747),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFB6F2D6),
        onPrimaryContainer = Color(0xFF002116),
        secondary = Color(0xFF4B6358),
        tertiary = Color(0xFF615C40),
        surface = Color.White,
        onSurface = Color(0xFF191C1A),
        surfaceVariant = Color(0xFFDCE5DD),
        onSurfaceVariant = Color(0xFF404943),
    )
    ThemeColorOption.OCEAN -> ThemePalette(
        primary = Color(0xFF006782),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFBCEBFF),
        onPrimaryContainer = Color(0xFF001F2A),
        secondary = Color(0xFF49626C),
        tertiary = Color(0xFF575E7E),
        surface = Color.White,
        onSurface = Color(0xFF191C1E),
        surfaceVariant = Color(0xFFD5E6EC),
        onSurfaceVariant = Color(0xFF41484D),
    )
    ThemeColorOption.CORAL -> ThemePalette(
        primary = Color(0xFF9C3C50),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD9E0),
        onPrimaryContainer = Color(0xFF3E0016),
        secondary = Color(0xFF75565D),
        tertiary = Color(0xFF765A2D),
        surface = Color.White,
        onSurface = Color(0xFF1B1B1C),
        surfaceVariant = Color(0xFFF0DEE1),
        onSurfaceVariant = Color(0xFF514347),
    )
}

private fun darkPaletteFor(themeColor: ThemeColorOption): ThemePalette = when (themeColor) {
    ThemeColorOption.FOREST -> ThemePalette(
        primary = Color(0xFF94D9BC),
        onPrimary = Color(0xFF002115),
        primaryContainer = Color(0xFF00382A),
        onPrimaryContainer = Color(0xFFAFF5D8),
        secondary = Color(0xFFB1CCC0),
        tertiary = Color(0xFFC8C9A4),
        surface = Color(0xFF181C1A),
        onSurface = Color(0xFFE1E3DF),
        surfaceVariant = Color(0xFF404943),
        onSurfaceVariant = Color(0xFFC0CCC5),
    )
    ThemeColorOption.OCEAN -> ThemePalette(
        primary = Color(0xFF7BD0F0),
        onPrimary = Color(0xFF003443),
        primaryContainer = Color(0xFF004C61),
        onPrimaryContainer = Color(0xFFB3E8FF),
        secondary = Color(0xFFB0CAD6),
        tertiary = Color(0xFFC0C3E8),
        surface = Color(0xFF171A1C),
        onSurface = Color(0xFFE1E2E4),
        surfaceVariant = Color(0xFF3F484D),
        onSurfaceVariant = Color(0xFFBFC9CE),
    )
    ThemeColorOption.CORAL -> ThemePalette(
        primary = Color(0xFFFFB1BB),
        onPrimary = Color(0xFF4E0020),
        primaryContainer = Color(0xFF6D2437),
        onPrimaryContainer = Color(0xFFFFD9DE),
        secondary = Color(0xFFE0BDC3),
        tertiary = Color(0xFFDCC48F),
        surface = Color(0xFF1A1B1C),
        onSurface = Color(0xFFE2E2E3),
        surfaceVariant = Color(0xFF4B4446),
        onSurfaceVariant = Color(0xFFD2C3C5),
    )
}

private fun FontOption.toFontFamily(): FontFamily = when (this) {
    FontOption.SANS -> FontFamily.SansSerif
    FontOption.SERIF -> FontFamily.Serif
    FontOption.MONOSPACE -> FontFamily.Monospace
}

private fun Typography.withFontFamily(fontFamily: FontFamily): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
)

private data class ThemePalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val tertiary: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
)

private const val MIN_FONT_SCALE = 0.85f
private const val MAX_FONT_SCALE = 1.30f
