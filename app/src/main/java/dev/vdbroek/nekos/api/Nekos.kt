package dev.vdbroek.nekos.api

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import dev.vdbroek.nekos.models.NekosResponse
import dev.vdbroek.nekos.utils.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

object Nekos {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    object State {
        var skip by mutableStateOf(0)
        var isNew by mutableStateOf(true)
        var init by mutableStateOf(true)
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
        // Remove all images with tags that could potentially show sexually suggestive images
        val tags = State.tags.joinToString(", ") {
            if (it.startsWith("-"))  {
                val tag = it.replaceFirst("-", "")
                "-\\\"$tag\\\""
            } else {
                "\\\"$it\\\""
            }
        }

        val result = coroutineScope.async {
            val (_, _, result) = "/images/search".httpPost()
                .header(mapOf("Content-Type" to "application/json"))
                .body("{\"nsfw\": false, \"tags\": \"$tags\", \"limit\": 30, \"skip\": ${State.skip}, \"sort\": \"${State.sort}\"}")
                .responseString()

            return@async result
        }.await()

        val (data, exception) = result
        when (result) {
            is Result.Success -> {
                State.skip += 30
                State.init = false
                State.isNew = false

                if (data != null) {
                    return Response(Gson().fromJson(data, NekosResponse::class.java), null)
                }
                return Response(null, Exception("No data returned"))
            }
            is Result.Failure -> {
                if (exception != null) {
                    return Response(null, exception)
                }
                return Response(null, Exception("No data returned"))
            }
        }
    }
}
