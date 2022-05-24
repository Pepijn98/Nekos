package dev.vdbroek.nekos

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import dev.vdbroek.nekos.api.UserState
import dev.vdbroek.nekos.components.Alert
import dev.vdbroek.nekos.components.InfiniteListState
import dev.vdbroek.nekos.components.TopBar
import dev.vdbroek.nekos.components.isActive
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.screens.*
import dev.vdbroek.nekos.ui.theme.NekosTheme
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.LocalActivity
import dev.vdbroek.nekos.utils.dataStore
import kotlinx.coroutines.launch
import me.onebone.toolbar.ExperimentalToolbarApi
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

@Composable
fun EnterAnimation(content: @Composable () -> Unit) {
    AnimatedVisibility(
        visibleState = remember {
            MutableTransitionState(
                initialState = false
            )
        }.apply {
            targetState = true
        },
        enter = fadeIn(animationSpec = tween(200), initialAlpha = 0.3f),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalToolbarApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val coroutine = rememberCoroutineScope()
            val navController = rememberNavController()
            val toolbarScaffoldState = rememberCollapsingToolbarScaffoldState()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val toolbarState by remember { derivedStateOf { toolbarScaffoldState.toolbarState } }
            val currentRoute by remember { derivedStateOf { navBackStackEntry?.destination?.route } }

            CompositionLocalProvider(LocalActivity provides this) {
                NekosTheme {
                    when (currentRoute) {
                        Screens.Login.route,
                        Screens.Register.route -> {
                            window.statusBarColor = MaterialTheme.colorScheme.primary.toArgb()
                        }
                        else -> {
                            window.statusBarColor = MaterialTheme.colorScheme.background.toArgb()
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        window.insetsController?.setSystemBarsAppearance(
                            if (ThemeState.isDark)
                                0
                            else
                                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        window.decorView.systemUiVisibility =
                            if (ThemeState.isDark)
                                0
                            else
                                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }

                    Scaffold(
                        contentColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            if (
                                currentRoute == Screens.Login.route ||
                                currentRoute == Screens.Register.route ||
                                currentRoute == Screens.PostInfo.route
                            ) return@Scaffold

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
                                                            toolbarState.expand()
                                                        }
                                                    }
                                                    is LazyGridState -> {
                                                        val state = (InfiniteListState.scrollState as LazyGridState)
                                                        val firstVisibleItemIndex by derivedStateOf { state.firstVisibleItemIndex }

                                                        if (firstVisibleItemIndex > 1) {
                                                            state.scrollToItem(0)
                                                            toolbarState.expand()
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
                                                painter = painterResource(id = R.drawable.ic_login),
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
                            modifier = Modifier.padding(padding),
                            navController = navController,
                            startDestination = Screens.Home.route
                        ) {
                            composable(
                                route = Screens.Home.route
                            ) {
                                EnterAnimation {
                                    TopBar(
                                        navController = navController,
                                        toolbarScaffoldState = toolbarScaffoldState,
                                        dataStore = dataStore,
                                        route = Screens.Home.route
                                    ) {
                                        Home(
                                            navController = navController
                                        )
                                    }
                                }
                            }

                            composable(
                                route = Screens.PostInfo.route
                            ) {
                                val jsonData = it.arguments?.getString("data")
                                val data = Gson().fromJson(jsonData, Neko::class.java)
                                EnterAnimation {
                                    TopBar(
                                        navController = navController,
                                        toolbarScaffoldState = toolbarScaffoldState,
                                        dataStore = dataStore,
                                        route = Screens.PostInfo.route
                                    ) {
                                        PostInfo(
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
                                    TopBar(
                                        navController = navController,
                                        toolbarScaffoldState = toolbarScaffoldState,
                                        dataStore = dataStore,
                                        route = Screens.Settings.route
                                    ) {
                                        Settings(dataStore = dataStore)
                                    }
                                }
                            }

                            // -BEGIN: PROFILE FLOW
                            composable(
                                route = Screens.Login.route
                            ) {
                                EnterAnimation {
                                    Login(
                                        dataStore = dataStore,
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
                                    TopBar(
                                        navController = navController,
                                        toolbarScaffoldState = toolbarScaffoldState,
                                        dataStore = dataStore,
                                        route = Screens.Profile.route
                                    ) {
                                        Profile(
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
                                    TopBar(
                                        navController = navController,
                                        toolbarScaffoldState = toolbarScaffoldState,
                                        dataStore = dataStore,
                                        route = Screens.User.route
                                    ) {
                                        User(
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
            }
        }
    }
}
