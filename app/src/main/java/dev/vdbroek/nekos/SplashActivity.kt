package dev.vdbroek.nekos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.lifecycleScope
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.dataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val IS_DARK = booleanPreferencesKey("is_dark")
val MANUAL = booleanPreferencesKey("manual")

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            val splashScreen = installSplashScreen()
            splashScreen.setKeepOnScreenCondition { true }
        }
        super.onCreate(savedInstanceState)

        lifecycleScope.launchWhenCreated {
            // Set theme colors
            ThemeState.isDark = dataStore.data.map { it[IS_DARK] ?: true }.first()
            ThemeState.manual = dataStore.data.map { it[MANUAL] ?: false }.first()

            // TODO : Remove delay and make an actual api request to get the intial set of images
            delay(3000)

            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
