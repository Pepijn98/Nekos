package dev.vdbroek.nekos.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import dev.vdbroek.nekos.screenTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetails(navController: NavHostController, id: String?) {
    screenTitle = "Image"

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        Card(
            modifier = Modifier
//                .width(250.dp)
                .padding(10.dp)
                .clip(defaultShape)
                .background(color = Color.Transparent),
            shape = defaultShape
        ) {
            Image(
                painter = rememberAsyncImagePainter("https://nekos.moe/image/$id"),
                alignment = Alignment.Center,
                contentScale = ContentScale.FillBounds,
                contentDescription = "Image"
            )
        }
    }
}
