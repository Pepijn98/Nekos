package dev.vdbroek.nekos.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vdbroek.nekos.ui.theme.ColorUI
import java.util.*

enum class SnackbarType {
    DEFAULT,
    INFO,
    SUCCESS,
    WARNING,
    DANGER
}

var SnackbarHostState.type by mutableStateOf(SnackbarType.DEFAULT)

suspend fun SnackbarHostState.showSnackbar(
    message: String,
    actionLabel: String? = null,
    snackbarType: SnackbarType = SnackbarType.DEFAULT,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult {
    type = snackbarType
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
            val backgroundColor = when (hostState.type) {
                SnackbarType.DEFAULT -> ColorUI.dark
                SnackbarType.INFO -> ColorUI.info
                SnackbarType.SUCCESS -> ColorUI.success
                SnackbarType.WARNING -> ColorUI.warning
                SnackbarType.DANGER -> ColorUI.danger
            }

            val textColor = when (hostState.type) {
                SnackbarType.WARNING -> Color.Black
                else -> Color.White
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
