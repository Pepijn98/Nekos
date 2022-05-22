package dev.vdbroek.nekos.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.IS_DARK
import dev.vdbroek.nekos.utils.MANUAL
import dev.vdbroek.nekos.utils.STAGGERED
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    navController: NavHostController,
    dataStore: DataStore<Preferences>
) {
    App.screenTitle = "Settings"

    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val coroutine = rememberCoroutineScope()

    val themeOptions = listOf("dark", "light", "system")
    val defaultThemeOption = if (ThemeState.manual) {
        if (ThemeState.isDark) {
            themeOptions[0]
        } else {
            themeOptions[1]
        }
    } else {
        themeOptions[2]
    }
    val (selectedThemeOption, onThemeOptionSelected) = remember { mutableStateOf(defaultThemeOption) }

    val layoutOptions = listOf("normal", "staggered")
    val defaultLayoutOption = if (ThemeState.staggered) {
        layoutOptions[1]
    } else {
        layoutOptions[0]
    }
    val (selectedLayoutOption, onLayoutOptionSelected) = remember { mutableStateOf(defaultLayoutOption) }

    Column {
        Text(
            modifier = Modifier.padding(start = 24.dp),
            text = "Theme",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall
        )
        Divider(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column {
            themeOptions.forEach { text ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = text == selectedThemeOption,
                            onClick = {
                                onThemeOptionSelected(text)
                                coroutine.launch { changeTheme(text, isSystemInDarkTheme, dataStore) }
                            }
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        modifier = Modifier.padding(all = Dp(value = 8F)),
                        selected = (text == selectedThemeOption),
                        onClick = {
                            onThemeOptionSelected(text)
                            coroutine.launch { changeTheme(text, isSystemInDarkTheme, dataStore) }
                        }
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() },
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Text(
            modifier = Modifier.padding(start = 24.dp),
            text = "Layout",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall
        )
        Divider(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column {
            layoutOptions.forEach { text ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = text == selectedLayoutOption,
                            onClick = {
                                onLayoutOptionSelected(text)
                                coroutine.launch { changeLayout(text, dataStore) }
                            }
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        modifier = Modifier.padding(all = Dp(value = 8F)),
                        selected = (text == selectedLayoutOption),
                        onClick = {
                            onLayoutOptionSelected(text)
                            coroutine.launch { changeLayout(text, dataStore) }
                        }
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = text.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() },
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

suspend fun changeTheme(
    theme: String,
    isSystemInDarkTheme: Boolean,
    dataStore: DataStore<Preferences>
) {
    when (theme) {
        "dark" -> {
            ThemeState.manual = true
            ThemeState.isDark = true
        }
        "light" -> {
            ThemeState.manual = true
            ThemeState.isDark = false
        }
        "system" -> {
            ThemeState.manual = false
            ThemeState.isDark = isSystemInDarkTheme
        }
    }

    dataStore.edit { preferences ->
        preferences[MANUAL] = ThemeState.manual
        preferences[IS_DARK] = ThemeState.isDark
    }
}

suspend fun changeLayout(
    layout: String,
    dataStore: DataStore<Preferences>
) {
    when (layout) {
        "normal" -> {
            ThemeState.staggered = false
        }
        "staggered" -> {
            ThemeState.staggered = true
        }
    }

    dataStore.edit { preferences ->
        preferences[STAGGERED] = ThemeState.staggered
    }
}
