package dev.vdbroek.nekos.api

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.models.LoginResponse
import dev.vdbroek.nekos.models.UserResponse
import dev.vdbroek.nekos.utils.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

object UserState {
    var isLoggedIn by mutableStateOf(false)
    var isBusy by mutableStateOf(false)
    var token by mutableStateOf<String?>(null)

    suspend fun signIn(snackbarHost: SnackbarHostState, username: String, password: String) {
        isBusy = true
        val (data, exception) = User.authenticate(username, password)
        when {
            data != null -> {
                token = data.token
                isLoggedIn = true
            }
            exception != null -> {
                snackbarHost.showCustomSnackbar(
                    message = exception.message ?: "Authentication Faild",
                    actionLabel = "X",
                    withDismissAction = true,
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

object User {
    private val coroutine = CoroutineScope(Dispatchers.IO)

    suspend fun authenticate(username: String, password: String): Response<LoginResponse?, Exception?> {
        val (_, _, result) = coroutine.async {
            return@async "/auth".httpPost()
                .header(mapOf("Content-Type" to "application/json"))
                .body("{\"username\": \"$username\", \"password\": \"$password\"}")
                .responseString()
        }.await()

        val (data, exception) = result
        return when (result) {
            is Result.Success -> {
                if (data != null) {
                    Response(Gson().fromJson(data, LoginResponse::class.java), null)
                } else {
                    Response(null, Exception("[AUTH]: Invalid response from API"))
                }
            }
            is Result.Failure -> {
                Api.handleException(exception, "AUTH")
            }
        }
    }

    suspend fun register(username: String, email: String, password: String): Response<String?, Exception?> {
        val (_, response, result) = coroutine.async {
            return@async "/register".httpPost()
                .header(mapOf("Content-Type" to "application/json"))
                .body("{\"username\": \"$username\", \"email\": \"$email\", \"password\": \"$password\"}")
                .responseString()
        }.await()

        val (data, exception) = result
        return when (result) {
            is Result.Success -> {
                if (data != null || response.statusCode == 201) {
                    Response("A confirmation email has been send, please confirm before logging in.", null)
                } else {
                    Response(null, Exception("[REGISTER]: Invalid response from API"))
                }
            }
            is Result.Failure -> {
                Api.handleException(exception, "REGISTER")
            }
        }
    }

    suspend fun getMe(): Response<UserResponse?, Exception?> {
        if (UserState.token.isNullOrBlank())
            return Response(null, Exception("Not logged in"))

        val (_, _, result) = coroutine.async {
            return@async "/user/@me".httpGet()
                .header(mapOf("Authorization" to UserState.token!!))
                .responseString()
        }.await()

        val (data, exception) = result
        return when (result) {
            is Result.Success -> {
                if (data != null) {
                    Response(Gson().fromJson(data, UserResponse::class.java), null)
                } else {
                    Response(null, Exception("[GET_ME]: Invalid response from API"))
                }
            }
            is Result.Failure -> {
                Api.handleException(exception, "GET_ME")
            }
        }
    }
}
