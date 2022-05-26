package dev.vdbroek.nekos.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.ui.theme.imageShape
import dev.vdbroek.nekos.utils.GlideApp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ZoomableNetworkImage(
    url: String?,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    shape: Shape = imageShape,
    maxScale: Float = 1f,
    minScale: Float = 3f,
    isRotation: Boolean = false,
    isZoomable: Boolean = true,
    scrollState: ScrollableState? = null
) {

    val context = LocalContext.current
    val placeholder = painterResource(id = R.drawable.placeholder)
    val failedPlaceholder = painterResource(id = R.drawable.no_image_placeholder)
    var state by remember { mutableStateOf(placeholder) }

    GlideApp.with(context)
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

    ZoomableImage(
        painter = state,
        modifier = modifier,
        backgroundColor = backgroundColor,
        imageAlign = alignment,
        contentScale = contentScale,
        shape = shape,
        maxScale = maxScale,
        minScale = minScale,
        isRotation = isRotation,
        isZoomable= isZoomable,
        scrollState = scrollState
    )
}
