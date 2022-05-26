package dev.vdbroek.nekos.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import dev.vdbroek.nekos.api.Nekos
import dev.vdbroek.nekos.api.NekosRequestState
import dev.vdbroek.nekos.ui.screens.HomeScreenState
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.rememberMutableStateOf
import kotlinx.coroutines.launch

@Composable
fun HomeRefresh(
    content: @Composable () -> Unit
) {
    val coroutine = rememberCoroutineScope()
    val refreshState = rememberMutableStateOf(false)
    val swipeRefreshState = rememberSwipeRefreshState(refreshState.value)

    SwipeRefresh(
        state = swipeRefreshState,
        indicatorPadding = PaddingValues(
            top = 108.dp
        ),
        indicator = { state, trigger ->
            SwipeRefreshIndicator(
                state = state,
                refreshTriggerDistance = trigger,
                scale = true,
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape,
            )
        },
        onRefresh = {
            refreshState.value = true
            NekosRequestState.apply {
                end = false
                skip = 0
            }
            coroutine.launch {
                val (response, exception) = Nekos.getImages()
                when {
                    response != null -> {
                        HomeScreenState.images.apply {
                            clear()
                            addAll(response.images.filter { !it.tags.contains(App.buggedTag) })
                        }
                        refreshState.value = false
                    }
                    exception != null -> {
                        App.snackbarHost.showCustomSnackbar(
                            message = exception.message ?: "Failed to fetch images",
                            actionLabel = "x",
                            withDismissAction = true,
                            snackbarType = SnackbarType.DANGER,
                            duration = SnackbarDuration.Long
                        )
                    }
                }
            }
        },
        content = content
    )
}
