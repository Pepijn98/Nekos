@file:OptIn(ExperimentalFoundationApi::class)

package dev.vdbroek.nekos.components

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Card
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.gson.Gson
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.theme.NekoColors
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.ui.theme.imageShape
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.LocalNavigation
import kotlinx.coroutines.flow.distinctUntilChanged
import java.net.URLEncoder

object InfiniteListState {
    lateinit var scrollState: LazyGridState
    lateinit var staggeredScrollState: LazyStaggeredGridState
}

@Composable
fun InfiniteList(
    items: SnapshotStateList<Neko>,
    cells: Int,
    onLoadMore: () -> Unit
) {
    val navigation = LocalNavigation.current

    if (ThemeState.staggered) {
        InfiniteListState.staggeredScrollState = rememberLazyStaggeredGridState()
        StaggeredItems(
            state = InfiniteListState.staggeredScrollState,
            items = items,
            controller = navigation,
            cells = cells
        )
    } else {
        InfiniteListState.scrollState = rememberLazyGridState()
        FixedItems(
            state = InfiniteListState.scrollState,
            items = items,
            controller = navigation,
            cells = cells
        )
    }

    InfiniteListHandler(state = if (ThemeState.staggered) InfiniteListState.staggeredScrollState else InfiniteListState.scrollState) {
        onLoadMore()
    }
}

@Composable
fun StaggeredItems(
    state: LazyStaggeredGridState,
    items: SnapshotStateList<Neko>,
    controller: NavHostController,
    cells: Int
) {
    LazyVerticalStaggeredGrid(
        modifier = Modifier.fillMaxWidth(),
        state = state,
        columns = StaggeredGridCells.Fixed(cells),
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
                controller.navigate(Screens.Post.route.replace("{data}", encoded))
            }
        }
    }
}

@Composable
fun FixedItems(
    state: LazyGridState,
    items: SnapshotStateList<Neko>,
    controller: NavHostController,
    cells: Int
) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxWidth(),
        state = state,
        //flingBehavior = App.flingBehavior(),
        columns = GridCells.Fixed(cells),
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
                controller.navigate(Screens.Post.route.replace("{data}", encoded))
            }
        }
    }
}

@Composable
fun InfiniteListHandler(
    state: ScrollableState,
    buffer: Int = 10,
    onLoadMore: () -> Unit
) {
    // InfiniteListHandler should only accept LazyListState and LazyGridState
    check(state is LazyGridState || state is LazyStaggeredGridState) {
        "InfiniteListHandler state has to be either LazyGridState or LazyStaggeredGridState"
    }

    val loadMore = remember {
        derivedStateOf {
            val totalItemsNumber: Int
            val lastVisibleItemIndex: Int
            when (state) {
                is LazyStaggeredGridState -> {
                    val layoutInfo = state.layoutInfo
                    totalItemsNumber = layoutInfo.totalItemsCount
                    lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
                }
                is LazyGridState -> {
                    val layoutInfo = state.layoutInfo
                    totalItemsNumber = layoutInfo.totalItemsCount
                    lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
                }
                else -> throw IllegalStateException("InfiniteListHandler state has to be either LazyGridState or LazyStaggeredGridState")
            }

            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(loadMore) {
        snapshotFlow { loadMore.value }
            .distinctUntilChanged()
            .collect {
                // Only load more when true
                if (!App.initialLoad && App.isReady && it) {
                    Log.e("LOAD_MORE", "$it")
                    onLoadMore()
                }
            }
    }
}

//@Composable
//fun InfiniteListHandler(
//    state: LazyGridState,
//    buffer: Int = 10,
//    onLoadMore: () -> Unit
//) {
//    val loadMore = remember {
//        derivedStateOf {
//            val totalItemsNumber: Int
//            val lastVisibleItemIndex: Int
//            val layoutInfo = state.layoutInfo
//            totalItemsNumber = layoutInfo.totalItemsCount
//            lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
//
//            lastVisibleItemIndex > (totalItemsNumber - buffer)
//        }
//    }
//
//    LaunchedEffect(loadMore) {
//        snapshotFlow { loadMore.value }
//            .distinctUntilChanged()
//            .collect {
//                // Only load more when true
//                if (!App.initialLoad && App.isReady && it) {
//                    Log.e("LOAD_MORE", "$it")
//                    onLoadMore()
//                }
//            }
//    }
//}
//
//@Composable
//fun InfiniteStaggeredListHandler(
//    state: LazyStaggeredGridState,
//    buffer: Int = 10,
//    onLoadMore: () -> Unit
//) {
//    val loadMore = remember {
//        derivedStateOf {
//            val layoutInfo = state.layoutInfo
//            val totalItemsNumber = layoutInfo.totalItemsCount
//            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
//
//            lastVisibleItemIndex > (totalItemsNumber - buffer)
//        }
//    }
//
//    LaunchedEffect(loadMore) {
//        snapshotFlow { loadMore.value }
//            .distinctUntilChanged()
//            .collect {
//                // Only load more when true
//                if (!App.initialLoad && App.isReady && it) {
//                    Log.e("LOAD_MORE", "$it")
//                    onLoadMore()
//                }
//            }
//    }
//}

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
            modifier = if (ThemeState.staggered) Modifier.fillMaxSize() else Modifier
                .fillMaxWidth()
                .height(180.dp),
            alignment = Alignment.Center,
            contentScale = if (ThemeState.staggered) ContentScale.FillWidth else ContentScale.Crop,
            contentDescription = "Image"
        )
    }
}
