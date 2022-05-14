package dev.vdbroek.nekos.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import dev.vdbroek.nekos.screenTitle
import dev.vdbroek.nekos.ui.Navigation
import kotlinx.coroutines.flow.distinctUntilChanged

val defaultShape = RoundedCornerShape(10.dp)

data class ImageData(
    val id: Int,
    val image: String
)

val listItems = mutableStateListOf(
    ImageData(1, "https://nekos.moe/image/Gu8dj-_mB"),
    ImageData(2,"https://nekos.moe/image/oGjsVRjrs"),
    ImageData(3, "https://nekos.moe/image/GjS-oEacX"),
    ImageData(4, "https://nekos.moe/image/IaQqVdLOP"),
    ImageData(5, "https://nekos.moe/image/QBOtRa36Z"),
    ImageData(6, "https://nekos.moe/image/78qL2Rosw"),
    ImageData(7, "https://nekos.moe/image/o5xO9hl2s"),
    ImageData(8, "https://nekos.moe/image/xOzCcCbAx"),
    ImageData(9, "https://nekos.moe/image/-e27VbESp")
)

@Composable
fun Home(navController: NavHostController) {
    screenTitle = "Home"

    InfiniteList(listItems = listItems, navController = navController) {
        listItems += listItems
    }
}

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
                navController.navigate(Navigation.Image.route)
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
            .clip(defaultShape)
            .background(color = Color.Transparent)
            .clickable(onClick = { onNatureClicked(data) }),
        shape = defaultShape
    ) {
        Image(
            painter = rememberAsyncImagePainter(data.image),
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            contentDescription = "Image"
        )
    }
}
