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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import dev.vdbroek.nekos.components.Alert
import dev.vdbroek.nekos.components.NekosAppBar
import dev.vdbroek.nekos.components.NekosNavBar
import dev.vdbroek.nekos.components.isActive
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.screens.*
import dev.vdbroek.nekos.ui.theme.NekosTheme
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.LocalActivity
import dev.vdbroek.nekos.utils.dataStore

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
    private val noNavBar = listOf(
        Screens.Login.route,
        Screens.Register.route,
        Screens.PostInfo.route
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
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
                                    NekosAppBar(
                                        navController = navController,
                                        dataStore = dataStore,
                                        route = currentRoute
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
                                    NekosAppBar(
                                        navController = navController,
                                        dataStore = dataStore,
                                        route = currentRoute
                                    ) { toolbarState ->
                                        PostInfo(
                                            toolbarState = toolbarState,
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
                                        dataStore = dataStore,
                                        route = currentRoute
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
                                    NekosAppBar(
                                        navController = navController,
                                        dataStore = dataStore,
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
                                        dataStore = dataStore,
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
            }
        }
    }
}
