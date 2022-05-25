package dev.vdbroek.nekos.api

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.models.*
import dev.vdbroek.nekos.ui.screens.HomeScreenState
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

object NekosRequestState {
    var end by mutableStateOf(false)
    var skip by mutableStateOf(0)
    //    var tags = mutableStateListOf<String>()
    var tags = App.defaultTags.toMutableStateList()
    var sort by Delegates.observable("newest") { _, _, _ ->
        // Clear current loaded images and request new ones with the updated sorting option
        val coroutine = CoroutineScope(Dispatchers.Default)
        coroutine.launch {
            val (response, exception) = Nekos.getImages()
            when {
                response != null -> {
                    HomeScreenState.images.apply {
                        clear()
                        addAll(response.images.filter { !it.tags.contains(App.buggedTag) })
                    }
                }
                exception != null && exception is EndException -> {
                    App.snackbarHost.showCustomSnackbar(
                        message = exception.message,
                        actionLabel = "x",
                        withDismissAction = true,
                        snackbarType = SnackbarType.INFO,
                        duration = SnackbarDuration.Short
                    )
                }
                exception != null -> {
                    App.snackbarHost.showCustomSnackbar(
                        message = exception.message ?: "Failed to fetch more images",
                        actionLabel = "x",
                        withDismissAction = true,
                        snackbarType = SnackbarType.DANGER,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }
}

object Nekos : Api() {
    data class ImageSearchBody(
        val nsfw: Boolean = false,
        val tags: List<String>,
        val limit: Int = 30,
        val skip: Int,
        val sort: String,
        val uploader: String? = null
    )

    suspend fun getImages(): Response<NekosResponse?, Exception?> {
        if (NekosRequestState.end) return Response(null, EndException("You have reached the end"))

        // Remove all images with tags that could potentially show sexually suggestive images
        val tags = NekosRequestState.tags.map {
            if (it.startsWith("-")) {
                val tag = it.replaceFirst("-", "")
                "-\"$tag\""
            } else {
                "\"$it\""
            }
        }

        val bodyData = ImageSearchBody(
//            nsfw = true,
            tags = tags,
            skip = NekosRequestState.skip,
            sort = NekosRequestState.sort
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
                NekosRequestState.skip += 30

                if (data != null) {
                    val nekosResponse = Gson().fromJson(data, NekosResponse::class.java)
                    if (nekosResponse.images.size == 0) {
                        NekosRequestState.end = true
                        return Response(null, EndException("You have reached the end"))
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

    suspend fun getTags(): Response<TagsResponse?, Exception?> {
        val (_, response, result) = coroutine.async {
            return@async "/tags".httpGet()
                .header(mapOf("Content-Type" to "application/json"))
                .responseString()
        }.await()

        val (data, exception) = result
        return when (result) {
            is Result.Success -> {
                if (data != null || response.statusCode == 201) {
                    Response(Gson().fromJson(data, TagsResponse::class.java), null)
                } else {
                    Response(null, Exception("[GET_TAGS]: Invalid response from API"))
                }
            }
            is Result.Failure -> {
                handleException(exception, "GET_TAGS")
            }
        }
    }
}
