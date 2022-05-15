package dev.vdbroek.nekos.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dev.vdbroek.nekos.R

@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {

    val context = LocalContext.current
    val placeholder = painterResource(id = R.drawable.placeholder)
    val failedPlaceholder = painterResource(id = R.drawable.no_iamge_placeholder)
    var state by remember { mutableStateOf(placeholder) }

    Glide.with(context)
        .asBitmap()
        .load(url)
        .centerInside()
        .into(object : CustomTarget<Bitmap>() {
            override fun onLoadCleared(p: Drawable?) {
                state = placeholder
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                state = failedPlaceholder
            }

            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                state = BitmapPainter(resource.asImageBitmap())
            }
        })

    Image(
        painter = state,
        contentDescription,
        modifier,
        alignment,
        contentScale,
        alpha,
        colorFilter
    )
}
