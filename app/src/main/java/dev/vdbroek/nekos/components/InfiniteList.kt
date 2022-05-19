package dev.vdbroek.nekos.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
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
import dev.vdbroek.nekos.ui.theme.ColorUI
import dev.vdbroek.nekos.ui.theme.imageShape
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun InfiniteList(
    items: SnapshotStateList<Neko>,
    navController: NavController,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    LazyStaggeredGrid(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        cells = StaggeredCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 16.dp,
            end = 12.dp,
            bottom = 16.dp
        )
    ) {
        items(items.size) { i ->
            ListItem(data = items[i]) {
                val jsonData = Gson().toJson(items[i])
                navController.navigate(Screens.ImageDetails.route.replace("{data}", jsonData))
            }
        }
    }

    InfiniteListHandler(listState = listState) {
        onLoadMore()
    }
}

@Composable
fun InfiniteListHandler(
    listState: LazyListState,
    buffer: Int = 10,
    onLoadMore: () -> Unit
) {
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
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
            .padding(10.dp)
            .width(160.dp)
            .shadow(3.dp, imageShape, true, ColorUI.dark)
            .clip(imageShape)
            // .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), imageShape)
            .clickable(onClick = { onItemClicked(data) }),
        shape = imageShape
    ) {
        NetworkImage(
            url = data.getThumbnailUrl(),
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.Center,
            contentScale = ContentScale.FillWidth,
            contentDescription = "Image"
        )
    }
}
