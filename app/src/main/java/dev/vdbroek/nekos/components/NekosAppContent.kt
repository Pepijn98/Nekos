package dev.vdbroek.nekos.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.gson.Gson
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.screens.*
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.EnterAnimation

private val noNavBar = listOf(
    Screens.Login.route,
    Screens.Register.route,
    Screens.Post.route
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NekosAppContent(
    navController: NavHostController,
    currentRoute: String?
) {
    Scaffold(
        contentColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!noNavBar.contains(currentRoute)) {
                NekosNavBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
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
            composable(
                route = Screens.Home.route
            ) {
                EnterAnimation {
                    HomeRefresh {
                        NekosAppBar(
                            navController = navController,
                            route = currentRoute
                        ) {
                            Home(
                                navController = navController
                            )
                        }
                    }
                }
            }

            composable(
                route = Screens.Post.route
            ) {
                val jsonData = it.arguments?.getString("data")
                val data = Gson().fromJson(jsonData, Neko::class.java)
                EnterAnimation {
                    NekosAppBar(
                        navController = navController,
                        route = currentRoute
                    ) {
                        Post(
                            navController = navController,
                            data = data
                        )
                    }
                }
            }

            composable(
                route = Screens.Settings.route
            ) {
                EnterAnimation {
                    NekosAppBar(
                        navController = navController,
                        route = currentRoute
                    ) {
                        Settings()
                    }
                }
            }

            // -BEGIN: PROFILE FLOW
            composable(
                route = Screens.Login.route
            ) {
                EnterAnimation {
                    Login(
                        navController = navController
                    )
                }
            }

            composable(
                route = Screens.Register.route
            ) {
                EnterAnimation {
                    Register(
                        navController = navController
                    )
                }
            }

            composable(
                route = Screens.Profile.route
            ) {
                EnterAnimation {
                    NekosAppBar(
                        navController = navController,
                        route = currentRoute
                    ) { toolbarState ->
                        Profile(
                            scrollState = toolbarState,
                            navController = navController
                        )
                    }
                }
            }

            composable(
                route = Screens.User.route
            ) {
                val userID = it.arguments?.getString("id") ?: return@composable
                EnterAnimation {
                    NekosAppBar(
                        navController = navController,
                        route = currentRoute
                    ) { toolbarState ->
                        User(
                            scrollState = toolbarState,
                            navController = navController,
                            id = userID
                        )
                    }
                }
            }
            // -END: PROFILE FLOW
        }
    }
}
