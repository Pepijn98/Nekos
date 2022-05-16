package dev.vdbroek.nekos.utils

import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.vdbroek.nekos.api.User
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import kotlinx.coroutines.delay

class UserState : ViewModel() {
    var isLoggedIn by mutableStateOf(false)
    var isBusy by mutableStateOf(false)
    var token by mutableStateOf("")

    suspend fun signIn(scaffoldState: ScaffoldState, username: String, password: String) {
        isBusy = true
        val (data, exception) = User.authenticate(username, password)
        when {
            data != null -> {
                token = data.token
                isLoggedIn = true
            }
            exception != null -> {
                scaffoldState.snackbarHostState.showCustomSnackbar(
                    message = exception.message ?: "Authentication Faild",
                    actionLabel = "X",
                    snackbarType = SnackbarType.DANGER,
                    duration = SnackbarDuration.Long
                )
            }
        }
        isBusy = false
    }

    suspend fun signOut() {
        isBusy = true
        delay(2000)
        isLoggedIn = false
        isBusy = false
    }
}

val LocalUserState = compositionLocalOf<UserState> { error("User State Context Not Found!") }
