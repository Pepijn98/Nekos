package dev.vdbroek.nekos.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vdbroek.nekos.ui.theme.ColorUI
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
    snackbarType: SnackbarType = SnackbarType.DEFAULT,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult {
    println(isActive)
    // If there's currently a snackbar dismiss that one and open the new one
    if (isActive) {
        currentSnackbarData?.dismiss()
        isActive = false
    }

    type = snackbarType
    isActive = true
    return showSnackbar(message, actionLabel, duration)
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
            if (data.duration != SnackbarDuration.Indefinite) {
                val time = when (data.duration) {
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
                SnackbarType.DEFAULT -> if (ThemeState.isDark) ColorUI.darkCard else MaterialTheme.colorScheme.background
                SnackbarType.INFO -> ColorUI.info
                SnackbarType.SUCCESS -> ColorUI.success
                SnackbarType.WARNING -> ColorUI.warning
                SnackbarType.DANGER -> ColorUI.danger
            }

            val textColor = when (hostState.type) {
                SnackbarType.DEFAULT -> if (ThemeState.isDark) ColorUI.light else MaterialTheme.colorScheme.onBackground
                SnackbarType.WARNING -> Color.Black
                else -> ColorUI.light
            }

            Snackbar(
                modifier = Modifier.padding(8.dp),
                backgroundColor = backgroundColor,
                elevation = 1.dp,
                action = {
                    data.actionLabel?.let { label ->
                        if (label.lowercase(Locale.getDefault()) == "x") {
                            IconButton(onClick = { onDismiss() }) {
                                Icon(imageVector = Icons.Rounded.Clear, contentDescription = null)
                            }
                        } else {
                            TextButton(onClick = { onDismiss() }) {
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
                },
                content = {
                    Text(
                        text = data.message,
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
