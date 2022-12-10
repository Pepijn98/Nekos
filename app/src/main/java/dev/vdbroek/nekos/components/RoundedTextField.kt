package dev.vdbroek.nekos.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.alpha
import dev.vdbroek.nekos.utils.dim
import dev.vdbroek.nekos.utils.lighten

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundedTextField(
    modifier: Modifier = Modifier,
    text: String,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    counter: Boolean = false,
    maxChar: Int = 10,
    isPassword: Boolean = false,
    isError: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    icon: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    var pwVisible by rememberSaveable { mutableStateOf(false) }

    val primaryErrorColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    val mCustomTextSelectionColors = TextSelectionColors(
        handleColor = primaryErrorColor,
        backgroundColor = primaryErrorColor.alpha(0.6f)
    )

    Column {
        CompositionLocalProvider(LocalTextSelectionColors provides mCustomTextSelectionColors) {
            TextField(
                modifier = modifier
                    .border(
                        border = BorderStroke(
                            width = 1.dp,
                            color = primaryErrorColor
                        ),
                        shape = CircleShape
                    ),
                value = text,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = placeholder
                    )
                },
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = if (!pwVisible && isPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                isError = isError,
                singleLine = singleLine,
                maxLines = maxLines,
                shape = CircleShape,
                leadingIcon = icon,
                trailingIcon = {
                    if (isPassword) {
                        val description = if (pwVisible) {
                            "Hide password"
                        } else {
                            "Show password"
                        }

                        IconButton(
                            onClick = {
                                pwVisible = !pwVisible
                            }
                        ) {
                            Icon(
                                imageVector = if (pwVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = description
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    placeholderColor = if (ThemeState.isDark) {
                        MaterialTheme.colorScheme.onBackground.dim(0.6f)
                    } else {
                        MaterialTheme.colorScheme.onBackground.lighten(0.6f)
                    },
                    cursorColor = MaterialTheme.colorScheme.primary,
                    errorCursorColor = MaterialTheme.colorScheme.error,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onBackground,
                    errorTrailingIconColor = MaterialTheme.colorScheme.onBackground,
                    focusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onBackground,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        if (counter) {
            Text(
                modifier = Modifier
                    .fillMaxWidth(),
                text = "${text.length} / $maxChar",
                textAlign = TextAlign.End,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
