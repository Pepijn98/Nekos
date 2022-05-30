package dev.vdbroek.nekos.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import dev.vdbroek.nekos.MainActivity
import dev.vdbroek.nekos.api.Nekos
import dev.vdbroek.nekos.api.NekosRequestState
import dev.vdbroek.nekos.components.HtmlText
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.ui.theme.NekoColors
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.*
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarState
import java.util.*

private var openStaggeredWarning by mutableStateOf(false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    toolbarState: CollapsingToolbarState
) {
    App.screenTitle = "Settings"

    val activity = LocalActivity.current
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
    var selectedThemeOption by remember { mutableStateOf(defaultThemeOption) }

    val layoutOptions = listOf("fixed", "staggered")
    val defaultLayoutOption = if (ThemeState.staggered) layoutOptions[1] else layoutOptions[0]
    var selectedLayoutOption by remember { mutableStateOf(defaultLayoutOption) }

    val contentOptions = listOf("everything", "no_nsfw", "only_nsfw")
    var selectedContentOption by remember { mutableStateOf(App.nsfw) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .scrollable(
                state = toolbarState,
                orientation = Orientation.Vertical
            )
    ) {
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
                                selectedThemeOption = text
                                coroutine.launch { changeTheme(text, isSystemInDarkTheme, context.dataStore) }
                            }
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        modifier = Modifier.padding(all = Dp(value = 8F)),
                        selected = (text == selectedThemeOption),
                        onClick = {
                            selectedThemeOption = text
                            coroutine.launch { changeTheme(text, isSystemInDarkTheme, context.dataStore) }
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
                                if (text == "staggered") {
                                    openStaggeredWarning = true
                                } else {
                                    selectedLayoutOption = text
                                    coroutine.launch { changeLayout(text, context.dataStore) }
                                }
                            }
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        modifier = Modifier
                            .padding(all = 8.dp),
                        selected = (text == selectedLayoutOption),
                        onClick = {
                            if (text == "staggered") {
                                openStaggeredWarning = true
                            } else {
                                selectedLayoutOption = text
                                coroutine.launch { changeLayout(text, context.dataStore) }
                            }
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

        if (App.uncensored) {
            Text(
                modifier = Modifier.padding(start = 24.dp),
                text = "Content",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall
            )
            Divider(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Column {
                contentOptions.forEach { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = text == selectedContentOption,
                                onClick = {
                                    selectedContentOption = text
                                    App.nsfw = text
                                    coroutine.launch {
                                        context.dataStore.edit { it[NSFW] = App.nsfw }
                                        updateImages()
                                    }
                                }
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            modifier = Modifier.padding(all = Dp(value = 8F)),
                            selected = (text == selectedContentOption),
                            onClick = {
                                selectedContentOption = text
                                App.nsfw = text
                                coroutine.launch {
                                    context.dataStore.edit { it[NSFW] = App.nsfw }
                                    updateImages()
                                }
                            }
                        )
                        Text(
                            modifier = Modifier.padding(start = 4.dp),
                            text = text.split("_").joinToString(" ") { it.capitalize() },
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth()
        ) {
            val year = Calendar.getInstance().get(Calendar.YEAR)
            HtmlText(
                modifier = Modifier
                    .align(Alignment.Center),
                html = """
                    <p>© $year — <a href="https://vdbroek.dev">Pepijn</a> | v${App.version} (${App.versionCode})<br/>
                    Made possible with <a href="https://nekos.moe">nekos.moe</a><br/></p>
                    """.trimIndent()
            )
        }
    }

    // TODO : Move Dialog to components/dialog as StaggeredWarningDialog
    if (openStaggeredWarning) {
        Dialog(
            onDismissRequest = {
                openStaggeredWarning = false
            }
        ) {
            Card(
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(10.dp, 5.dp, 10.dp, 10.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column {
                    Image(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(color = if (ThemeState.isDark) NekoColors.warning else NekoColors.warning.dim(0.9f)),
                        modifier = Modifier
                            .padding(top = 35.dp)
                            .height(70.dp)
                            .fillMaxWidth()
                    )

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Experimental UI Warning",
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .fillMaxWidth(),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Staggered layout grid is an experimental feature and will have some bugs. The app needs to restart to apply the changes.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(top = 10.dp, start = 25.dp, end = 25.dp)
                                .fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {

                        TextButton(onClick = {
                            selectedLayoutOption = layoutOptions[0]
                            openStaggeredWarning = false
                        }) {

                            Text(
                                text = "Not Now",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                            )
                        }
                        TextButton(onClick = {
                            selectedLayoutOption = layoutOptions[1]
                            coroutine.launch { changeLayout(layoutOptions[1], context.dataStore) }
                            openStaggeredWarning = false

                            // Restart app
                            val mainActivity = Intent(context, MainActivity::class.java)
                            activity.finish()
                            activity.startActivity(mainActivity)
                        }) {
                            Text(
                                text = "I Understand",
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(top = 5.dp, bottom = 5.dp)
                            )
                        }
                    }
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
        "fixed" -> {
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

suspend fun updateImages() {
    HomeScreenState.images.clear()
    NekosRequestState.apply {
        end = false
        skip = 0
    }

    val (response, exception) = Nekos.getImages()
    when {
        response != null -> {
            HomeScreenState.images.addAll(response.images)
        }
        exception != null -> {
            App.snackbarHost.showCustomSnackbar(
                message = exception.message ?: "Failed requesting images",
                actionLabel = "x",
                withDismissAction = true,
                snackbarType = SnackbarType.DANGER
            )
        }
    }
}
