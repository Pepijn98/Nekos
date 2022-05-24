package dev.vdbroek.nekos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeState {
    var isDark by mutableStateOf(true)
    var manual by mutableStateOf(false)
    var staggered by mutableStateOf(false)
}

private val LightThemeColors = lightColorScheme(
    primary = NekoColors.md_theme_light_primary,
    onPrimary = NekoColors.md_theme_light_onPrimary,
    primaryContainer = NekoColors.md_theme_light_primaryContainer,
    onPrimaryContainer = NekoColors.md_theme_light_onPrimaryContainer,
    secondary = NekoColors.md_theme_light_secondary,
    onSecondary = NekoColors.md_theme_light_onSecondary,
    secondaryContainer = NekoColors.md_theme_light_secondaryContainer,
    onSecondaryContainer = NekoColors.md_theme_light_onSecondaryContainer,
    tertiary = NekoColors.md_theme_light_tertiary,
    onTertiary = NekoColors.md_theme_light_onTertiary,
    tertiaryContainer = NekoColors.md_theme_light_tertiaryContainer,
    onTertiaryContainer = NekoColors.md_theme_light_onTertiaryContainer,
    error = NekoColors.md_theme_light_error,
    errorContainer = NekoColors.md_theme_light_errorContainer,
    onError = NekoColors.md_theme_light_onError,
    onErrorContainer = NekoColors.md_theme_light_onErrorContainer,
    background = NekoColors.md_theme_light_background,
    onBackground = NekoColors.md_theme_light_onBackground,
    surface = NekoColors.md_theme_light_surface,
    onSurface = NekoColors.md_theme_light_onSurface,
    surfaceVariant = NekoColors.md_theme_light_surfaceVariant,
    onSurfaceVariant = NekoColors.md_theme_light_onSurfaceVariant,
    outline = NekoColors.md_theme_light_outline,
    inverseOnSurface = NekoColors.md_theme_light_inverseOnSurface,
    inverseSurface = NekoColors.md_theme_light_inverseSurface,
    inversePrimary = NekoColors.md_theme_light_inversePrimary,
)
private val DarkThemeColors = darkColorScheme(
    primary = NekoColors.md_theme_dark_primary,
    onPrimary = NekoColors.md_theme_dark_onPrimary,
    primaryContainer = NekoColors.md_theme_dark_primaryContainer,
    onPrimaryContainer = NekoColors.md_theme_dark_onPrimaryContainer,
    secondary = NekoColors.md_theme_dark_secondary,
    onSecondary = NekoColors.md_theme_dark_onSecondary,
    secondaryContainer = NekoColors.md_theme_dark_secondaryContainer,
    onSecondaryContainer = NekoColors.md_theme_dark_onSecondaryContainer,
    tertiary = NekoColors.md_theme_dark_tertiary,
    onTertiary = NekoColors.md_theme_dark_onTertiary,
    tertiaryContainer = NekoColors.md_theme_dark_tertiaryContainer,
    onTertiaryContainer = NekoColors.md_theme_dark_onTertiaryContainer,
    error = NekoColors.md_theme_dark_error,
    errorContainer = NekoColors.md_theme_dark_errorContainer,
    onError = NekoColors.md_theme_dark_onError,
    onErrorContainer = NekoColors.md_theme_dark_onErrorContainer,
    background = NekoColors.md_theme_dark_background,
    onBackground = NekoColors.md_theme_dark_onBackground,
    surface = NekoColors.md_theme_dark_surface,
    onSurface = NekoColors.md_theme_dark_onSurface,
    surfaceVariant = NekoColors.md_theme_dark_surfaceVariant,
    onSurfaceVariant = NekoColors.md_theme_dark_onSurfaceVariant,
    outline = NekoColors.md_theme_dark_outline,
    inverseOnSurface = NekoColors.md_theme_dark_inverseOnSurface,
    inverseSurface = NekoColors.md_theme_dark_inverseSurface,
    inversePrimary = NekoColors.md_theme_dark_inversePrimary,
)

@Composable
fun NekosTheme(content: @Composable () -> Unit) {
    ThemeState.isDark = if (ThemeState.manual) ThemeState.isDark else isSystemInDarkTheme()

//    uiController.setSystemBarsColor(color = Color.Transparent)

    val colors = if (ThemeState.isDark) {
        DarkThemeColors
    } else {
        LightThemeColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
