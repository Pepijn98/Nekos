package dev.vdbroek.nekos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.components.NetworkImage
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.screenTitle
import dev.vdbroek.nekos.ui.theme.imageShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetails(data: Neko) {
    screenTitle = "Image"

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        Card(
            modifier = Modifier
                .padding(10.dp)
                .clip(imageShape)
                .background(color = Color.Transparent),
            shape = imageShape
        ) {
            NetworkImage(
                url = data.getImageUrl(),
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.Center,
                contentScale = ContentScale.Crop,
                contentDescription = "Image"
            )
        }
    }
}
