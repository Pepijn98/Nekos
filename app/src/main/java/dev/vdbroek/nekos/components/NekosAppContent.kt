package dev.vdbroek.nekos.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.gson.Gson
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.screens.*
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.EnterAnimation
import dev.vdbroek.nekos.utils.LocalNavigation
import dev.vdbroek.nekos.utils.LocalScreen

private val noNavBar = listOf(
    Screens.Login.route,
    Screens.Register.route,
    Screens.Post.route
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NekosAppContent() {
    val navController = LocalNavigation.current
    val current by LocalScreen.current

    Scaffold(
        contentColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!noNavBar.contains(current)) {
                NekosNavBar()
            }
        },
        snackbarHost = {
            Alert(
                onDismiss = {
                    App.snackbarHost.isActive = false
                    App.snackbarHost.currentSnackbarData?.dismiss()
                }
            )
        },
    ) { padding ->
        NavHost(
            modifier = Modifier
                .padding(padding),
            navController = navController,
            startDestination = Screens.Home.route
        ) {
            composable(Screens.Home.route) {
                EnterAnimation {
                    HomeRefresh {
                        NekosAppBar {
                            Home()
                        }
                    }
                }
            }

            composable(Screens.Post.route) {
                val jsonData = it.arguments?.getString("data")
                val data = Gson().fromJson(jsonData, Neko::class.java)
                EnterAnimation {
                    NekosAppBar {
                        Post(data)
                    }
                }
            }

            composable(Screens.Settings.route) {
                EnterAnimation {
                    NekosAppBar { toolbar ->
                        Settings(toolbar)
                    }
                }
            }

            // -BEGIN: PROFILE FLOW
            composable(Screens.Login.route) {
                EnterAnimation {
                    Login()
                }
            }

            composable(Screens.Register.route) {
                EnterAnimation {
                    Register()
                }
            }

            composable(Screens.Profile.route) {
                EnterAnimation {
                    NekosAppBar { toolbar ->
                        Profile(toolbar)
                    }
                }
            }

            composable(Screens.User.route) {
                val id = it.arguments?.getString("id") ?: return@composable
                EnterAnimation {
                    NekosAppBar { toolbar ->
                        User(id, toolbar)
                    }
                }
            }
            // -END: PROFILE FLOW
        }
    }
}
