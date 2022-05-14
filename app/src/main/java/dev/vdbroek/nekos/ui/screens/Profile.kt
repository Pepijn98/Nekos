package dev.vdbroek.nekos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.screenTitle

@Composable
fun Profile(navController: NavHostController) {
    screenTitle = "Profile"

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            modifier = Modifier.size(120.dp),
            onClick = {
                navController.backQueue.clear()
                navController.navigate(Screens.Home.route)
            }
        ) {
            Icon(
                modifier = Modifier.size(120.dp),
                imageVector = Icons.Default.Home,
                contentDescription = "Logo Icon",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
