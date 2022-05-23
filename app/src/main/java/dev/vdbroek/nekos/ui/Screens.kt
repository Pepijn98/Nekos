package dev.vdbroek.nekos.ui

sealed class Screens(val route: String) {
    object Home : Screens("home")
    object PostInfo : Screens("post_info/data={data}")
    object Settings : Screens("settings")
    object Login : Screens("login")
    object Register : Screens("register")
    object Profile : Screens("profile")
    object User : Screens("user/{id}")
}
