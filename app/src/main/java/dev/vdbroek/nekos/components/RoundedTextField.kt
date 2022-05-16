package dev.vdbroek.nekos.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.dim
import dev.vdbroek.nekos.utils.lighten

@Composable
fun RoundedTextField(
    modifier: Modifier = Modifier,
    text: String,
    placeholder: String,
    counter: Boolean = false,
    maxChar: Int = 10,
    isPassword: Boolean = false,
    icon: @Composable (() -> Unit)? = null,
    onValueChange: (String) -> Unit
) {
    var pwVisible by rememberSaveable { mutableStateOf(false) }

    Column {
        TextField(
            modifier = modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), CircleShape),
            value = text,
            onValueChange = onValueChange,
            placeholder = {
                Text(text = placeholder)
            },
            keyboardOptions = if (isPassword) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
            visualTransformation = if (!pwVisible && isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            shape = CircleShape,
            leadingIcon = icon,
            trailingIcon = {
                if (isPassword) {
                    val image = if (pwVisible) painterResource(id = R.drawable.visibility) else painterResource(id = R.drawable.visibility_off)
                    val description = if (pwVisible) "Hide password" else "Show password"
                    IconButton(onClick = { pwVisible = !pwVisible }) {
                        Icon(painter = image, description)
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
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        if (counter) {
            Text(
                text = "${text.length} / $maxChar",
                textAlign = TextAlign.End,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
