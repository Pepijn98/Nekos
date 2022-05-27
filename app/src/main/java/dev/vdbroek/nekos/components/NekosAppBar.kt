package dev.vdbroek.nekos.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.api.UserRequestState
import dev.vdbroek.nekos.api.UserState
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.screens.ProfileScreenState
import dev.vdbroek.nekos.ui.screens.UserScreenState
import dev.vdbroek.nekos.utils.*
import kotlinx.coroutines.launch
import me.onebone.toolbar.*

@Composable
fun NekosAppBar(
    navController: NavHostController,
    route: String?,
    body: @Composable (CollapsingToolbarScaffoldScope.(CollapsingToolbarState) -> Unit)
) {
    val context = LocalContext.current

    val toolbarScaffoldState = rememberCollapsingToolbarScaffoldState()
    val coroutine = rememberCoroutineScope()
    val toolbarState by remember { derivedStateOf { toolbarScaffoldState.toolbarState } }

    App.globalToolbarState = toolbarState

    CollapsingToolbarScaffold(
        modifier = Modifier
            .fillMaxSize(),
        state = toolbarScaffoldState,
        scrollStrategy = ScrollStrategy.EnterAlwaysCollapsed,
        toolbar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .parallax(ratio = 0.2f)
            )
            when (route) {
                Screens.Home.route -> {
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .road(
                                whenCollapsed = Alignment.CenterEnd,
                                whenExpanded = Alignment.TopEnd
                            )
                    ) {
                        SortingDropdown()
                        IconButton(
                            onClick = {
                                SortingDropdownState.expanded = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sort,
                                contentDescription = "Order",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                Screens.Settings.route,
                Screens.Post.route -> {
                    IconButton(
                        modifier = Modifier
                            .road(
                                whenCollapsed = Alignment.CenterStart,
                                whenExpanded = Alignment.TopStart
                            ),
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Screens.Profile.route -> {
                    IconButton(
                        modifier = Modifier
                            .road(
                                whenCollapsed = Alignment.CenterStart,
                                whenExpanded = Alignment.TopStart
                            ),
                        onClick = {
                            navController.popBackStack()

                            UserRequestState.apply {
                                end = false
                                skip = 0
                                tags = App.defaultTags.toMutableStateList()
                            }

                            ProfileScreenState.apply {
                                uploaderImages.clear()
                                initialRequest = true
                                user = null
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        modifier = Modifier
                            .road(
                                whenCollapsed = Alignment.CenterEnd,
                                whenExpanded = Alignment.TopEnd
                            ),
                        onClick = {
                            navController.backQueue.clear()
                            navController.navigate(Screens.Home.route)

                            UserState.apply {
                                isLoggedIn = false
                                token = null
                                username = null
                            }

                            UserRequestState.apply {
                                end = false
                                skip = 0
                                tags = App.defaultTags.toMutableStateList()
                            }

                            ProfileScreenState.apply {
                                uploaderImages.clear()
                                initialRequest = true
                                user = null
                            }

                            coroutine.launch {
                                context.dataStore.edit { preferences ->
                                    preferences[IS_LOGGED_IN] = false
                                    preferences[TOKEN] = ""
                                    preferences[USERNAME] = ""
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Screens.User.route -> {
                    IconButton(
                        modifier = Modifier
                            .road(
                                whenCollapsed = Alignment.CenterStart,
                                whenExpanded = Alignment.TopStart
                            ),
                        onClick = {
                            navController.popBackStack()

                            UserRequestState.apply {
                                end = false
                                skip = 0
                                tags = App.defaultTags.toMutableStateList()
                            }

                            UserScreenState.apply {
                                uploaderImages.clear()
                                initialRequest = true
                                user = null
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            Text(
                modifier = Modifier
                    .padding(16.dp, 16.dp, 16.dp, 16.dp)
                    .road(
                        whenCollapsed = Alignment.Center,
                        whenExpanded = Alignment.BottomStart
                    ),
                text = App.screenTitle,
                fontSize = (MaterialTheme.typography.headlineLarge.fontSize.value + (30 - 18) * toolbarState.progress).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        body = {
            body(toolbarState)
        }
    )
}
