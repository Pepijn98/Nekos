package dev.vdbroek.nekos.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.accompanist.flowlayout.FlowRow
import dev.vdbroek.nekos.api.RelationshipType
import dev.vdbroek.nekos.api.User
import dev.vdbroek.nekos.api.UserRequestState
import dev.vdbroek.nekos.api.UserState
import dev.vdbroek.nekos.components.SnackbarType
import dev.vdbroek.nekos.components.ZoomableNetworkImage
import dev.vdbroek.nekos.components.showCustomSnackbar
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.theme.NekoColors
import dev.vdbroek.nekos.ui.theme.imageShape
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.App.saveImageBitmap
import dev.vdbroek.nekos.utils.App.saveImageBitmap29
import dev.vdbroek.nekos.utils.rememberMutableStateOf
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

enum class LoadingState {
    LOADING,
    FAILED,
    SUCCESS,
    NONE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Post(
    navController: NavHostController,
    data: Neko
) {
    App.screenTitle = "Post Info"

    val context = LocalContext.current

    var loadingState by rememberMutableStateOf(LoadingState.NONE)
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    val groupedTags = data.tags.chunked(3)
    val scrollState = rememberLazyListState()
    val coroutine = rememberCoroutineScope()

    var liked by rememberMutableStateOf(false)
    var favorited by rememberMutableStateOf(false)

    var likeCount by rememberMutableStateOf(data.likes)
    var favoriteCount by rememberMutableStateOf(data.favorites)

    if (UserState.isLoggedIn) {
        LaunchedEffect(key1 = true) {
            val (response, exception) = User.getMe()
            when {
                response != null -> {
                    liked = response.user.likes.contains(data.id)
                    favorited = response.user.favorites.contains(data.id)
                }
                exception != null -> {
                    App.snackbarHost.showCustomSnackbar(
                        message = exception.message ?: "Could not retrieve user data",
                        actionLabel = "x",
                        withDismissAction = true,
                        snackbarType = SnackbarType.DANGER
                    )
                }
            }
        }
    }

    LazyColumn(
        state = scrollState
    ) {
        item {
            Card(
                modifier = Modifier
                    .padding(10.dp)
                    .shadow(3.dp, imageShape, true, NekoColors.dark)
                    .clip(imageShape)
                    .background(color = Color.Transparent),
                shape = imageShape
            ) {
                ZoomableNetworkImage(
                    url = data.getImageUrl(),
                    modifier = Modifier
                        .fillMaxWidth(),
                    alignment = Alignment.Center,
                    contentScale = ContentScale.FillWidth,
                    scrollState = scrollState
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp)
            ) {
                // -LIKE
                if (liked) {
                    Button(
                        modifier = Modifier
                            .padding(end = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NekoColors.like,
                            contentColor = Color.White
                        ),
                        onClick = {
                            coroutine.launch {
                                val (success, exception) = User.patchRelationship(data.id, RelationshipType.Like.value, false)
                                when {
                                    success == true -> {
                                        liked = false
                                        likeCount--
                                    }
                                    exception != null -> {
                                        App.snackbarHost.showCustomSnackbar(
                                            message = exception.message ?: "Failed removing like",
                                            actionLabel = "x",
                                            withDismissAction = true,
                                            snackbarType = SnackbarType.DANGER
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(end = 2.dp),
                            imageVector = Icons.Filled.ThumbUp,
                            contentDescription = "Unlike button"
                        )
                        Text(
                            text = "$likeCount Likes",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    OutlinedButton(
                        modifier = Modifier
                            .padding(end = 4.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = NekoColors.like
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NekoColors.like
                        ),
                        onClick = {
                            coroutine.launch {
                                val (success, exception) = User.patchRelationship(data.id, RelationshipType.Like.value, true)
                                when {
                                    success == true -> {
                                        liked = true
                                        likeCount++
                                    }
                                    exception != null -> {
                                        App.snackbarHost.showCustomSnackbar(
                                            message = exception.message ?: "Failed adding like",
                                            actionLabel = "x",
                                            withDismissAction = true,
                                            snackbarType = SnackbarType.DANGER
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(end = 2.dp),
                            imageVector = Icons.Filled.ThumbUp,
                            contentDescription = "Like button"
                        )
                        Text(
                            text = "$likeCount Likes",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // -FAVORITE
                if (favorited) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NekoColors.favorite,
                            contentColor = Color.White
                        ),
                        onClick = {
                            coroutine.launch {
                                val (success, exception) = User.patchRelationship(data.id, RelationshipType.Favorite.value, false)
                                when {
                                    success == true -> {
                                        favorited = false
                                        favoriteCount--
                                    }
                                    exception != null -> {
                                        App.snackbarHost.showCustomSnackbar(
                                            message = exception.message ?: "Failed removing favorite",
                                            actionLabel = "x",
                                            withDismissAction = true,
                                            snackbarType = SnackbarType.DANGER
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(end = 2.dp),
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Unfavorite button"
                        )
                        Text(
                            text = "$favoriteCount Favorites",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    OutlinedButton(
                        border = BorderStroke(
                            width = 1.dp,
                            color = NekoColors.favorite
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NekoColors.favorite
                        ),
                        onClick = {
                            coroutine.launch {
                                val (success, exception) = User.patchRelationship(data.id, RelationshipType.Favorite.value, true)
                                when {
                                    success == true -> {
                                        favorited = true
                                        favoriteCount++
                                    }
                                    exception != null -> {
                                        App.snackbarHost.showCustomSnackbar(
                                            message = exception.message ?: "Failed adding favorite",
                                            actionLabel = "x",
                                            withDismissAction = true,
                                            snackbarType = SnackbarType.DANGER
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(end = 2.dp),
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Favorite button"
                        )
                        Text(
                            text = "$favoriteCount Favorites",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    val modifier = Modifier
                        .graphicsLayer {
                            rotationZ = angle
                        }

                    FilledIconButton(
                        modifier = if (loadingState == LoadingState.LOADING) modifier else Modifier,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = {
                            if (loadingState == LoadingState.NONE && App.permissionGranted) {
                                Glide.with(context)
                                    .asBitmap()
                                    .load(data.getImageUrl())
                                    .into(object : CustomTarget<Bitmap>() {
                                        override fun onLoadCleared(p: Drawable?) {
                                            loadingState = LoadingState.LOADING
                                        }

                                        override fun onLoadFailed(errorDrawable: Drawable?) {
                                            loadingState = LoadingState.FAILED
                                        }

                                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                            try {
                                                val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    context.saveImageBitmap29(resource, data.id)
                                                } else {
                                                    context.saveImageBitmap(resource, data.id)
                                                }

                                                loadingState = if (success) {
                                                    LoadingState.SUCCESS
                                                } else {
                                                    LoadingState.FAILED
                                                }
                                            } catch (e: Exception) {
                                                loadingState = LoadingState.FAILED
                                                coroutine.launch {
                                                    App.snackbarHost.showCustomSnackbar(
                                                        message = e.message ?: "Failed downloading image",
                                                        actionLabel = "x",
                                                        withDismissAction = true,
                                                        snackbarType = SnackbarType.DANGER
                                                    )
                                                }
                                            }
                                        }
                                    })
                            }
                        }
                    ) {
                        when (loadingState) {
                            LoadingState.LOADING -> {
                                Icon(
                                    imageVector = Icons.Filled.Autorenew,
                                    contentDescription = "Downloading image"
                                )
                            }
                            LoadingState.FAILED -> {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Failed downloading image"
                                )
                            }
                            LoadingState.SUCCESS -> {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Success downloading image"
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Filled.Save,
                                    contentDescription = "Save image"
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp)
            ) {
                Text(
                    text = "Uploader:",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .clickable {
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

                            navController.navigate(Screens.User.route.replace("{id}", data.uploader.id))
                        },
                    text = data.uploader.username,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (data.approver != null) {
            item {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 8.dp)
                ) {
                    Text(
                        text = "Approver:",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .clickable {
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

                                navController.navigate(Screens.User.route.replace("{id}", data.approver.id))
                            },
                        text = data.approver.username,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp)
            ) {
                Text(
                    text = "Uploaded:",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    modifier = Modifier
                        .padding(start = 4.dp),
                    text = App.timestamp(data.createdAt),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (data.artist != null) {
            item {
                Row(
                    modifier = Modifier
                        .padding(start = 16.dp, top = 8.dp)
                ) {
                    Text(
                        text = if (data.artist.contains("+")) "Artists:" else "Artist:",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 4.dp),
                        text = data.artist.replace("+", ", "),
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        item {
            Text(
                modifier = Modifier
                    .padding(start = 8.dp, top = 8.dp),
                text = "Tags",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall
            )
            Divider(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        items(groupedTags.size) { i ->
            TagGroup(
                tags = groupedTags[i],
                last = ((groupedTags.size - 1) == i)
            )
        }
    }
}

@Composable
fun TagGroup(
    tags: List<String>,
    last: Boolean = false
) {
    FlowRow(
        modifier = Modifier
            .padding(
                start = 8.dp,
                end = 4.dp,
                bottom = if (last) 6.dp else 0.dp
            )
    ) {
        tags.forEach { tag ->
            val realTag = tag.replace("+", " ")
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    text = realTag,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
