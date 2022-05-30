package dev.vdbroek.nekos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.api.User
import dev.vdbroek.nekos.api.UserRequestState
import dev.vdbroek.nekos.components.InfiniteRow
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.models.EndException
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.models.UserData
import dev.vdbroek.nekos.ui.theme.NekoColors
import dev.vdbroek.nekos.ui.theme.imageShape
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.LocalNavigation
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarState
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

object UserScreenState {
    val uploaderImages = mutableStateListOf<Neko>()
    var initialRequest by mutableStateOf(true)
    var user by mutableStateOf<UserData?>(null)
}

@Composable
fun User(
    id: String,
    toolbarState: CollapsingToolbarState
) {
    App.screenTitle = ""

    val navigation = LocalNavigation.current
    val coroutine = rememberCoroutineScope()

    BackHandler {
        navigation.popBackStack()

        UserRequestState.apply {
            end = false
            skip = 0
            tags = App.defaultTags.toMutableStateList()
        }

        UserScreenState.apply {
            uploaderImages.clear()
            initialRequest = true
            user = null
        }
    }

    suspend fun getUserUploads(
        username: String
    ) {
        val (response, exception) = User.getUploads(username)
        when {
            response != null -> UserScreenState.uploaderImages.addAll(response.images.filter { !it.tags.contains(App.buggedTag) })
            exception != null && exception is EndException -> return
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

    LaunchedEffect(key1 = true) {
        if (UserScreenState.initialRequest) {
            val (userData, userException) = User.getUser(id)
            when {
                userData != null -> {
                    UserScreenState.user = userData.user
                    getUserUploads(userData.user.username)
                    UserScreenState.initialRequest = false
                }
                userException != null -> {
                    App.snackbarHost.showCustomSnackbar(
                        message = userException.message ?: "Could not retrieve user data",
                        actionLabel = "x",
                        withDismissAction = true,
                        snackbarType = SnackbarType.DANGER
                    )
                }
            }
        }
    }

    Column(
        Modifier
            .scrollable(
                state = toolbarState,
                orientation = Orientation.Vertical
            )
    ) {
        if (UserScreenState.user != null) {
            App.screenTitle = UserScreenState.user!!.username

            val dateTime = UserScreenState.user!!.createdAt.split("T")[0]
            val from = LocalDate.parse(dateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val today = LocalDate.now()
            val period = Period.between(from, today)

            Column(
                modifier = Modifier
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp)
            ) {
                Image(
                    modifier = Modifier
                        .size(250.dp)
                        .clip(imageShape),
                    painter = painterResource(id = R.drawable.profile_placeholder),
                    contentDescription = "Avatar"
                )
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 8.dp),
                        imageVector = Icons.Filled.ThumbUp,
                        tint = NekoColors.like,
                        contentDescription = "Thumb up icon"
                    )
                    Text(
                        text = "${UserScreenState.user!!.likesReceived} Likes",
                        color = NekoColors.like,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 16.dp)
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 8.dp),
                        imageVector = Icons.Filled.Favorite,
                        tint = NekoColors.favorite,
                        contentDescription = "Favorite icon"
                    )
                    Text(
                        text = "${UserScreenState.user!!.favoritesReceived} Favorites",
                        color = NekoColors.favorite,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 8.dp),
                    text = "Joined ${period.years} years ago",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 8.dp),
                    text = "Posted ${UserScreenState.user!!.uploads} images",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 8.dp),
                    text = "Has given ${UserScreenState.user!!.likes.size} likes and ${UserScreenState.user!!.favorites.size} favorites",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 8.dp),
                    text = "Uploads",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.headlineSmall
                )
                Divider(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
            InfiniteRow(
                items = UserScreenState.uploaderImages
            ) {
                coroutine.launch {
                    if (!UserScreenState.initialRequest)
                        getUserUploads(UserScreenState.user!!.username)
                }
            }
        }
    }
}
