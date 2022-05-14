package dev.vdbroek.nekos

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.github.kittinunf.fuel.core.FuelManager
import dev.vdbroek.nekos.api.Nekos
import dev.vdbroek.nekos.ui.screens.images
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.App
import dev.vdbroek.nekos.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val IS_DARK = booleanPreferencesKey("is_dark")
val MANUAL = booleanPreferencesKey("manual")

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val splashScreen = installSplashScreen()
            splashScreen.setKeepOnScreenCondition { true }
        }
        super.onCreate(savedInstanceState)

        val (version, code) = App.getVersions(this)
        App.version = version
        App.versionCode = code
        App.userAgent = "Nekos-Android/v$version (https://github.com/Pepijn98/Nekos)"

        FuelManager.instance.basePath = App.baseUrl
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to App.userAgent)

        lifecycleScope.launchWhenCreated {
            // Set theme colors
            ThemeState.isDark = dataStore.data.map { it[IS_DARK] ?: true }.first()
            ThemeState.manual = dataStore.data.map { it[MANUAL] ?: false }.first()

            val (response, exception) = Nekos.getImages()
            when {
                response != null -> images.addAll(response.images)
                exception != null -> finish()
            }

            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
