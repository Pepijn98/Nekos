package dev.vdbroek.nekos.api

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import dev.vdbroek.nekos.models.ApiException
import dev.vdbroek.nekos.models.HttpException
import dev.vdbroek.nekos.models.LoginResponse
import dev.vdbroek.nekos.models.UserResponse
import dev.vdbroek.nekos.utils.Response
import dev.vdbroek.nekos.utils.UserState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

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
                    Response(null, Exception("No data returned"))
                }
            }
            is Result.Failure -> {
                Api.handleException(exception)
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
                    Response(null, Exception("No data returned"))
                }
            }
            is Result.Failure -> {
                Api.handleException(exception)
            }
        }
    }

    suspend fun getMe(userState: UserState): Response<UserResponse?, Exception?> {
        val (_, _, result) = coroutine.async {
            return@async "/user/@me".httpGet()
                .header(mapOf("Authorization" to userState.token))
                .responseString()
        }.await()

        val (data, exception) = result
        return when (result) {
            is Result.Success -> {
                if (data != null) {
                    Response(Gson().fromJson(data, UserResponse::class.java), null)
                } else {
                    Response(null, Exception("No data returned"))
                }
            }
            is Result.Failure -> {
                Api.handleException(exception)
            }
        }
    }
}
