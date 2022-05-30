package dev.vdbroek.nekos.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import dev.vdbroek.nekos.models.EndException
import dev.vdbroek.nekos.models.LoginResponse
import dev.vdbroek.nekos.models.NekosResponse
import dev.vdbroek.nekos.models.UserResponse
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.Response
import kotlinx.coroutines.async
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

sealed class RelationshipType(val value: String) {
    object Like : RelationshipType("like")
    object Favorite : RelationshipType("favorite")
}

object UserRequestState {
    var end by mutableStateOf(false)
    var skip by mutableStateOf(0)
    var tags = App.defaultTags.toMutableStateList()
}

object UserState {
    var isLoggedIn by mutableStateOf(false)
    var token by mutableStateOf<String?>(null)
    var username by mutableStateOf<String?>(null)
}

object User : Api() {

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
                handleException(exception, "AUTH")
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
                handleException(exception, "REGISTER")
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
                handleException(exception, "GET_ME")
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
                handleException(exception, "GET_USER")
            }
        }
    }

    suspend fun getUploads(uploader: String): Response<NekosResponse?, Exception?> {
        if (UserRequestState.end) return Response(null, EndException("You have reached the end"))

        // Remove all images with tags that could potentially show sexually suggestive images
        val tags = UserRequestState.tags.map {
            if (it.startsWith("-")) {
                val tag = it.replaceFirst("-", "")
                "-\"$tag\""
            } else {
                "\"$it\""
            }
        }

        val bodyData = Nekos.ImageSearchBody(
            nsfw = if (App.uncensored && App.nsfw) null else false,
            tags = tags,
            skip = UserRequestState.skip,
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
                UserRequestState.skip += 30

                if (data != null) {
                    val nekosResponse = Gson().fromJson(data, NekosResponse::class.java)

                    if (nekosResponse.images.size == 0) {
                        UserRequestState.end = true
                        return Response(null, EndException("You have reached the end"))
                    }

                    // We request 30 images if the amount of images returned is less we've reached the end
                    // but we don't need to return since the response still has some images
                    if (nekosResponse.images.size < 30) {
                        UserRequestState.end = true
                    }

                    return Response(nekosResponse, null)
                }
                return Response(null, Exception("No data returned"))
            }
            is Result.Failure -> {
                return handleException(exception)
            }
        }
    }

    suspend fun patchRelationship(image: String, type: String, create: Boolean): Response<Boolean?, Exception?> {
        if (!UserState.isLoggedIn || UserState.token.isNullOrBlank()) return Response(null, Exception("[PATCH]: Not logged in"))

        val jsonBody = "{\"type\": \"$type\", \"create\": $create}"
            .toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("${App.baseUrl}/image/$image/relationship")
            .headers(
                Headers.Builder()
                    .add("Authorization", UserState.token!!)
                    .add("User-Agent", App.userAgent)
                    .add("Content-Type", "application/json;charset=utf-8")
                    .build()
            )
            .patch(jsonBody)
            .build()

        @Suppress("BlockingMethodInNonBlockingContext")
        val result = coroutine.async {
            return@async try {
                val response = client.newCall(request)
                    .execute()
                    .also {
                        it.close()
                    }
                if (response.isSuccessful && response.code in 200..204) {
                    Result.success(true)
                } else {
                    Result.error(FuelError.wrap(Exception("Invalid response from API")))
                }
            } catch (e: Exception) {
                Result.error(FuelError.wrap(e))
            }
        }.await()

        val (data, exception) = result
        return when (result) {
            is Result.Success -> {
                Response(data, null)
            }
            is Result.Failure -> {
                handleException(exception, "PATCH")
            }
        }
    }
}
