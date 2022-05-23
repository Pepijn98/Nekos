package dev.vdbroek.nekos.components

//import androidx.compose.material.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vdbroek.nekos.ui.theme.NekoColors
import dev.vdbroek.nekos.ui.theme.ThemeState
import kotlinx.coroutines.delay
import java.util.*

enum class SnackbarType {
    DEFAULT,
    INFO,
    SUCCESS,
    WARNING,
    DANGER
}

/**
 * If there's currently a snackbar shown
 */
var SnackbarHostState.isActive by mutableStateOf(false)
var SnackbarHostState.type by mutableStateOf(SnackbarType.DEFAULT)

suspend fun SnackbarHostState.showCustomSnackbar(
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean,
    snackbarType: SnackbarType = SnackbarType.DEFAULT,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult {
    // If there's currently a snackbar dismiss that one and open the new one
    if (isActive) {
        currentSnackbarData?.dismiss()
        isActive = false
    }

    type = snackbarType
    isActive = true
    return showSnackbar(message, actionLabel, withDismissAction, duration)
}

@Composable
fun Alert(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit?
) {
    SnackbarHost(
        modifier = modifier,
        hostState = hostState,
        snackbar = { data ->
            // Update snackbar active state when snackbar auto hides after X seconds.
            // Unless it's set to Indefinite, which means the user has to manually dismiss the snackbar
            if (data.visuals.duration != SnackbarDuration.Indefinite) {
                val time = when (data.visuals.duration) {
                    SnackbarDuration.Long -> 10000L
                    SnackbarDuration.Short -> 4000L
                    else -> return@SnackbarHost // Else will never reach but Kotlin doesn't seem to recognize that
                }

                LaunchedEffect(key1 = true) {
                    delay(time)
                    onDismiss()
                }
            }

            val backgroundColor = when (hostState.type) {
                SnackbarType.DEFAULT -> if (ThemeState.isDark) NekoColors.darkCard else MaterialTheme.colorScheme.background
                SnackbarType.INFO -> NekoColors.info
                SnackbarType.SUCCESS -> NekoColors.success
                SnackbarType.WARNING -> NekoColors.warning
                SnackbarType.DANGER -> NekoColors.danger
            }

            val textColor = when (hostState.type) {
                SnackbarType.DEFAULT -> if (ThemeState.isDark) NekoColors.light else MaterialTheme.colorScheme.onBackground
                SnackbarType.WARNING -> NekoColors.dark
                else -> NekoColors.light
            }

            Snackbar(
                modifier = Modifier
                    .padding(8.dp),
                containerColor = backgroundColor,
                contentColor = textColor,
                actionContentColor = textColor,
                action = {
                    data.visuals.actionLabel?.let { label ->
                        when (label.lowercase(Locale.getDefault())) {
                            "x" -> {
                                IconButton(
                                    onClick = {
                                        onDismiss()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Clear,
                                        contentDescription = null
                                    )
                                }
                            }
                            "home" -> {
                                IconButton(
                                    onClick = {
                                        onDismiss()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Home,
                                        contentDescription = null
                                    )
                                }
                            }
                            else -> {
                                TextButton(
                                    onClick = {
                                        onDismiss()
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        style = TextStyle(
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.W500,
                                            fontSize = 16.sp
                                        ),
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                },
                content = {
                    Text(
                        text = data.visuals.message,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.W500,
                            fontSize = 16.sp
                        ),
                        color = textColor
                    )
                }
            )
        }
    )
}
