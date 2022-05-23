package dev.vdbroek.nekos.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.api.NekosUserState
import dev.vdbroek.nekos.api.UserState
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.screens.ProfileScreenState
import dev.vdbroek.nekos.ui.screens.UserScreenState
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.IS_LOGGED_IN
import dev.vdbroek.nekos.utils.TOKEN
import dev.vdbroek.nekos.utils.USERNAME
import kotlinx.coroutines.launch
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.CollapsingToolbarScaffoldScope
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

@Composable
fun TopBar(
    navController: NavHostController,
    dataStore: DataStore<Preferences>,
    route: String,
    body: @Composable (CollapsingToolbarScaffoldScope.() -> Unit)
) {
    val coroutine = rememberCoroutineScope()
    val toolbarScaffoldState = rememberCollapsingToolbarScaffoldState()
    val progress = toolbarScaffoldState.toolbarState.progress

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
                    .pin()
            )
            when (route) {
                Screens.Home.route -> {
                    IconButton(
                        modifier = Modifier
                            .road(
                                whenCollapsed = Alignment.CenterStart,
                                whenExpanded = Alignment.TopStart
                            ),
                        onClick = {
                            // TODO : Animated into TextInput
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
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
                            // TODO : Show options menu
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sort),
                            contentDescription = "Order",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Screens.Settings.route,
                Screens.PostInfo.route -> {
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

                            NekosUserState.apply {
                                end = false
                                skip = 0
                                tags = App.defaultTags
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

                            NekosUserState.apply {
                                end = false
                                skip = 0
                                tags = App.defaultTags
                            }

                            ProfileScreenState.apply {
                                uploaderImages.clear()
                                initialRequest = true
                                user = null
                            }

                            coroutine.launch {
                                dataStore.edit { preferences ->
                                    preferences[IS_LOGGED_IN] = false
                                    preferences[TOKEN] = ""
                                    preferences[USERNAME] = ""
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logout),
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

                            NekosUserState.apply {
                                end = false
                                skip = 0
                                tags = App.defaultTags
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
                fontSize = (MaterialTheme.typography.headlineLarge.fontSize.value + (30 - 18) * progress).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        body = body
    )
}
