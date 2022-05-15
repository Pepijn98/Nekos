package dev.vdbroek.nekos.utils

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Converts dp to px using LocalDensity.
 */
val Int.px: Float @Composable get() = with(LocalDensity.current) { this@px.dp.toPx() }

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

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
