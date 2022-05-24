package dev.vdbroek.nekos.api

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.models.ApiException
import dev.vdbroek.nekos.models.EndException
import dev.vdbroek.nekos.models.HttpException
import dev.vdbroek.nekos.models.NekosResponse
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

object Nekos {
    private val coroutine = CoroutineScope(Dispatchers.IO)

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
