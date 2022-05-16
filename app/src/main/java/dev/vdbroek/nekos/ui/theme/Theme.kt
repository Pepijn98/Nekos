package dev.vdbroek.nekos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeState {
    var isDark by mutableStateOf(true)
    var manual by mutableStateOf(false)
}

private val DarkColorScheme = darkColorScheme(
    primary = ColorUI.blue200,
    secondary = ColorUI.blue500,
    tertiary = ColorUI.blue700,
    background = ColorUI.dark,
    surface = ColorUI.dark,
    error = ColorUI.danger,
    onPrimary = ColorUI.dark,
    onSecondary = ColorUI.dark,
    onTertiary = ColorUI.light,
    onBackground = ColorUI.light,
    onError = ColorUI.light
)

private val LightColorScheme = lightColorScheme(
    primary = ColorUI.blue200,
    secondary = ColorUI.blue500,
    tertiary = ColorUI.blue700,
    background = ColorUI.light,
    surface = ColorUI.light,
    error = ColorUI.danger,
    onPrimary = ColorUI.dark,
    onSecondary = ColorUI.dark,
    onTertiary = ColorUI.light,
    onBackground = ColorUI.dark,
    onError = ColorUI.light
)

@Composable
fun NekosTheme(content: @Composable () -> Unit) {
    ThemeState.isDark = if (ThemeState.manual) ThemeState.isDark else isSystemInDarkTheme()

    val colors = if (ThemeState.isDark) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
