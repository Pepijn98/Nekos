package dev.vdbroek.nekos.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import dev.vdbroek.nekos.models.*
import dev.vdbroek.nekos.utils.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

object Nekos {
    private val coroutine = CoroutineScope(Dispatchers.IO)

    object State {
        var end by mutableStateOf(false)
        var skip by mutableStateOf(0)
        var sort by mutableStateOf("newest")
        var tags = mutableStateListOf(
            "-bare shoulders",
            "-bikini",
            "-crop top",
            "-swimsuit",
            "-midriff",
            "-no bra",
            "-panties",
            "-covered nipples",
            "-from behind",
            "-knees up",
            "-leotard",
            "-black bikini top",
            "-black bikini bottom",
            "-off-shoulder shirt",
            "-naked shirt"
        )
    }

    suspend fun getImages(): Response<NekosResponse?, Exception?> {
        if (State.end) return Response(null, EndException("You have reached the end"))

        // Remove all images with tags that could potentially show sexually suggestive images
        val tags = State.tags.joinToString(", ") {
            if (it.startsWith("-")) {
                val tag = it.replaceFirst("-", "")
                "-\\\"$tag\\\""
            } else {
                "\\\"$it\\\""
            }
        }

        val (_, _, result) = coroutine.async {
            return@async "/images/search".httpPost()
                .header(mapOf("Content-Type" to "application/json"))
                .body("{\"nsfw\": false, \"tags\": \"$tags\", \"limit\": 30, \"skip\": ${State.skip}, \"sort\": \"${State.sort}\"}")
                .responseString()
        }.await()

        val (data, exception) = result
        when (result) {
            is Result.Success -> {
                State.skip += 30

                if (data != null) {
                    val nekosResponse = Gson().fromJson(data, NekosResponse::class.java)
                    if (nekosResponse.images.size == 0) {
                        State.end = true
                        return Response(null, EndException("You have reached the end"))
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
