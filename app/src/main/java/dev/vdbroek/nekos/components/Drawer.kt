package dev.vdbroek.nekos.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ButtonDefaults.elevation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.SplashActivity
import dev.vdbroek.nekos.api.UserState
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.theme.ColorUI
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.*
import kotlinx.coroutines.launch

@Composable
fun Drawer(
    navController: NavController,
    dataStore: DataStore<Preferences>,
    scaffoldState: ScaffoldState
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val coroutine = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = remember {
            object : Arrangement.Vertical {
                override fun Density.arrange(
                    totalSize: Int,
                    sizes: IntArray,
                    outPositions: IntArray
                ) {
                    var currentOffset = 0
                    sizes.forEachIndexed { index, size ->
                        if (index == sizes.lastIndex) {
                            outPositions[index] = totalSize - size
                        } else {
                            outPositions[index] = currentOffset
                            currentOffset += size
                        }
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            0.0f to ColorUI.blue200,
                            0.5f to ColorUI.blue500,
                            1.0f to ColorUI.blue700,
                            start = Offset(x = 0.0f, y = 0.0f),
                            end = Offset(x = constraints.maxWidth.px, y = constraints.maxHeight.px)
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    if (UserState.isLoggedIn) {
                        Column(modifier = Modifier.align(Alignment.BottomStart)) {
                            Image(
                                painter = painterResource(id = R.drawable.profile_placeholder),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(100.dp),
                                contentDescription = null
                            )
                            Text(
                                text = UserState.name ?: "",
                                modifier = Modifier.padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
                                fontWeight = FontWeight(900),
                                color = ColorUI.light
                            )
                        }
                    }
                }
            }
        }
        DrawerRow(
            scaffoldState = scaffoldState,
            icon = { textColor ->
                Icon(
                    modifier = Modifier.padding(end = 5.dp),
                    imageVector = Icons.Filled.Home,
                    contentDescription = "",
                    tint = textColor
                )
            },
            title = "Home",
            selected = navBackStackEntry?.destination?.route == Screens.Home.route
        ) {
            if (navBackStackEntry?.destination?.route != Screens.Home.route) {
                navController.backQueue.clear()
                navController.navigate(Screens.Home.route) {
                    restoreState = true
                }
            }
        }
        if (UserState.isLoggedIn) {
            DrawerRow(
                scaffoldState = scaffoldState,
                icon = { textColor ->
                    Icon(
                        modifier = Modifier.padding(end = 5.dp),
                        imageVector = Icons.Filled.Person,
                        contentDescription = "",
                        tint = textColor
                    )
                },
                title = "Profile",
                selected = navBackStackEntry?.destination?.route == Screens.Profile.route
            ) {
                if (navBackStackEntry?.destination?.route != Screens.Profile.route) {
                    navController.navigate(Screens.Profile.route)
                }
            }
            DrawerRow(
                scaffoldState = scaffoldState,
                icon = { textColor ->
                    Icon(
                        modifier = Modifier.padding(end = 5.dp),
                        painter = painterResource(id = R.drawable.logout),
                        contentDescription = "",
                        tint = textColor
                    )
                },
                title = "Logout",
                selected = navBackStackEntry?.destination?.route == Screens.Login.route
            ) {
                dataStore.edit { preferences ->
                    preferences[TOKEN] = ""
                    preferences[USERNAME] = ""
                    preferences[IS_LOGGED_IN] = false
                }

                UserState.apply {
                    token = null
                    name = null
                    isLoggedIn = false
                }
            }
        } else {
            DrawerRow(
                scaffoldState = scaffoldState,
                icon = { textColor ->
                    Icon(
                        modifier = Modifier.padding(end = 5.dp),
                        painter = painterResource(id = R.drawable.login),
                        contentDescription = "",
                        tint = textColor
                    )
                },
                title = "Login",
                selected = navBackStackEntry?.destination?.route == Screens.Login.route
            ) {
                if (navBackStackEntry?.destination?.route != Screens.Login.route) {
                    navController.navigate(Screens.Login.route)
                }
            }
        }
        DrawerRow(
            scaffoldState = scaffoldState,
            icon = { textColor ->
                Icon(
                    modifier = Modifier.padding(end = 5.dp),
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "",
                    tint = textColor
                )
            },
            title = "Restart"
        ) {
            /*
            TODO:
             Create an option where people can choose to use a staggered layout or fixed size layout
             Restart app on option change
             */
            val splashActivity = Intent(context, SplashActivity::class.java)
            activity.finish()
            activity.startActivity(splashActivity)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = {
                ThemeState.manual = true
                ThemeState.isDark = !ThemeState.isDark
                coroutine.launch {
                    dataStore.edit { preferences ->
                        preferences[IS_DARK] = ThemeState.isDark
                        preferences[MANUAL] = ThemeState.manual
                    }
                }
            }) {
                Icon(
                    painter = if (ThemeState.isDark) painterResource(id = R.drawable.light_mode) else painterResource(id = R.drawable.dark_mode),
                    contentDescription = "Change Theme",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun DrawerRow(
    scaffoldState: ScaffoldState,
    icon: @Composable (textColor: Color) -> Unit,
    title: String,
    selected: Boolean = false,
    onClick: suspend () -> Unit
) {
    val coroutine = rememberCoroutineScope()
    val drawerItemShape = RoundedCornerShape(percent = 50)
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Button(
        onClick = {
            coroutine.launch {
                scaffoldState.drawerState.close()
                onClick()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 2.dp, end = 8.dp)
            .clip(drawerItemShape),
        shape = drawerItemShape,
        colors = ButtonDefaults.buttonColors(backgroundColor = background),
        elevation = elevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            icon(textColor)
            Text(
                color = textColor,
                text = title
            )
        }
    }
}
