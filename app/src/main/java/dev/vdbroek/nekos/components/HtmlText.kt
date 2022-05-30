package dev.vdbroek.nekos.components

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import dev.vdbroek.nekos.ui.theme.NekoColors
import dev.vdbroek.nekos.ui.theme.ThemeState

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context)
        },
        update = {
            it.setTextColor(if (ThemeState.isDark) NekoColors.md_theme_dark_onBackground.toArgb() else NekoColors.md_theme_light_onBackground.toArgb())
            it.setLinkTextColor(if (ThemeState.isDark) NekoColors.md_theme_dark_primary.toArgb() else NekoColors.md_theme_light_primary.toArgb())
            it.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
            it.movementMethod = LinkMovementMethod.getInstance()
        }
    )
}
