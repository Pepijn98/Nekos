package dev.vdbroek.nekos.ui.screens

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.ui.Navigation
import kotlinx.coroutines.delay

@Composable
fun Splash(navController: NavHostController) {
    SplashLayout()

    LaunchedEffect(key1 = true) {
        // TODO : Remove delay and make an actual api request to get the intial set of images
        delay(2000)
        navController.popBackStack()
        navController.navigate(Navigation.Home.route)
    }
}

@Composable
fun SplashLayout() {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )

    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier
                .clip(CircleShape)
                .size(160.dp)
                .graphicsLayer {
                    rotationZ = angle
                },
            painter = painterResource(id = R.drawable.icon),
            contentDescription = "Logo Icon"
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
