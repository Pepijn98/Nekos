package dev.vdbroek.nekos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.gson.Gson
import dev.vdbroek.nekos.components.Alert
import dev.vdbroek.nekos.components.Drawer
import dev.vdbroek.nekos.components.TopBar
import dev.vdbroek.nekos.components.isActive
import dev.vdbroek.nekos.models.Neko
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.screens.*
import dev.vdbroek.nekos.ui.theme.NekosTheme
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
        }.apply { targetState = true },
        enter = fadeIn(animationSpec = tween(500), initialAlpha = 0.3f),
        exit = fadeOut(animationSpec = tween(500))
    ) {
        content()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
            val navBackStackEntry by navController.currentBackStackEntryAsState()

            CompositionLocalProvider(LocalActivity provides this) {
                NekosTheme {
                    Scaffold(
                        scaffoldState = scaffoldState,
                        drawerBackgroundColor = MaterialTheme.colorScheme.surface,
                        drawerContentColor = MaterialTheme.colorScheme.surface,
                        drawerScrimColor = DrawerDefaults.scrimColor,
                        backgroundColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            if (navBackStackEntry?.destination?.route != Screens.Login.route) {
                                TopBar(scaffoldState = scaffoldState, title = App.screenTitle)
                            }
                        },
                        drawerShape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                        drawerGesturesEnabled = navBackStackEntry?.destination?.route != Screens.Login.route,
                        drawerContent = {
                            if (navBackStackEntry?.destination?.route != Screens.Login.route) {
                                Drawer(navController = navController, dataStore = dataStore, scaffoldState = scaffoldState)
                            }
                        },
                        snackbarHost = {
                            Alert(hostState = it, onDismiss = {
                                it.isActive = false
                                it.currentSnackbarData?.dismiss()
                            })
                        },
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Screens.Home.route
                        ) {
                            composable(route = Screens.Home.route) {
                                EnterAnimation {
                                    Home(state = scaffoldState, navController = navController)
                                }
                            }

                            composable(route = Screens.ImageDetails.route) {
                                val jsonData = it.arguments?.getString("data")
                                val data = Gson().fromJson(jsonData, Neko::class.java)
                                EnterAnimation {
                                    ImageDetails(data = data)
                                }
                            }
                            composable(route = Screens.Login.route) {
                                EnterAnimation {
                                    Login(state = scaffoldState, dataStore = dataStore, navController = navController)
                                }
                            }

                            composable(route = Screens.Profile.route) {
                                EnterAnimation {
                                    Profile(navController = navController)
                                }
                            }

                            composable(route = Screens.Register.route) {
                                EnterAnimation {
                                    Register(navController = navController)
                                }
                            }

                            composable(route = Screens.ForgotPassword.route) {
                                EnterAnimation {
                                    ForgotPassword(navController = navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
