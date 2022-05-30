package dev.vdbroek.nekos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.kittinunf.fuel.core.FuelManager
import dev.vdbroek.nekos.api.Nekos
import dev.vdbroek.nekos.api.UserState
import dev.vdbroek.nekos.components.NekosAppContent
import dev.vdbroek.nekos.ui.Screens
import dev.vdbroek.nekos.ui.screens.HomeScreenState
import dev.vdbroek.nekos.ui.theme.NekosTheme
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.*
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {
    private var isSplashActive by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
                .also {
                    it.setKeepOnScreenCondition { isSplashActive }
                }
        }

        super.onCreate(savedInstanceState)

        val (version, code) = App.getVersions(this)
        App.version = version
        App.versionCode = code
        App.userAgent = "Nekos-Android/v$version (https://github.com/Pepijn98/Nekos)"

        FuelManager.instance.basePath = App.baseUrl
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to App.userAgent)

        lifecycleScope.launchWhenCreated {
            val prefs = dataStore.data.first()

            if (App.uncensored) {
                App.nsfw = prefs[NSFW] ?: App.defaultNsfw
            }

            ThemeState.isDark = prefs[IS_DARK] ?: true
            ThemeState.manual = prefs[MANUAL] ?: false
            ThemeState.staggered = prefs[STAGGERED] ?: false

            UserState.isLoggedIn = prefs[IS_LOGGED_IN] ?: false
            if (UserState.isLoggedIn) {
                UserState.token = prefs[TOKEN]
                UserState.username = prefs[USERNAME]
            }

            App.isReady = prefs[READY] ?: true

            val (tagsResponse) = Nekos.getTags()
            if (tagsResponse != null) {
                App.tags.addAll(tagsResponse.tags)
            }

            if (App.isReady) {
                val (response, exception) = Nekos.getImages()
                when {
                    response != null -> {
                        HomeScreenState.images.addAll(if (App.uncensored) response.images else response.images.filter { !it.tags.contains(App.buggedTag) })
                        isSplashActive = false
                        App.initialLoad = false
                    }
                    exception != null -> finish()
                }
            }
        }

        setContent {
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                App.permissionGranted = granted
            }

            LaunchedEffect(key1 = true) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    App.permissionGranted = true
                }
            }

            val navigation = rememberNavController()
            val entry by navigation.currentBackStackEntryAsState()
            val screen = remember { derivedStateOf { entry?.destination?.route ?: "" } }

            CompositionLocalProvider(
                LocalActivity provides this,
                LocalNavigation provides navigation,
                LocalScreen provides screen
            ) {
                NekosTheme {
                    val current by LocalScreen.current

                    when (current) {
                        Screens.Login.route,
                        Screens.Register.route -> {
                            window.statusBarColor = MaterialTheme.colorScheme.primary.toArgb()
                        }
                        else -> {
                            window.statusBarColor = MaterialTheme.colorScheme.background.toArgb()
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        window.insetsController?.setSystemBarsAppearance(
                            if (ThemeState.isDark)
                                0
                            else
                                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        window.decorView.systemUiVisibility =
                            if (ThemeState.isDark)
                                0
                            else
                                window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }

                    if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.S) && isSplashActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                modifier = Modifier
                                    .size(112.dp),
                                painter = painterResource(id = R.drawable.ic_logo_squircle),
                                contentDescription = "Splash logo"
                            )
                        }
                    } else {
                        NekosAppContent()
                    }
                }
            }
        }
    }
}
