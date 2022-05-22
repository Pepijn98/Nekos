package dev.vdbroek.nekos.ui.screens

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.api.Nekos
import dev.vdbroek.nekos.components.InfiniteList
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.models.EndException
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.utils.App
import kotlinx.coroutines.launch

val images = mutableStateListOf<Neko>()

@Composable
fun Home(snackbarHost: SnackbarHostState, navController: NavHostController) {
    App.screenTitle = "Posts"

    val coroutine = rememberCoroutineScope()

    InfiniteList(
        items = images,
        navController = navController,
        cells = 2
    ) {
        coroutine.launch {
            val (response, exception) = Nekos.getImages()
            when {
                response != null -> images.addAll(response.images)
                exception != null && exception is EndException -> {
                    snackbarHost.showCustomSnackbar(
                        message = exception.message,
                        actionLabel = "X",
                        withDismissAction = true,
                        snackbarType = SnackbarType.INFO,
                        duration = SnackbarDuration.Short
                    )
                }
                exception != null -> {
                    snackbarHost.showCustomSnackbar(
                        message = exception.message ?: "Failed to fetch more images",
                        actionLabel = "X",
                        withDismissAction = true,
                        snackbarType = SnackbarType.DANGER,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }
}
