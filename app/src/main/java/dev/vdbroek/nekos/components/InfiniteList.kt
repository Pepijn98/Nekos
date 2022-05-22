package dev.vdbroek.nekos.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import com.nesyou.staggeredgrid.LazyStaggeredGrid
import com.nesyou.staggeredgrid.StaggeredCells
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.theme.NekoColors
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.ui.theme.imageShape
import kotlinx.coroutines.flow.distinctUntilChanged
import java.net.URLEncoder

@Composable
fun InfiniteList(
    items: SnapshotStateList<Neko>,
    navController: NavController,
    onLoadMore: () -> Unit
) {
    val scrollableState: ScrollableState

    if (ThemeState.staggered) {
        scrollableState = rememberLazyListState()
        StaggeredItems(
            listState = scrollableState,
            items = items,
            navController = navController
        )
    } else {
        scrollableState = rememberLazyGridState()
        FixedItems(
            gridState = scrollableState,
            items = items,
            navController = navController
        )
    }

    InfiniteListHandler(scrollableState = scrollableState) {
        onLoadMore()
    }
}

@Composable
fun StaggeredItems(
    listState: LazyListState,
    items: SnapshotStateList<Neko>,
    navController: NavController
) {
    LazyStaggeredGrid(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        cells = StaggeredCells.Fixed(2),
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
                navController.navigate(Screens.ImageDetails.route.replace("{data}", encoded))
            }
        }
    }
}

@Composable
fun FixedItems(
    gridState: LazyGridState,
    items: SnapshotStateList<Neko>,
    navController: NavController
) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxWidth(),
        state = gridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 16.dp,
            end = 12.dp,
            bottom = 16.dp
        )
    ) {
        items(items.size) { i ->
            ListItem(data = items[i]) {
                val jsonData = Gson().toJson(it)
                // We HAVE to urlencode the json since there's a tag that contains a forward slash (":/") which breaks the navigation routing obviously
                val encoded = URLEncoder.encode(jsonData, "utf-8")
                navController.navigate(Screens.ImageDetails.route.replace("{data}", encoded))
            }
        }
    }
}

@Composable
fun InfiniteListHandler(
    scrollableState: ScrollableState,
    buffer: Int = 10,
    onLoadMore: () -> Unit
) {
    // InfiniteListHandler should only accept LazyListState and LazyGridState
    check(scrollableState is LazyListState || scrollableState is LazyGridState) {
        "InfiniteListHandler state has to be either LazyListState or LazyGridState"
    }

    val loadMore = remember {
        derivedStateOf {
            val totalItemsNumber: Int
            val lastVisibleItemIndex: Int
            when (scrollableState) {
                is LazyListState -> {
                    val layoutInfo = scrollableState.layoutInfo
                    totalItemsNumber = layoutInfo.totalItemsCount
                    lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
                }
                is LazyGridState -> {
                    val layoutInfo = scrollableState.layoutInfo
                    totalItemsNumber = layoutInfo.totalItemsCount
                    lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
                }
                else -> throw IllegalStateException("InfiniteListHandler state has to be either LazyListState or LazyGridState")
            }

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
            modifier = if (ThemeState.staggered) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(180.dp),
            alignment = Alignment.Center,
            contentScale = if (ThemeState.staggered) ContentScale.FillWidth else ContentScale.Crop,
            contentDescription = "Image"
        )
    }
}
