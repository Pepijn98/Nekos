package dev.vdbroek.nekos.ui.screens

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.ui.Navigation
import kotlinx.coroutines.delay

@Composable
fun Splash(navController: NavHostController) {
    SplashLayout()

    LaunchedEffect(key1 = true) {
        delay(4000)
        navController.popBackStack()
        navController.navigate(Navigation.Home.route)
    }
}

@Composable
fun SplashLayout() {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(120.dp),
            imageVector = Icons.Default.Email,
            contentDescription = "Logo Icon",
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
@Preview
fun SplashPreview() {
    SplashLayout()
}

@Composable
@Preview(uiMode = UI_MODE_NIGHT_YES)
fun SplashDarkPreview() {
    SplashLayout()
}
