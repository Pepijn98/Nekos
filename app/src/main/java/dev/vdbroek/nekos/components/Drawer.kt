package dev.vdbroek.nekos.components

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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.vdbroek.nekos.IS_DARK
import dev.vdbroek.nekos.MANUAL
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.drawerGesture
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.theme.ColorUI
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.LocalUserState
import dev.vdbroek.nekos.utils.px
import kotlinx.coroutines.launch

@Composable
fun Drawer(
    navController: NavController,
    dataStore: DataStore<Preferences>,
    scaffoldState: ScaffoldState
) {
    val user = LocalUserState.current
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
                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Image(
                            painter = painterResource(id = R.drawable.profile_placeholder),
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(100.dp),
                            contentDescription = null
                        )
                        Text(
                            text = "Pepijn van den Broek",
                            modifier = Modifier.padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
                            fontWeight = FontWeight(900),
                            color = ColorUI.light
                        )
                    }
                }
            }
        }
        DrawerRow(
            icon = { textColor ->
                Icon(
                    modifier = Modifier.padding(end = 5.dp),
                    imageVector = Icons.Filled.Home,
                    contentDescription = "",
                    tint = textColor
                )
            },
            title = "Home",
            selected = navBackStackEntry?.destination?.route == Screens.Home.route,
            onClick = {
                if (navBackStackEntry?.destination?.route != Screens.Home.route) {
                    coroutine.launch { scaffoldState.drawerState.close() }
                    navController.backQueue.clear()
                    navController.navigate(Screens.Home.route)
                }
            }
        )
        if (user.isLoggedIn) {
            DrawerRow(
                icon = { textColor ->
                    Icon(
                        modifier = Modifier.padding(end = 5.dp),
                        imageVector = Icons.Filled.Person,
                        contentDescription = "",
                        tint = textColor
                    )
                },
                title = "Profile",
                selected = navBackStackEntry?.destination?.route == Screens.Profile.route,
                onClick = {
                    if (navBackStackEntry?.destination?.route != Screens.Profile.route) {
                        coroutine.launch { scaffoldState.drawerState.close() }
                        navController.navigate(Screens.Profile.route)
                    }
                }
            )
        }
        DrawerRow(
            icon = { textColor ->
                Icon(
                    modifier = Modifier.padding(end = 5.dp),
                    painter = painterResource(id = R.drawable.login),
                    contentDescription = "",
                    tint = textColor
                )
            },
            title = "Login",
            selected = navBackStackEntry?.destination?.route == Screens.Login.route,
            onClick = {
                if (navBackStackEntry?.destination?.route != Screens.Login.route) {
                    coroutine.launch {
                        scaffoldState.drawerState.close()
                    }
                    navController.navigate(Screens.Login.route)
                }
            }
        )
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
private fun DrawerRow(icon: @Composable (textColor: Color) -> Unit, title: String, selected: Boolean, onClick: () -> Unit) {
    val drawerItemShape = RoundedCornerShape(percent = 50)
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Button(
        onClick = onClick,
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
