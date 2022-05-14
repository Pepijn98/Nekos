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
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.vdbroek.nekos.components.Alert
import dev.vdbroek.nekos.components.Drawer
import dev.vdbroek.nekos.components.TopBar
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.screens.Home
import dev.vdbroek.nekos.ui.screens.ImageDetails
import dev.vdbroek.nekos.ui.screens.Profile
import dev.vdbroek.nekos.ui.theme.NekosTheme
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

var screenTitle: String? by mutableStateOf(null)

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

            NekosTheme {
                Scaffold(
                    scaffoldState = scaffoldState,
                    drawerBackgroundColor = MaterialTheme.colorScheme.surface,
                    drawerContentColor = MaterialTheme.colorScheme.surface,
                    drawerScrimColor = DrawerDefaults.scrimColor,
                    backgroundColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopBar(scaffoldState = scaffoldState, title = screenTitle)
                    },
                    drawerShape = RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp),
                    drawerContent = {
                        Drawer(navController = navController, dataStore = dataStore, scaffoldState = scaffoldState)
                    },
                    snackbarHost = {
                        Alert(hostState = it, onDismiss = {
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
                                Home(navController = navController)
                            }
                        }

                        composable(route = Screens.ImageDetails.route + "/{id}") {
                            val id = it.arguments?.getString("id")
                            EnterAnimation {
                                ImageDetails(navController = navController, id = id)
                            }
                        }

                        composable(route = Screens.Profile.route) {
                            EnterAnimation {
                                Profile(navController = navController)
                            }
                        }
                    }
                }
            }
        }
    }
}
