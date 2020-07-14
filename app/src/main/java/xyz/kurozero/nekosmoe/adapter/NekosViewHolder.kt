package xyz.kurozero.nekosmoe.adapter

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import kotlinx.android.synthetic.main.grid_list_item.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import xyz.kurozero.nekosmoe.model.Neko
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.snackbar.Snackbar
import com.hendraanggrian.pikasso.into
import com.hendraanggrian.pikasso.picasso
import com.squareup.picasso.Picasso
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.android.synthetic.main.dialog_view_neko.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import xyz.kurozero.nekosmoe.*
import xyz.kurozero.nekosmoe.helper.Api
import xyz.kurozero.nekosmoe.helper.hasPermissions
import xyz.kurozero.nekosmoe.helper.isConnected
import java.io.*

lateinit var file: File

class NekosViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindView(neko: Neko) {
        itemView.nekoImg.foreground = itemView.context.getDrawable(R.drawable.image_border)

        val radius = itemView.context.resources.getDimensionPixelSize(R.dimen.corner_radius)

        Glide.with(itemView.context)
            .load(neko.getThumbnailUrl())
            .transform(CenterCrop(), RoundedCorners(radius))
            .into(itemView.nekoImg)

        itemView.nekoImg.onClick {
            val view = LayoutInflater.from(itemView.context).inflate(R.layout.dialog_view_neko, null)
            val nekoDialog = AlertDialog.Builder(itemView.context)
                .setView(view)
                .create()

            view.tvUploader.text = itemView.context.getString(R.string.uploaded_by, neko.uploader.username)
            view.tvApproved.text = itemView.context.getString(R.string.approved_by, neko.approver?.username ?: "-")
            view.tvNekoFavorites.text = itemView.context.getString(R.string.neko_favorites, neko.favorites)
            view.tvNekoLikes.text = itemView.context.getString(R.string.neko_likes, neko.likes)
            view.tvArtist.text = itemView.context.getString(R.string.neko_artist, neko.artist)
            view.tvTags.text = itemView.context.getString(R.string.neko_tags, neko.tags.joinToString(", "))
            view.tvTags.movementMethod = ScrollingMovementMethod()

            Glide.with(itemView.context)
                .load(neko.getImageUrl())
                .centerCrop()
                .into(view.fullNekoImg)

            view.fullNekoImg.onClick {
                StfalconImageViewer.Builder(itemView.context, listOf(neko)) { view, neko ->
                    Picasso.get().load(neko.getImageUrl()).into(view)
                }.show()
            }

            if (isLoggedin) {
                if (user == null)
                    return@onClick

                val liked = user?.likes?.find { id -> id == neko.id }
                val faved = user?.favorites?.find { id -> id == neko.id }
                view.btnLikeNeko.text = if (liked.isNullOrBlank()) "Like" else "Unlike"
                view.btnFavNeko.text = if (faved.isNullOrBlank()) "Favorite" else "Unfavorite"
            }

            val token = sharedPreferences.getString("token", "") ?: ""
            view.btnLikeNeko.onClick {
                if (isLoggedin) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val likedNeko = user?.likes?.find { id -> id == neko.id }
                        val reqbodystr =
                            if (likedNeko.isNullOrBlank()) "{\"create\": true, \"type\": \"like\"}"
                            else "{\"create\": false, \"type\": \"like\"}"

                        val reqbody = RequestBody.create(MediaType.parse("application/json"), reqbodystr)
                        val requrl = "https://nekos.moe/api/v1/image/${neko.id}/relationship"

                        val headers = okhttp3.Headers.Builder()
                            .add("Authorization", token)
                            .add("User-Agent", Api.userAgent)
                            .add("Content-Type", "application/json;charset=utf-8")
                            .build()

                        val request = Request.Builder()
                            .url(requrl)
                            .headers(headers)
                            .patch(reqbody)
                            .build()

                        try {
                            val response = httpClient.newCall(request).execute()
                            if (!response.isSuccessful || response.code() > 204) {
                                val msg = if (likedNeko.isNullOrBlank()) "Failed to like" else "Failed to unlike"
                                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                            } else {
                                if (likedNeko.isNullOrBlank()) {
                                    user?.likes?.add(neko.id)
                                    sharedPreferences.edit().putString("user", Api.gson.toJson(user!!)).apply()
                                    Snackbar.make(view, "Liked", Snackbar.LENGTH_SHORT).show()
                                    GlobalScope.launch(Dispatchers.Main) { view.btnLikeNeko.text = "Unlike" }
                                } else {
                                    user?.likes?.remove(neko.id)
                                    sharedPreferences.edit().putString("user", Api.gson.toJson(user!!)).apply()
                                    Snackbar.make(view, "Unliked", Snackbar.LENGTH_SHORT).show()
                                    GlobalScope.launch(Dispatchers.Main) { view.btnLikeNeko.text = "Like" }
                                }
                            }
                            response.close()
                        } catch (e: IOException) {
                            Snackbar.make(view, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Snackbar.make(view, "Login to use this action", Snackbar.LENGTH_LONG).show()
                }
            }

            view.btnFavNeko.onClick {
                if (isLoggedin) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val favedNeko = user?.favorites?.find { id -> id == neko.id }
                        val reqbodystr =
                            if (favedNeko.isNullOrBlank()) "{\"create\": true, \"type\": \"favorite\"}"
                            else "{\"create\": false, \"type\": \"favorite\"}"

                        val reqbody = RequestBody.create(MediaType.parse("application/json"), reqbodystr)
                        val requrl = "https://nekos.moe/api/v1/image/${neko.id}/relationship"

                        val headers = okhttp3.Headers.Builder()
                            .add("Authorization", token)
                            .add("User-Agent", Api.userAgent)
                            .build()

                        val request = Request.Builder()
                            .url(requrl)
                            .headers(headers)
                            .patch(reqbody)
                            .build()

                        try {
                            val response = httpClient.newCall(request).execute()
                            if (!response.isSuccessful || response.code() > 204) {
                                val msg = if (favedNeko.isNullOrBlank()) "Failed to favorite" else "Failed to unfavorite"
                                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
                            } else {
                                if (favedNeko.isNullOrBlank()) {
                                    user?.favorites?.add(neko.id)
                                    sharedPreferences.edit().putString("user", Api.gson.toJson(user!!)).apply()
                                    Snackbar.make(view, "Favorited", Snackbar.LENGTH_SHORT).show()
                                    GlobalScope.launch(Dispatchers.Main) { view.btnFavNeko.text = "Unfavorite" }
                                } else {
                                    user?.favorites?.remove(neko.id)
                                    sharedPreferences.edit().putString("user", Api.gson.toJson(user!!)).apply()
                                    Snackbar.make(view, "Unfavorited", Snackbar.LENGTH_SHORT).show()
                                    GlobalScope.launch(Dispatchers.Main) { view.btnFavNeko.text = "Favorite" }
                                }
                            }
                            response.close()
                        } catch (e: IOException) {
                            Snackbar.make(view, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Snackbar.make(view, "Login to use this action", Snackbar.LENGTH_LONG).show()
                }
            }

            view.btnCloseNeko.onClick {
                nekoDialog.dismiss()
            }

            view.btnShareNeko.onClick {
                if (!connected || !isConnected(itemView.context)) return@onClick

                picasso.load(neko.getImageUrl()).into {
                    onFailed { e, _ ->
                        Snackbar.make(view, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG).show()
                    }
                    onLoaded { bitmap, _ ->
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "image/jpeg"

                        val sdCard = itemView.context.getExternalFilesDir(Context.STORAGE_SERVICE) ?: return@onLoaded
                        val dir = File(sdCard.absolutePath + "/share")
                        if (dir.exists().not())
                            dir.mkdirs()

                        file = File(dir, "share-${neko.id}.jpg")

                        try {
                            file.createNewFile()
                            val fos = FileOutputStream(file)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                            fos.flush()
                            fos.close()
                        } catch (e: IOException) {
                            val message = e.message ?: "Unable to save/share image"
                            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
                        }

                        val uri = FileProvider.getUriForFile(view.context, view.context.applicationContext.packageName + ".NekoFileProvider", file)
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                        intent.putExtra(Intent.EXTRA_TEXT, "Artist: ${neko.artist}\n" +
                                "Tags: ${neko.tags.subList(0, 5).joinToString(", ")}\n" +
                                "#catgirls #nekos\n" +
                                "https://nekos.moe/post/${neko.id}")

                        startActivityForResult(view.context as MainActivity, Intent.createChooser(intent,"Share Image"),997, null)

                        file.deleteOnExit()
                    }
                }
            }

            view.btnSaveNeko.onClick {
                if (!connected || ! isConnected(view.context)) return@onClick

                if (!hasPermissions(view.context, permissions)) {
                    ActivityCompat.requestPermissions(view.context as MainActivity, permissions, 999)
                } else {
                    downloadAndSave(neko, view)
                }
            }

            nekoDialog.show()
        }
    }
}

private fun downloadAndSave(neko: Neko, view: View) {
    picasso.load(neko.getImageUrl()).into {
        onFailed { e, _ ->
            Snackbar.make(view, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG).show()
        }
        onLoaded { bitmap, _ ->
            val fos: OutputStream

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = view.context.contentResolver;
                val values = ContentValues()
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, neko.id)
                values.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Nekos")
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@onLoaded
                fos = resolver.openOutputStream(uri) ?: return@onLoaded
            } else {
                @Suppress("DEPRECATION")
                val mediaStorageDir = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)}${File.separator}Nekos${File.separator}")
                if (!mediaStorageDir.exists()) mediaStorageDir.mkdirs()
                val file = File(mediaStorageDir, "${neko.id}.jpg")
                file.createNewFile()
                fos = FileOutputStream(file)
            }

            try {
                val saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                fos.flush()
                fos.close()
                if (saved) Snackbar.make(view, "Saved as ${neko.id}.jpeg", Snackbar.LENGTH_SHORT).show()
                else Snackbar.make(view, "Could not save image", Snackbar.LENGTH_SHORT).show()
            } catch (e: IOException) {
                val message = e.message ?: "Unable to save/share image"
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}