package dev.vdbroek.nekos.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dev.vdbroek.nekos.R
import dev.vdbroek.nekos.ui.theme.ThemeState
import dev.vdbroek.nekos.utils.GlideApp
import java.io.ByteArrayOutputStream


@Composable
fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    thumbnail: Boolean = true
) {

    val context = LocalContext.current
    val placeholder = painterResource(id = R.drawable.placeholder)
    val failedPlaceholder = painterResource(id = R.drawable.no_iamge_placeholder)
    var state by remember { mutableStateOf(placeholder) }
    var isPlaceholder by remember { mutableStateOf(true) }

    val requestOptions = if (thumbnail) {
        // Caching a big list of images makes the scrolling very underperform, who would have tought
        RequestOptions()
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
    } else {
        RequestOptions()
            .downsample(DownsampleStrategy.CENTER_INSIDE)
    }

    GlideApp.with(context)
        .asBitmap()
        .load(url)
        .centerInside()
        .apply(requestOptions)
        .into(object : CustomTarget<Bitmap>() {
            override fun onLoadCleared(p: Drawable?) {
                isPlaceholder = true
                state = placeholder
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                isPlaceholder = true
                state = failedPlaceholder
            }

            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                isPlaceholder = false
                state = if (thumbnail) {
                    val stream = ByteArrayOutputStream()
                    resource.compress(Bitmap.CompressFormat.JPEG, 50, stream)
                    val byteArray = stream.toByteArray()
                    val compressed = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    BitmapPainter(compressed.asImageBitmap())
                } else {
                    BitmapPainter(resource.asImageBitmap())
                }
            }
        })

    Image(
        painter = state,
        contentDescription = contentDescription,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
        alpha = if (isPlaceholder) 0.2f else alpha,
        colorFilter = colorFilter
    )
}
