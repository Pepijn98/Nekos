package dev.vdbroek.nekos.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.api.UserState
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.utils.App
import kotlinx.coroutines.launch
import me.onebone.toolbar.ExperimentalToolbarApi

@OptIn(ExperimentalToolbarApi::class)
@Composable
fun NekosNavBar(
    navController: NavHostController,
    currentRoute: String?
) {
    val coroutine = rememberCoroutineScope()

    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screens.Home.route,
            label = {
                Text(text = "Home")
            },
            icon = {
                Icon(
                    imageVector = if (currentRoute == Screens.Home.route) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            onClick = {
                if (currentRoute != Screens.Home.route) {
                    navController.backQueue.clear()
                    navController.navigate(Screens.Home.route)
                } else {
                    coroutine.launch {
                        when (InfiniteListState.scrollState) {
                            is LazyListState -> {
                                val state = (InfiniteListState.scrollState as LazyListState)
                                val firstVisibleItemIndex by derivedStateOf { state.firstVisibleItemIndex }

                                if (firstVisibleItemIndex > 1) {
                                    state.scrollToItem(0)
                                    App.globalToolbarState?.expand()
                                }
                            }
                            is LazyGridState -> {
                                val state = (InfiniteListState.scrollState as LazyGridState)
                                val firstVisibleItemIndex by derivedStateOf { state.firstVisibleItemIndex }

                                if (firstVisibleItemIndex > 1) {
                                    state.scrollToItem(0)
                                    App.globalToolbarState?.expand()
                                }
                            }
                        }
                    }
                }
            }
        )
        if (UserState.isLoggedIn) {
            NavigationBarItem(
                selected = currentRoute == Screens.Profile.route,
                label = {
                    Text(text = "Profile")
                },
                icon = {
                    Icon(
                        imageVector = if (currentRoute == Screens.Profile.route) Icons.Filled.Person else Icons.Outlined.Person,
                        contentDescription = "Profile"
                    )
                },
                onClick = {
                    if (currentRoute != Screens.Profile.route) {
                        navController.navigate(Screens.Profile.route)
                    }
                }
            )
        } else {
            NavigationBarItem(
                selected = false,
                label = {
                    Text(text = "Login")
                },
                icon = {
                    Icon(
                        imageVector = if (currentRoute == Screens.Login.route) Icons.Filled.Login else Icons.Outlined.Login,
                        contentDescription = "Login"
                    )
                },
                onClick = {
                    navController.navigate(Screens.Login.route)
                }
            )
        }
        NavigationBarItem(
            selected = currentRoute == Screens.Settings.route,
            label = {
                Text(text = "Settings")
            },
            icon = {
                Icon(
                    imageVector = if (currentRoute == Screens.Settings.route) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            onClick = {
                navController.navigate(Screens.Settings.route)
            }
        )
    }
}
