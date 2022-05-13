package dev.vdbroek.nekos.ui

sealed class Navigation(val route: String) {
    object Splash : Navigation("splash")
    object Home : Navigation("home")
    object Profile : Navigation("profile")
    object Image : Navigation("image")
}
