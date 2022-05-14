package dev.vdbroek.nekos.ui

sealed class Screens(val route: String) {
    object Home : Screens("home")
    object Profile : Screens("profile")
    object ImageDetails : Screens("image_details")
}
