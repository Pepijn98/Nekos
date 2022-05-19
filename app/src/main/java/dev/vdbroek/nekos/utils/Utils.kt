package dev.vdbroek.nekos.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlin.math.min

/**
 * Converts dp to px using LocalDensity.
 */
val Int.px: Float @Composable get() = with(LocalDensity.current) { this@px.dp.toPx() }

/**
 * Simple data store for key, value pairs
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Dim color
 */
fun Color.dim(factor: Float): Color {
//    return Color(ColorUtils.blendARGB(toArgb(), Color.Black.toArgb(), 0.2f))
    val r = min((red * factor), 255f)
    val g = min((green * factor), 255f)
    val b = min((blue * factor), 255f)
    return Color(r, g, b, alpha)
}

/**
 * Lighten color
 */
fun Color.lighten(factor: Float): Color {
    return Color(ColorUtils.blendARGB(toArgb(), Color.White.toArgb(), factor))

//    val r = min((red + 255 * factor), 255f)
//    val g = min((green + 255 * factor), 255f)
//    val b = min((blue + 255 * factor), 255f)
//    return Color(r, g, b, alpha)

//    val r = (255 - red) * factor + red
//    val g = (255 - green) * factor + green
//    val b = (255 - blue) * factor + blue
//    return Color(r, g, b, alpha, ColorSpaces.ProPhotoRgb)
}

val PaddingValues.top: Dp get() = calculateTopPadding()
val PaddingValues.end: Dp @Composable get() = calculateEndPadding(LocalLayoutDirection.current)
val PaddingValues.bottom: Dp get() = calculateBottomPadding()
val PaddingValues.start: Dp @Composable get() = calculateStartPadding(LocalLayoutDirection.current)

@Composable
fun PaddingValues.copy(
    start: Dp = this.start,
    top: Dp = this.top,
    end: Dp = this.end,
    bottom: Dp = this.bottom
): PaddingValues = PaddingValues(start, top, end, bottom)

object App {
    var screenTitle by mutableStateOf("")

    const val baseUrl = "https://nekos.moe/api/v1"

    lateinit var version: String
    lateinit var versionCode: String
    lateinit var userAgent: String

    fun getVersions(ctx: Context): Pair<String, String> {
        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        return Pair(packageInfo.versionName, String.format("%03d", packageInfo.longVersionCode))
    }

    fun hasLowRam(): Boolean {
        // Get app memory info
        val available = Runtime.getRuntime().maxMemory()
        val used = Runtime.getRuntime().totalMemory()

        // Check for & and handle low memory state
        val percentAvailable = 100f * (1f - used.toFloat() / available)

        return percentAvailable <= 5.0f
    }
}

val LocalActivity = staticCompositionLocalOf<ComponentActivity> {
    error("CompositionLocal LocalActivity not present")
}
