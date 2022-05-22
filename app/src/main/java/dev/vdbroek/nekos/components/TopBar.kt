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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.utils.App
import me.onebone.toolbar.CollapsingToolbarScaffold
import me.onebone.toolbar.CollapsingToolbarScaffoldScope
import me.onebone.toolbar.ScrollStrategy
import me.onebone.toolbar.rememberCollapsingToolbarScaffoldState

@Composable
fun TopBar(
    navController: NavHostController,
    route: String,
    body: @Composable (CollapsingToolbarScaffoldScope.() -> Unit)
) {
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
                Screens.ImageDetails.route -> {
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
                            // TODO : Logout
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logout),
                            contentDescription = "Order",
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

//    TopAppBar(
//        backgroundColor = MaterialTheme.colorScheme.primary,
//        contentColor = MaterialTheme.colorScheme.primary,
//        title = {
//            Text(
//                text = title ?: stringResource(id = R.string.app_name),
//                color = MaterialTheme.colorScheme.onPrimary
//            )
//        }
//    )
}
