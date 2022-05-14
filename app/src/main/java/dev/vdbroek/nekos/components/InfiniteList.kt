package dev.vdbroek.nekos.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.theme.ColorUI
import dev.vdbroek.nekos.ui.theme.imageShape
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun InfiniteList(
    listItems: List<ImageData>,
    navController: NavController,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyGridState()

    LazyVerticalGrid(
        modifier = Modifier
            .fillMaxWidth(),
        state = listState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 16.dp,
            end = 12.dp,
            bottom = 16.dp
        )
    ) {
        items(listItems.size) { i ->
            ListItem(data = listItems[i]) {
                navController.navigate(Screens.ImageDetails.route + "/${it.image}")
            }
        }
    }

    InfiniteListHandler(listState = listState) {
        onLoadMore()
    }
}

@Composable
fun InfiniteListHandler(
    listState: LazyGridState,
    buffer: Int = 2,
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
                onLoadMore()
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListItem(data: ImageData, onNatureClicked: (ImageData) -> Unit) {
    Card(
        modifier = Modifier
            .padding(10.dp)
            .size(160.dp)
            .shadow(3.dp, imageShape, true, ColorUI.dark)
            .clip(imageShape)
//            .background(color = Color.Transparent)
//            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), imageShape)
            .clickable(onClick = { onNatureClicked(data) }),
        shape = imageShape
    ) {
        NetworkImage(
            url = "https://nekos.moe/thumbnail/${data.image}",
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            contentDescription = "Image"
        )
    }
}
