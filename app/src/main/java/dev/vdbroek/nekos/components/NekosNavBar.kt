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
import androidx.compose.runtime.*
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.vdbroek.nekos.api.UserState
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.LocalNavigation
import dev.vdbroek.nekos.utils.LocalScreen
import kotlinx.coroutines.launch
import me.onebone.toolbar.ExperimentalToolbarApi

@OptIn(ExperimentalToolbarApi::class)
@Composable
fun NekosNavBar() {
    val navController = LocalNavigation.current
    val screen by LocalScreen.current

    val coroutine = rememberCoroutineScope()

    NavigationBar {
        NavigationBarItem(
            selected = screen == Screens.Home.route,
            label = {
                Text(text = "Home")
            },
            icon = {
                Icon(
                    imageVector = if (screen == Screens.Home.route) Icons.Filled.Home else Icons.Outlined.Home,
                    contentDescription = "Home"
                )
            },
            onClick = {
                if (screen != Screens.Home.route) {
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
                selected = screen == Screens.Profile.route,
                label = {
                    Text(text = "Profile")
                },
                icon = {
                    Icon(
                        imageVector = if (screen == Screens.Profile.route) Icons.Filled.Person else Icons.Outlined.Person,
                        contentDescription = "Profile"
                    )
                },
                onClick = {
                    if (screen != Screens.Profile.route) {
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
                        imageVector = if (screen == Screens.Login.route) Icons.Filled.Login else Icons.Outlined.Login,
                        contentDescription = "Login"
                    )
                },
                onClick = {
                    navController.navigate(Screens.Login.route)
                }
            )
        }
        NavigationBarItem(
            selected = screen == Screens.Settings.route,
            label = {
                Text(text = "Settings")
            },
            icon = {
                Icon(
                    imageVector = if (screen == Screens.Settings.route) Icons.Filled.Settings else Icons.Outlined.Settings,
                    contentDescription = "Settings"
                )
            },
            onClick = {
                navController.navigate(Screens.Settings.route)
            }
        )
    }
}
