package dev.vdbroek.nekos.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.components.ImageData
import dev.vdbroek.nekos.components.InfiniteList
import dev.vdbroek.nekos.screenTitle

@Composable
fun Home(navController: NavHostController) {
    screenTitle = "Home"

    val listItems = remember {
        mutableStateListOf(
            ImageData(1, "Gu8dj-_mB"),
            ImageData(2, "oGjsVRjrs"),
            ImageData(3, "GjS-oEacX"),
            ImageData(4, "IaQqVdLOP"),
            ImageData(5, "QBOtRa36Z"),
            ImageData(6, "78qL2Rosw"),
            ImageData(7, "o5xO9hl2s"),
            ImageData(8, "xOzCcCbAx"),
            ImageData(9, "-e27VbESp")
        )
    }

    InfiniteList(listItems = listItems, navController = navController) {
        // TODO : Request next set of images
        listItems += listItems
    }
}
