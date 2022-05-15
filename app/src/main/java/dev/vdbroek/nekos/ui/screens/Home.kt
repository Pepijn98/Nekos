package dev.vdbroek.nekos.ui.screens

import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.api.Nekos
import dev.vdbroek.nekos.components.InfiniteList
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.models.EndException
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.screenTitle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val images = mutableStateListOf<Neko>()

@Composable
fun Home(state: ScaffoldState, navController: NavHostController) {
    screenTitle = "Home"

    val scope = CoroutineScope(Dispatchers.Default)

    InfiniteList(items = images, navController = navController) {
        scope.launch {
            val (response, exception) = Nekos.getImages()
            when {
                response != null -> images.addAll(response.images)
                exception != null && exception is EndException -> {
                    state.snackbarHostState.showCustomSnackbar(
                        message = exception.message,
                        actionLabel = "X",
                        snackbarType = SnackbarType.INFO,
                        duration = SnackbarDuration.Short
                    )
                }
                exception != null -> {
                    state.snackbarHostState.showCustomSnackbar(
                        message = exception.message ?: "Failed to fetch more images",
                        actionLabel = "X",
                        snackbarType = SnackbarType.DANGER,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }
}
