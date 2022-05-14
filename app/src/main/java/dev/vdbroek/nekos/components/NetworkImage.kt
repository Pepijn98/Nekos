package dev.vdbroek.nekos.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import dev.vdbroek.nekos.R

data class ImageData(
    val id: Int,
    val image: String
)

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
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var drawable by remember { mutableStateOf<Drawable?>(null) }

    DisposableEffect(url) {
        val picasso = Picasso.get()

        val target = object : Target {
            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                drawable = placeHolderDrawable
            }

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                drawable = errorDrawable
            }

            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                image = bitmap?.asImageBitmap()
            }
        }

        picasso
            .load(url)
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.no_iamge_placeholder)
            .into(target)

        onDispose {
            image = null
            drawable = null
            picasso.cancelRequest(target)
        }
    }

    if (image != null) {
        Image(
            bitmap = image!!,
            contentDescription,
            modifier,
            alignment,
            contentScale,
            alpha,
            colorFilter
        )
    } else if (drawable != null) {
        Canvas(modifier = modifier) {
            drawIntoCanvas { canvas ->
                drawable!!.draw(canvas.nativeCanvas)
            }
        }
    }
}
