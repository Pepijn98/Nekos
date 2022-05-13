package dev.vdbroek.nekos.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.screenTitle
import dev.vdbroek.nekos.ui.Navigation
import kotlinx.coroutines.flow.distinctUntilChanged

val defaultShape = RoundedCornerShape(10.dp)

data class ImageData(
    val id: Int,
    val image: Int
)

var listItems = mutableStateListOf(
    ImageData(1, R.drawable.nature_1),
    ImageData(2, R.drawable.nature_2),
    ImageData(3, R.drawable.nature_3),
    ImageData(4, R.drawable.nature_4),
    ImageData(5, R.drawable.nature_5),
    ImageData(6, R.drawable.nature_6),
    ImageData(7, R.drawable.nature_7),
    ImageData(8, R.drawable.nature_8),
    ImageData(9, R.drawable.nature_9)
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
        modifier = Modifier.fillMaxWidth(),
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
            painter = painterResource(id = data.image),
            modifier = Modifier.fillMaxSize(),
            alignment = Alignment.Center,
            contentScale = ContentScale.Crop,
            contentDescription = "Image"
        )
    }
}
