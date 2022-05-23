package dev.vdbroek.nekos.api

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.models.*
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay

object NekosUserState {
    var end by mutableStateOf(false)
    var skip by mutableStateOf(0)
//    var tags = mutableStateListOf<String>()
    var tags = App.defaultTags
}

object UserState {
    var isLoggedIn by mutableStateOf(false)
    var isBusy by mutableStateOf(false)
    var token by mutableStateOf<String?>(null)
    var username by mutableStateOf<String?>(null)

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

    suspend fun register(email: String, username: String, password: String): Response<String?, Exception?> {
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

    suspend fun getUser(id: String): Response<UserResponse?, Exception?> {
        val (_, _, result) = coroutine.async {
            return@async "/user/$id".httpGet()
                .responseString()
        }.await()

        val (data, exception) = result
        return when (result) {
            is Result.Success -> {
                if (data != null) {
                    Response(Gson().fromJson(data, UserResponse::class.java), null)
                } else {
                    Response(null, Exception("[GET_USER]: Invalid response from API"))
                }
            }
            is Result.Failure -> {
                Api.handleException(exception, "GET_USER")
            }
        }
    }

    suspend fun getUploads(uploader: String): Response<NekosResponse?, Exception?> {
        if (NekosUserState.end) return Response(null, EndException("You have reached the end"))

        // Remove all images with tags that could potentially show sexually suggestive images
        val tags = NekosUserState.tags.map {
            if (it.startsWith("-")) {
                val tag = it.replaceFirst("-", "")
                "-\"$tag\""
            } else {
                "\"$it\""
            }
        }

        val bodyData = Nekos.ImagesBody(
//            nsfw = true,
            tags = tags,
            skip = NekosUserState.skip,
            sort = "newest",
            uploader = uploader
        )

        val (_, _, result) = coroutine.async {
            return@async "/images/search".httpPost()
                .header(mapOf("Content-Type" to "application/json"))
                .body(Gson().toJson(bodyData))
                .responseString()
        }.await()

        val (data, exception) = result
        when (result) {
            is Result.Success -> {
                NekosUserState.skip += 30

                if (data != null) {
                    val nekosResponse = Gson().fromJson(data, NekosResponse::class.java)
                    println(nekosResponse)

                    if (nekosResponse.images.size == 0) {
                        NekosUserState.end = true
                        return Response(null, EndException("You have reached the end"))
                    }

                    // We request 30 images if the amount of images returned is less we've reached the end
                    // but we don't need to return since the response still has some images
                    if (nekosResponse.images.size < 30) {
                        NekosUserState.end = true
                    }

                    return Response(nekosResponse, null)
                }
                return Response(null, Exception("No data returned"))
            }
            is Result.Failure -> {
                if (exception != null) {
                    val httpException: HttpException? = try {
                        Gson().fromJson(exception.response.responseMessage, HttpException::class.java)
                    } catch (e: Exception) {
                        null
                    }

                    return Response(null, if (httpException != null) ApiException(httpException) else exception)
                }
                return Response(null, Exception("No data returned"))
            }
        }
    }
}
