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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.ui.Navigation
import dev.vdbroek.nekos.ui.theme.ColorUI
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.px
import kotlinx.coroutines.launch

@Composable
fun Drawer(
    navController: NavController,
    dataStore: DataStore<Preferences>,
    scaffoldState: ScaffoldState
) {
    val coroutineScope = rememberCoroutineScope()

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
                            painter = painterResource(id = R.drawable.placeholder),
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
            icon = Icons.Filled.Home,
            title = "Home",
            selected = navController.currentBackStackEntry?.destination?.route == Navigation.Home.route,
            onClick = {
                if (navController.currentBackStackEntry?.destination?.route != Navigation.Home.route) {
                    navController.backQueue.clear()
                    navController.navigate(Navigation.Home.route)
                    coroutineScope.launch { scaffoldState.drawerState.close() }
                }
            }
        )
        DrawerRow(
            icon = Icons.Filled.Person,
            title = "Profile",
            selected = navController.currentBackStackEntry?.destination?.route == Navigation.Profile.route,
            onClick = {
                if (navController.currentBackStackEntry?.destination?.route != Navigation.Profile.route) {
                    navController.navigate(Navigation.Profile.route)
                    coroutineScope.launch { scaffoldState.drawerState.close() }
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
                coroutineScope.launch {
                    val MANUAL = booleanPreferencesKey("manual")
                    val IS_DARK = booleanPreferencesKey("is_dark")
                    dataStore.edit { preferences ->
                        preferences[MANUAL] = ThemeState.manual
                        preferences[IS_DARK] = ThemeState.isDark
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
private fun DrawerRow(icon: ImageVector, title: String, selected: Boolean, onClick: () -> Unit) {
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
            Icon(
                modifier = Modifier.padding(end = 5.dp),
                imageVector = icon, contentDescription = title, tint = textColor
            )
            Text(
                color = textColor,
                text = title
            )
        }
    }
}
