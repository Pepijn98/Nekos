package dev.vdbroek.nekos.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.api.User
import dev.vdbroek.nekos.api.UserState
import dev.vdbroek.nekos.components.RoundedTextField
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.IS_LOGGED_IN
import dev.vdbroek.nekos.utils.TOKEN
import dev.vdbroek.nekos.utils.USERNAME
import kotlinx.coroutines.launch

@Composable
fun Login(
    snackbarHost: SnackbarHostState,
    dataStore: DataStore<Preferences>,
    navController: NavHostController
) {
    App.screenTitle = "Login"

    val coroutine = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var usernameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // -START: HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            Image(
                modifier = Modifier
                    .fillMaxSize(),
                painter = painterResource(id = R.drawable.header_login),
                contentDescription = "Login header",
                contentScale = ContentScale.FillHeight,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            IconButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 6.dp, top = 6.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.background,
                        shape = CircleShape
                    ),
                onClick = {
                    navController.popBackStack()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "Back"
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // -START: INPUT
            RoundedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                text = username,
                placeholder = "Username",
                counter = true,
                isError = usernameError,
                maxChar = App.maxUsernameChars,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Username icon"
                    )
                }
            ) {
                if (it.length <= App.maxUsernameChars) {
                    username = it
                    usernameError = !App.validateUsername(it)
                }
            }

            RoundedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                text = password,
                placeholder = "Password",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                counter = true,
                isError = passwordError,
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
                    password = it
                    passwordError = !App.validatePassword(it)
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
                        if (usernameError) {
                            coroutine.launch {
                                snackbarHost.showCustomSnackbar(
                                    message = "Invalid username",
                                    actionLabel = "x",
                                    withDismissAction = true,
                                    snackbarType = SnackbarType.WARNING,
                                    duration = SnackbarDuration.Short
                                )
                            }
                            return@Button
                        }

                        if (passwordError) {
                            coroutine.launch {
                                snackbarHost.showCustomSnackbar(
                                    message = "Invalid password",
                                    actionLabel = "x",
                                    withDismissAction = true,
                                    snackbarType = SnackbarType.WARNING,
                                    duration = SnackbarDuration.Short
                                )
                            }
                            return@Button
                        }

                        if (username.isBlank() || password.isBlank()) {
                            coroutine.launch {
                                snackbarHost.showCustomSnackbar(
                                    message = "One or more required fields are blank",
                                    actionLabel = "x",
                                    withDismissAction = true,
                                    snackbarType = SnackbarType.WARNING,
                                    duration = SnackbarDuration.Short
                                )
                            }
                            return@Button
                        }

                        coroutine.launch {
                            val (login, loginException) = User.authenticate(
                                username = username,
                                password = password
                            )
                            when {
                                login != null -> {
                                    UserState.token = login.token
                                    UserState.isLoggedIn = true
                                    val (userData, userException) = User.getMe()
                                    when {
                                        userData != null -> {
                                            UserState.username = userData.user.username

                                            dataStore.edit { preferences ->
                                                preferences[TOKEN] = UserState.token ?: ""
                                                preferences[USERNAME] = UserState.username ?: ""
                                                preferences[IS_LOGGED_IN] = UserState.isLoggedIn
                                            }

                                            navController.backQueue.clear()
                                            navController.navigate(Screens.Home.route)
                                        }
                                        userException != null -> {
                                            snackbarHost.showCustomSnackbar(
                                                message = userException.message ?: "Could not retrieve user data",
                                                withDismissAction = true,
                                                snackbarType = SnackbarType.DANGER
                                            )
                                        }
                                    }
                                }
                                loginException != null -> {
                                    snackbarHost.showCustomSnackbar(
                                        message = loginException.message ?: "Failed to login",
                                        withDismissAction = true,
                                        snackbarType = SnackbarType.DANGER
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text(text = "Login")
                }

                TextButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = CircleShape,
                    onClick = {
                        navController.navigate(Screens.Register.route)
                    }
                ) {
                    Text(text = "New User? Register Now")
                }
            }
        }
    }
}
