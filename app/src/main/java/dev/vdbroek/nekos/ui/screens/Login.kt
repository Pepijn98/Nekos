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
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.components.RoundedTextField
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.utils.App
import kotlinx.coroutines.launch

object LoginState {
    var username by mutableStateOf("")
    var password by mutableStateOf("")
}

@Composable
fun Login(state: ScaffoldState, navController: NavHostController) {
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
                            state.snackbarHostState.showCustomSnackbar(
                                message = "TODO : Navigate to home screen and pop backstack",
                                snackbarType = SnackbarType.INFO
                            )
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
