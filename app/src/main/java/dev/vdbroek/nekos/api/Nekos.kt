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

object NekosState {
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

object Nekos {
    private val coroutine = CoroutineScope(Dispatchers.IO)

    data class ImagesBody(
        val nsfw: Boolean = false,
        val tags: List<String>,
        val limit: Int = 30,
        val skip: Int,
        val sort: String,
        val uploader: String? = null
    )

    suspend fun getImages(): Response<NekosResponse?, Exception?> {
        if (NekosState.end) return Response(null, EndException("You have reached the end"))

        // Remove all images with tags that could potentially show sexually suggestive images
        val tags = NekosState.tags.map {
            if (it.startsWith("-")) {
                val tag = it.replaceFirst("-", "")
                "-\"$tag\""
            } else {
                "\"$it\""
            }
        }

        val bodyData = ImagesBody(
            tags = tags,
            skip = NekosState.skip,
            sort = NekosState.sort
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
                NekosState.skip += 30

                if (data != null) {
                    val nekosResponse = Gson().fromJson(data, NekosResponse::class.java)
                    if (nekosResponse.images.size == 0) {
                        NekosState.end = true
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
