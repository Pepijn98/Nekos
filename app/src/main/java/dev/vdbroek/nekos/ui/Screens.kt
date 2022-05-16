package dev.vdbroek.nekos.ui

sealed class Screens(val route: String) {
    object Home : Screens("home")
    object ImageDetails : Screens("image_details/data={data}")
    object Login : Screens("login")
    object Profile : Screens("profile")
    object Register : Screens("register")
}
