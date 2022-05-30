package dev.vdbroek.nekos.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.vdbroek.nekos.api.User
import dev.vdbroek.nekos.components.RoundedTextField
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.LocalNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private object RegisterState {
    var email by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    var emailError by mutableStateOf(false)
    var usernameError by mutableStateOf(false)
    var passwordError by mutableStateOf(false)
    var confirmPasswordError by mutableStateOf(false)
}

private fun register() {
    val coroutine = CoroutineScope(Dispatchers.Default)
    coroutine.launch {
        if (RegisterState.emailError) {
            App.snackbarHost.showCustomSnackbar(
                message = "Invalid email address",
                actionLabel = "x",
                withDismissAction = true,
                snackbarType = SnackbarType.WARNING,
                duration = SnackbarDuration.Short
            )
            return@launch
        }

        if (RegisterState.usernameError) {
            App.snackbarHost.showCustomSnackbar(
                message = "Invalid username",
                actionLabel = "x",
                withDismissAction = true,
                snackbarType = SnackbarType.WARNING,
                duration = SnackbarDuration.Short
            )
            return@launch
        }

        if (RegisterState.passwordError) {
            App.snackbarHost.showCustomSnackbar(
                message = "Invalid password",
                actionLabel = "x",
                withDismissAction = true,
                snackbarType = SnackbarType.WARNING,
                duration = SnackbarDuration.Short
            )
            return@launch
        }

        if (RegisterState.confirmPasswordError) {
            App.snackbarHost.showCustomSnackbar(
                message = "Invalid password confirmation",
                actionLabel = "x",
                withDismissAction = true,
                snackbarType = SnackbarType.WARNING,
                duration = SnackbarDuration.Short
            )
            return@launch
        }

        if (
            RegisterState.email.isBlank() ||
            RegisterState.username.isBlank() ||
            RegisterState.password.isBlank() ||
            RegisterState.confirmPassword.isBlank()
        ) {
            App.snackbarHost.showCustomSnackbar(
                message = "One or more required fields are blank",
                actionLabel = "x",
                withDismissAction = true,
                snackbarType = SnackbarType.WARNING,
                duration = SnackbarDuration.Short
            )
            return@launch
        }

        val (message, exception) = User.register(
            email = RegisterState.email,
            username = RegisterState.username,
            password = RegisterState.password
        )

        when {
            message != null -> {
                RegisterState.apply {
                    email = ""
                    username = ""
                    password = ""
                    confirmPassword = ""
                }

                App.snackbarHost.showCustomSnackbar(
                    message = message,
                    actionLabel = "x",
                    withDismissAction = true,
                    snackbarType = SnackbarType.INFO
                )
            }
            exception != null -> {
                App.snackbarHost.showCustomSnackbar(
                    message = exception.message ?: "Failed to register",
                    actionLabel = "x",
                    withDismissAction = true,
                    snackbarType = SnackbarType.DANGER
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Register() {
    App.screenTitle = "Register"

    val keyboard = LocalSoftwareKeyboardController.current
    val navigation = LocalNavigation.current

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // -START: HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 100.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    modifier = Modifier
                        .size(148.dp),
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.background)
                )
                Text(
                    text = App.screenTitle,
                    color = MaterialTheme.colorScheme.background,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // -END: HEADER

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 36.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // -START: INPUT
            RoundedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                text = RegisterState.email,
                placeholder = "Email",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                counter = true,
                isError = RegisterState.emailError,
                maxChar = App.maxEmailChars,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = "Email icon"
                    )
                }
            ) {
                if (it.length <= App.maxEmailChars) {
                    RegisterState.apply {
                        email = it
                        emailError = !App.validateEmail(it)
                    }
                }
            }

            RoundedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                text = RegisterState.username,
                placeholder = "Username",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                counter = true,
                isError = RegisterState.usernameError,
                maxChar = App.maxUsernameChars,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Username icon"
                    )
                }
            ) {
                if (it.length <= App.maxUsernameChars) {
                    RegisterState.apply {
                        username = it
                        usernameError = !App.validateUsername(it)
                    }
                }
            }

            RoundedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                text = RegisterState.password,
                placeholder = "Password",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                counter = true,
                isError = RegisterState.passwordError,
                maxChar = App.maxPasswordChars,
                isPassword = true,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Password icon"
                    )
                }
            ) {
                if (it.length <= App.maxPasswordChars) {
                    RegisterState.apply {
                        password = it
                        passwordError = !App.validatePassword(it)
                    }
                }
            }

            RoundedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                text = RegisterState.confirmPassword,
                placeholder = "Confirm Password",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboard?.hide()
                        register()
                    }
                ),
                counter = true,
                isError = RegisterState.confirmPasswordError,
                maxChar = App.maxPasswordChars,
                isPassword = true,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Password icon"
                    )
                }
            ) {
                if (it.length <= App.maxPasswordChars) {
                    RegisterState.apply {
                        confirmPassword = it
                        confirmPasswordError = !App.validatePassword(it) || confirmPassword != password
                    }
                }
            }
            // -END: INPUT

            Column(
                modifier = Modifier
                    .padding(top = 16.dp)
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = CircleShape,
                    onClick = {
                        register()
                    }
                ) {
                    Text(text = "Register")
                }

                OutlinedIconButton(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .align(Alignment.CenterHorizontally),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (ThemeState.isDark) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                    ),
                    shape = CircleShape,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = if (ThemeState.isDark) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onBackground
                        }
                    ),
                    onClick = {
                        navigation.popBackStack()
                    }
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(4.dp),
                        imageVector = Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Back"
                    )
                }
            }
        }
    }
}
