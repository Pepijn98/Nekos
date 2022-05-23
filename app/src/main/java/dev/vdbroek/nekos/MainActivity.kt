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
import androidx.compose.foundation.gestures.animateScrollBy
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
import com.google.accompanist.systemuicontroller.rememberSystemUiController
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
import dev.vdbroek.nekos.utils.LocalActivity
import dev.vdbroek.nekos.utils.dataStore
import kotlinx.coroutines.launch

@Composable
fun EnterAnimation(content: @Composable () -> Unit) {
    AnimatedVisibility(
        visibleState = remember {
            MutableTransitionState(
                initialState = false
            )
        }.apply { targetState = true },
        enter = fadeIn(animationSpec = tween(500), initialAlpha = 0.3f),
        exit = fadeOut(animationSpec = tween(500))
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val coroutine = rememberCoroutineScope()
            val snackbarHost = remember { SnackbarHostState() }
            val navController = rememberNavController()
            val systemUiController = rememberSystemUiController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()

            val isHome = navBackStackEntry?.destination?.route == Screens.Home.route
            val isProfile = navBackStackEntry?.destination?.route == Screens.Profile.route
            val isSettings = navBackStackEntry?.destination?.route == Screens.Settings.route
            val isLogin = navBackStackEntry?.destination?.route == Screens.Login.route
            val isRegister = navBackStackEntry?.destination?.route == Screens.Register.route
            val isPost = navBackStackEntry?.destination?.route == Screens.PostInfo.route

            CompositionLocalProvider(LocalActivity provides this) {
                NekosTheme(systemUiController) {
                    window.statusBarColor = MaterialTheme.colorScheme.background.toArgb()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        window.insetsController?.setSystemBarsAppearance(
                            if (ThemeState.isDark) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        window.decorView.systemUiVisibility =
                            if (ThemeState.isDark) 0 else window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }

                    Scaffold(
                        contentColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            if (
                                isLogin ||
                                isRegister ||
                                isPost
                            ) return@Scaffold

                            NavigationBar {
                                NavigationBarItem(
                                    selected = isHome,
                                    label = {
                                        Text(text = "Home")
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isHome) Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = "Home"
                                        )
                                    },
                                    onClick = {
                                        if (!isHome) {
                                            navController.backQueue.clear()
                                            navController.navigate(Screens.Home.route)
                                        } else {
                                            coroutine.launch {
                                                when (InfiniteListState.scrollState) {
                                                    is LazyListState -> {
                                                        val state = (InfiniteListState.scrollState as LazyListState)
                                                        if (state.layoutInfo.visibleItemsInfo[0].index > 1) {
                                                            state.animateScrollToItem(0)
                                                        }
                                                    }
                                                    is LazyGridState -> {
                                                        val state = (InfiniteListState.scrollState as LazyGridState)
                                                        if (state.layoutInfo.visibleItemsInfo[0].index > 1) {
                                                            state.animateScrollToItem(0)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                                if (UserState.isLoggedIn) {
                                    NavigationBarItem(
                                        selected = isProfile,
                                        label = {
                                            Text(text = "Profile")
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isProfile) Icons.Filled.Person else Icons.Outlined.Person,
                                                contentDescription = "Profile"
                                            )
                                        },
                                        onClick = {
                                            if (!isProfile) {
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
                                    selected = isSettings,
                                    label = {
                                        Text(text = "Settings")
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSettings) Icons.Filled.Settings else Icons.Outlined.Settings,
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
                                hostState = snackbarHost,
                                onDismiss = {
                                    snackbarHost.isActive = false
                                    snackbarHost.currentSnackbarData?.dismiss()
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
                                        dataStore = dataStore,
                                        route = Screens.Home.route
                                    ) {
                                        Home(
                                            snackbarHost = snackbarHost,
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
                                        snackbarHost = snackbarHost,
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
                                        snackbarHost = snackbarHost,
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
                                        dataStore = dataStore,
                                        route = Screens.Profile.route
                                    ) {
                                        Profile(
                                            navController = navController,
                                            snackbarHost = snackbarHost
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
                                        dataStore = dataStore,
                                        route = Screens.User.route
                                    ) {
                                        User(
                                            navController = navController,
                                            snackbarHost = snackbarHost,
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
