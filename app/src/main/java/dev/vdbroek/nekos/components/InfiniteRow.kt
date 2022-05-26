package dev.vdbroek.nekos.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.theme.NekoColors
import dev.vdbroek.nekos.ui.theme.imageShape
import kotlinx.coroutines.flow.distinctUntilChanged
import java.net.URLEncoder

@Composable
fun InfiniteRow(
    items: SnapshotStateList<Neko>,
    navController: NavController,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(
            start = 6.dp,
            top = 8.dp,
            end = 6.dp,
            bottom = 8.dp
        )
    ) {
        items(items.size) { i ->
            ListItem(data = items[i]) {
                val jsonData = Gson().toJson(it)
                // We HAVE to urlencode the json since there's a tag that contains a forward slash (":/") which breaks the navigation routing obviously
                val encoded = URLEncoder.encode(jsonData, "utf-8")
                navController.navigate(Screens.Post.route.replace("{data}", encoded))
            }
        }
    }

    InfiniteRowHandler(scrollableState = listState) {
        onLoadMore()
    }
}

@Composable
fun InfiniteRowHandler(
    scrollableState: LazyListState,
    buffer: Int = 1,
    onLoadMore: () -> Unit
) {
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = scrollableState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(loadMore) {
        snapshotFlow { loadMore.value }
            .distinctUntilChanged()
            .collect {
                // Only load more when true
                if (it) onLoadMore()
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListItem(data: Neko, onItemClicked: (Neko) -> Unit) {
    Card(
        modifier = Modifier
            .padding(6.dp)
            .shadow(3.dp, imageShape, true, NekoColors.dark)
            .clip(imageShape)
            .clickable(onClick = { onItemClicked(data) }),
        shape = imageShape
    ) {
        NetworkImage(
            url = data.getThumbnailUrl(),
            modifier = Modifier
                .size(180.dp),
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            contentDescription = "Image"
        )
    }
}
