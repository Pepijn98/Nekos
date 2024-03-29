package dev.vdbroek.nekos.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.api.User
import dev.vdbroek.nekos.api.UserState
import dev.vdbroek.nekos.components.RoundedTextField
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private object LoginState {
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    var usernameError by mutableStateOf(false)
    var passwordError by mutableStateOf(false)
}

private fun login(
    coroutine: CoroutineScope,
    context: Context,
    navigation: NavHostController
) {
    coroutine.launch {
        if (LoginState.usernameError) {
            App.snackbarHost.showCustomSnackbar(
                message = "Invalid username",
                actionLabel = "x",
                withDismissAction = true,
                snackbarType = SnackbarType.WARNING,
                duration = SnackbarDuration.Short
            )
            return@launch
        }

        if (LoginState.passwordError) {
            App.snackbarHost.showCustomSnackbar(
                message = "Invalid password",
                actionLabel = "x",
                withDismissAction = true,
                snackbarType = SnackbarType.WARNING,
                duration = SnackbarDuration.Short
            )
            return@launch
        }

        if (LoginState.username.isBlank() || LoginState.password.isBlank()) {
            App.snackbarHost.showCustomSnackbar(
                message = "One or more required fields are blank",
                actionLabel = "x",
                withDismissAction = true,
                snackbarType = SnackbarType.WARNING,
                duration = SnackbarDuration.Short
            )
            return@launch
        }

        val (login, loginException) = User.authenticate(
            username = LoginState.username,
            password = LoginState.password
        )
        when {
            login != null -> {
                UserState.token = login.token
                UserState.isLoggedIn = true
                val (userData, userException) = User.getMe()
                when {
                    userData != null -> {
                        UserState.username = userData.user.username

                        context.dataStore.edit { preferences ->
                            preferences[TOKEN] = UserState.token ?: ""
                            preferences[USERNAME] = UserState.username ?: ""
                            preferences[IS_LOGGED_IN] = UserState.isLoggedIn
                        }

                        navigation.backQueue.clear()
                        navigation.navigate(Screens.Home.route)
                    }
                    userException != null -> {
                        App.snackbarHost.showCustomSnackbar(
                            message = userException.message ?: "Could not retrieve user data",
                            actionLabel = "x",
                            withDismissAction = true,
                            snackbarType = SnackbarType.DANGER
                        )
                    }
                }
            }
            loginException != null -> {
                App.snackbarHost.showCustomSnackbar(
                    message = loginException.message ?: "Failed to login",
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
fun Login() {
    App.screenTitle = "Login"

    val keyboard = LocalSoftwareKeyboardController.current
    val navigation = LocalNavigation.current
    val context = LocalContext.current

    val coroutine = rememberCoroutineScope()

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
                text = LoginState.username,
                placeholder = "Username",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                counter = true,
                isError = LoginState.usernameError,
                maxChar = App.maxUsernameChars,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Username icon"
                    )
                }
            ) {
                if (it.length <= App.maxUsernameChars) {
                    LoginState.apply {
                        username = it
                        usernameError = !App.validateUsername(it)
                    }
                }
            }

            RoundedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                text = LoginState.password,
                placeholder = "Password",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboard?.hide()
                        login(coroutine, context, navigation)
                    }
                ),
                counter = true,
                isError = LoginState.passwordError,
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
                    LoginState.apply {
                        password = it
                        passwordError = !App.validatePassword(it)

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
                        login(coroutine, context, navigation)
                    }
                ) {
                    Text(text = "Login")
                }

                TextButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = CircleShape,
                    onClick = {
                        navigation.navigate(Screens.Register.route)
                    }
                ) {
                    Text(text = "New User? Register Now")
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
