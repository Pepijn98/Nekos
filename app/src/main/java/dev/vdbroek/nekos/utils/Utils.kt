package dev.vdbroek.nekos.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import androidx.navigation.NavHostController
import dev.vdbroek.nekos.BuildConfig
import io.iamjosephmj.flinger.configs.FlingConfiguration
import io.iamjosephmj.flinger.flings.flingBehavior
import me.onebone.toolbar.CollapsingToolbarScaffoldState
import me.onebone.toolbar.CollapsingToolbarState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ln
import kotlin.math.min

val LocalActivity = staticCompositionLocalOf<ComponentActivity> {
    error("CompositionLocal LocalActivity not present")
}

val LocalNavigation = staticCompositionLocalOf<NavHostController> {
    error("CompositionLocal LocalNavigation not present")
}

val LocalScreen = staticCompositionLocalOf<State<String>> {
    error("CompositionLocal LocalScreen not present")
}

/**
 * Converts dp to px using LocalDensity.
 */
val Int.px: Float @Composable get() = with(LocalDensity.current) { this@px.dp.toPx() }

/**
 * Simple data store for key, value pairs
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = if (App.uncensored) "nekos-uncensored-settings" else "nekos-settings")

/**
 * Dim color
 */
fun Color.dim(factor: Float): Color {
    require(factor in 0.0..1.0) {
        "factor has to be between 0.0 and 1.0"
    }

    val r = min((red * factor), 255f)
    val g = min((green * factor), 255f)
    val b = min((blue * factor), 255f)
    return Color(r, g, b, alpha)
}

/**
 * Lighten color
 */
fun Color.lighten(factor: Float): Color {
    require(factor in 0.0..1.0) {
        "factor has to be between 0.0 and 1.0"
    }

    return Color(ColorUtils.blendARGB(toArgb(), Color.White.toArgb(), factor))
}

/**
 * Set alpha for color
 */
fun Color.alpha(newAlpha: Float): Color {
    require(newAlpha in 0.0..1.0) {
        "alpha has to be between 0.0 and 1.0"
    }

    return Color(red, green, blue, newAlpha)
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

/**
 * Create a copy of the original state list
 */
fun <T> SnapshotStateList<T>.copy() = mutableStateListOf<T>().also { it.addAll(this) }

/**
 * Capitalize string (defaults to Locale.ROOT)
 */
fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

@Composable
fun EnterAnimation(content: @Composable () -> Unit) {
    AnimatedVisibility(
        visibleState = remember {
            MutableTransitionState(
                initialState = false
            )
        }.apply {
            targetState = true
        },
        enter = fadeIn(animationSpec = tween(200), initialAlpha = 0.3f),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        content()
    }
}

@Composable
fun <T> rememberMutableStateOf(
    value: T,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
): MutableState<T> = remember {
    mutableStateOf(value, policy)
}

//class NavigationHost(val controller: NavHostController) {
//
//    val entry
//        @Composable
//        get() = controller.currentBackStackEntryAsState()
//
//    val currentScreen by derivedStateOf { controller.currentBackStackEntry?.destination?.route }
//}

class ToolbarScaffoldHost(val scaffoldState: CollapsingToolbarScaffoldState) {

    val toolbarState by derivedStateOf { scaffoldState.toolbarState }
}

object App {
    const val baseUrl = "https://nekos.moe/api/v1"

    /**
     * Uncensored build version (only on github)
     */
    const val uncensored = BuildConfig.BUILD_TYPE == "uncensored"

    var isReady by mutableStateOf(false)
    var initialLoad by mutableStateOf(true)

    const val defaultNsfw = "no_nsfw"
    var nsfw by mutableStateOf(defaultNsfw)
    var permissionGranted by mutableStateOf(false)
    var screenTitle by mutableStateOf("")
    val snackbarHost = SnackbarHostState()

    // Only use when absolutely necessary and there is 100% no other way to access the toolbar state
    var globalToolbarState: CollapsingToolbarState? = null

    lateinit var version: String
    lateinit var versionCode: String
    lateinit var userAgent: String

    val tags = mutableStateListOf<String>()
    const val defaultSort = "newest"
    val buggedTag = if (uncensored) "" else "off-shoulder shirt"
    val defaultTags = if (uncensored) listOf() else listOf(
        "-bare shoulders",
        "-bikini",
        "-crop top",
        "-swimsuit",
        "-midriff",
        "-no bra",
        "-panties",
        "-covered nipples",
        "-from behind",
        "-knees up",
        "-leotard",
        "-black bikini top",
        "-black bikini bottom",
        "-off-shoulder shirt",
        "-naked shirt"
    )

    const val minUsernameChars = 1
    const val maxUsernameChars = 35

    const val minEmailChars = 5
    const val maxEmailChars = 70

    const val minPasswordChars = 8
    const val maxPasswordChars = 70

    @Composable
    fun flingBehavior() = flingBehavior(
        FlingConfiguration.Builder()
            .scrollViewFriction(0.008f)
            .absVelocityThreshold(0f)
            .gravitationalForce(9.80665f)
            .inchesPerMeter(39.37f)
            .decelerationRate((ln(0.78) / ln(0.9)).toFloat())
            .decelerationFriction(0.09f)
            .splineInflection(0.1f)
            .splineStartTension(0.1f)
            .splineEndTension(1.0f)
            .numberOfSplinePoints(100)
            .build(),
    )

    /**
     * Usernames cannot contain whitespace chars, newlines or "@" and have to be between 1 and 35 characters
     */
    fun validateUsername(text: String): Boolean =
        (!Regex("[@\\r\\n\\t\\s]").containsMatchIn(text) && text.length in minUsernameChars..maxUsernameChars)

    /**
     * Validate email pattern and have to be between 5 and 70 characters
     */
    fun validateEmail(text: String): Boolean =
        (Regex("^[^@]+@[^.@]+\\.[^.@]+$").matches(text) && text.length in minEmailChars..maxEmailChars)

    /**
     * Passwords have to be between 8 and 70 characters
     */
    fun validatePassword(text: String): Boolean =
        (text.length in minPasswordChars..maxPasswordChars)

    fun timestamp(timeCreated: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
        val timeCreatedDate = dateFormat.parse(timeCreated)!!
        return DateUtils.getRelativeTimeSpanString(timeCreatedDate.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS) as String
    }

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

    fun Context.saveImageBitmap(image: Bitmap, id: String): Boolean {
        val mediaStorage = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}${File.separator}Nekos${File.separator}")
        if (!mediaStorage.exists()) {
            mediaStorage.mkdirs()
        }

        val file = File(mediaStorage, "$id.jpg").also {
            it.createNewFile()
        }
        val fos = FileOutputStream(file)

        return image
            .compress(Bitmap.CompressFormat.JPEG, 100, fos)
            .also {
                MediaScannerConnection.scanFile(this, arrayOf(file.toString()), arrayOf(file.name), null)
                fos.flush()
                fos.close()
            }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun Context.saveImageBitmap29(image: Bitmap, id: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, id)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Nekos")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        val fos = contentResolver.openOutputStream(uri) ?: return false

        return image
            .compress(Bitmap.CompressFormat.JPEG, 100, fos)
            .also {
                fos.flush()
                fos.close()
            }
    }
}
