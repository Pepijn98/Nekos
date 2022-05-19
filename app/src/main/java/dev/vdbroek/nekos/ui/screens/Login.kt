package dev.vdbroek.nekos.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ScaffoldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

object LoginState {
    var username by mutableStateOf("")
    var password by mutableStateOf("")
}

@Composable
fun Login(
    state: ScaffoldState,
    dataStore: DataStore<Preferences>,
    navController: NavHostController
) {
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = true) {
        state.drawerState.close()
    }

    App.screenTitle = "Login"

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(id = R.drawable.wave),
                contentDescription = "",
                contentScale = ContentScale.FillHeight,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Image(
                    modifier = Modifier.size(148.dp),
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary)
                )
                Text(
                    text = App.screenTitle,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val maxUsernameChars = 15
            val maxPasswordChars = 15

            RoundedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                text = LoginState.username,
                placeholder = "Username",
                counter = true,
                maxChar = maxUsernameChars,
                icon = { Icon(imageVector = Icons.Filled.Person, contentDescription = "Username icon") }
            ) {
                if (it.length > maxUsernameChars) return@RoundedTextField
                LoginState.username = it
            }
            RoundedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                text = LoginState.password,
                placeholder = "Password",
                counter = true,
                maxChar = maxPasswordChars,
                isPassword = true,
                icon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = "Password icon") }
            ) {
                if (it.length > maxPasswordChars) return@RoundedTextField
                LoginState.password = it
            }
            Column(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = CircleShape,
                    onClick = {
                        scope.launch {
                            val (login, loginException) = User.authenticate(LoginState.username, LoginState.password)
                            when {
                                login != null -> {
                                    UserState.token = login.token
                                    UserState.isLoggedIn = true
                                    val (userData, userException) = User.getMe()
                                    when {
                                        userData != null -> {
                                            UserState.name = userData.user.username

                                            dataStore.edit { preferences ->
                                                preferences[TOKEN] = UserState.token ?: ""
                                                preferences[USERNAME] = UserState.name ?: ""
                                                preferences[IS_LOGGED_IN] = UserState.isLoggedIn
                                            }

                                            navController.backQueue.clear()
                                            navController.navigate(Screens.Home.route)
                                        }
                                        userException != null -> {
                                            state.snackbarHostState.showCustomSnackbar(
                                                message = userException.message ?: "Could not retrieve user data",
                                                snackbarType = SnackbarType.DANGER
                                            )
                                        }
                                    }
                                }
                                loginException != null -> {
                                    state.snackbarHostState.showCustomSnackbar(
                                        message = loginException.message ?: "Failed to login",
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
                        scope.launch {
                            state.snackbarHostState.showCustomSnackbar(message = "TODO : Open forgot password url", snackbarType = SnackbarType.INFO)
                        }
                    }
                ) {
                    Text(text = "FORGOT PASSWORD")
                }
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = CircleShape,
                    onClick = {
                        scope.launch {
                            state.snackbarHostState.showCustomSnackbar(message = "TODO : Navigate to register screen", snackbarType = SnackbarType.INFO)
                        }
                    }
                ) {
                    Text(text = "New User? Register Now")
                }
            }
        }
    }
}
