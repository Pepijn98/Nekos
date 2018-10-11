package xyz.kurozero.nekosmoe

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v7.widget.RecyclerView
import android.text.method.ScrollingMovementMethod
import com.stfalcon.frescoimageviewer.ImageViewer
import com.github.kittinunf.fuel.Fuel
import com.google.firebase.analytics.FirebaseAnalytics
import com.hendraanggrian.pikasso.picasso
import com.hendraanggrian.pikasso.*
import io.sentry.Sentry
import io.sentry.event.BreadcrumbBuilder
import io.sentry.event.UserBuilder
import org.jetbrains.anko.*
import kotlinx.android.synthetic.main.nekos_entry.view.*
import kotlinx.android.synthetic.main.view_neko_dialog.view.*
import kotlinx.serialization.json.JSON
import java.io.File
import java.io.FileOutputStream
import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.IOException

lateinit var file: File

class NekoAdapter(private val context: Context, private var nekos: Nekos) : RecyclerView.Adapter<NekoViewHolder>() {

    override fun getItemCount(): Int {
        return nekos.images.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NekoViewHolder {
        return NekoViewHolder(LayoutInflater.from(context).inflate(R.layout.nekos_entry, parent, false), parent)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: NekoViewHolder, position: Int) {
        val neko = nekos.images[position]
        val thumbnailImage = "https://nekos.moe/thumbnail/${neko.id}"
        val fullImage = "https://nekos.moe/image/${neko.id}"

        holder.imgNeko.foreground =
                if (neko.nsfw) context.getDrawable(R.drawable.border_nsfw)
                else context.getDrawable(R.drawable.border_sfw)

        picasso.load(thumbnailImage)
                .square()
                .rounded(8f, 0f)
                .into(holder.imgNeko.toProgressTarget())

        holder.imgNeko.setOnClickListener {
            if (!connected || !isConnected(context)) {
                showSnackbar(it, context, "No network connection", Snackbar.LENGTH_SHORT)
                return@setOnClickListener
            }

            // Analytics
            bundle.clear()
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "nekos_view_image")
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "View Neko ${neko.id}")
            firebaseAnalytics.logEvent("view_image", bundle)

            val view = LayoutInflater.from(context).inflate(R.layout.view_neko_dialog, holder.parent, false)
            val nekoDialog = AlertDialog.Builder(context)
                    .setView(view)
                    .create()

            view.tvUploader.text = context.getString(R.string.uploaded_by, neko.uploader.username)
            view.tvApproved.text = context.getString(R.string.approved_by, neko.approver?.username ?: "-")
            view.tvNekoFavorites.text = context.getString(R.string.neko_favorites, neko.favorites)
            view.tvNekoLikes.text = context.getString(R.string.neko_likes, neko.likes)
            view.tvArtist.text = context.getString(R.string.neko_artist, neko.artist)
            view.tvTags.text = context.getString(R.string.neko_tags, neko.tags.joinToString(", "))
            view.tvTags.movementMethod = ScrollingMovementMethod()

            picasso.load(fullImage).into(view.fullNekoImg.toProgressTarget())

            view.fullNekoImg.setOnClickListener { _ ->
                ImageViewer.Builder(context, arrayOf(fullImage)).show()
            }

            if (isLoggedin) {
                if (user == null)
                    return@setOnClickListener

                val liked = user?.likes?.find { id -> id == neko.id }
                val faved = user?.favorites?.find { id -> id == neko.id }
                view.btnLikeNeko.text = if (liked.isNullOrBlank()) "Like" else "Unlike"
                view.btnFavNeko.text = if (faved.isNullOrBlank()) "Favorite" else "Unfavorite"
            }

                val token = sharedPreferences.getString("token", "")
                view.btnLikeNeko.setOnClickListener { _ ->
                    if (isLoggedin) {
                        doAsync {
                            val likedNeko = user?.likes?.find { id -> id == neko.id }
                            val reqbodystr =
                                    if (likedNeko.isNullOrBlank()) "{\"create\": true, \"type\": \"like\"}"
                                    else "{\"create\": false, \"type\": \"like\"}"

                            val reqbody = RequestBody.create(MediaType.parse("application/json"), reqbodystr)
                            val requrl = "https://nekos.moe/api/v1/image/${neko.id}/relationship"

                            val headers = okhttp3.Headers.Builder()
                                    .add("Authorization", token)
                                    .add("User-Agent", userAgent)
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
                                    showSnackbar(view, context, msg, Snackbar.LENGTH_SHORT)
                                } else {
                                    if (likedNeko.isNullOrBlank()) {
                                        user?.likes?.add(neko.id)
                                        sharedPreferences.edit().putString("user", JSON.stringify(user!!)).apply()
                                        showSnackbar(view, context, "Liked", Snackbar.LENGTH_SHORT)
                                        uiThread { _ -> view.btnLikeNeko.text = "Unlike" }
                                    } else {
                                        user?.likes?.remove(neko.id)
                                        sharedPreferences.edit().putString("user", JSON.stringify(user!!)).apply()
                                        showSnackbar(view, context, "Unliked", Snackbar.LENGTH_SHORT)
                                        uiThread { _ -> view.btnLikeNeko.text = "Like" }
                                    }
                                }
                                response.close()
                            } catch (e: IOException) {
                                showSnackbar(view, context, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG)
                                if (isLoggedin)
                                    Sentry.getContext().user = UserBuilder().setUsername(user?.username ?: "Unkown user").setId(user?.id ?: "0").build()
                                Sentry.getContext().recordBreadcrumb(BreadcrumbBuilder().setMessage("Failed to update like relationship").build())
                                Sentry.getContext().addExtra("request-body", reqbodystr)
                                Sentry.getContext().addExtra("request-url", requrl)
                                Sentry.getContext().addTag("fuel-http-request", "true")
                                Sentry.capture(e)
                                Sentry.clearContext()
                            }
                        }
                    } else {
                        showSnackbar(view, context, "Login to use this action", Snackbar.LENGTH_LONG)
                    }
                }

                view.btnFavNeko.setOnClickListener { _ ->
                    if (isLoggedin) {
                        doAsync {
                            val favedNeko = user?.favorites?.find { id -> id == neko.id }
                            val reqbodystr =
                                    if (favedNeko.isNullOrBlank()) "{\"create\": true, \"type\": \"favorite\"}"
                                    else "{\"create\": false, \"type\": \"favorite\"}"

                            val reqbody = RequestBody.create(MediaType.parse("application/json"), reqbodystr)
                            val requrl = "https://nekos.moe/api/v1/image/${neko.id}/relationship"

                            val headers = okhttp3.Headers.Builder()
                                    .add("Authorization", token)
                                    .add("User-Agent", userAgent)
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
                                    showSnackbar(view, context, msg, Snackbar.LENGTH_SHORT)
                                } else {
                                    if (favedNeko.isNullOrBlank()) {
                                        user?.favorites?.add(neko.id)
                                        sharedPreferences.edit().putString("user", JSON.stringify(user!!)).apply()
                                        showSnackbar(view, context, "Favorited", Snackbar.LENGTH_SHORT)
                                        uiThread { _ -> view.btnFavNeko.text = "Unfavorite" }
                                    } else {
                                        user?.favorites?.remove(neko.id)
                                        sharedPreferences.edit().putString("user", JSON.stringify(user!!)).apply()
                                        showSnackbar(view, context, "Unfavorited", Snackbar.LENGTH_SHORT)
                                        uiThread { _ -> view.btnFavNeko.text = "Favorite" }
                                    }
                                }
                                response.close()
                            } catch (e: IOException) {
                                showSnackbar(view, context, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG)
                                if (isLoggedin)
                                    Sentry.getContext().user = UserBuilder().setUsername(user?.username ?: "Unkown user").setId(user?.id ?: "0").build()
                                Sentry.getContext().recordBreadcrumb(BreadcrumbBuilder().setMessage("Failed to update favorite relationship").build())
                                Sentry.getContext().addExtra("request-body", reqbodystr)
                                Sentry.getContext().addExtra("request-url", requrl)
                                Sentry.getContext().addTag("fuel-http-request", "true")
                                Sentry.capture(e)
                                Sentry.clearContext()
                            }
                        }
                    } else {
                        showSnackbar(view, context, "Login to use this action", Snackbar.LENGTH_LONG)
                    }
                }

            view.btnSaveNeko.setOnClickListener { _ ->
                if (!connected || !isConnected(context)) {
                    showSnackbar(view, context, "No network connection", Snackbar.LENGTH_LONG)
                } else {
                    if (!hasPermissions(context, permissions)) {
                        ActivityCompat.requestPermissions(context as NekoMain, permissions, 999)
                    } else {
                        // Analytics
                        bundle.clear()
                        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "nekos_save_image")
                        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Save Neko ${neko.id}")
                        firebaseAnalytics.logEvent("save_image", bundle)

                        doAsync {
                            downloadAndSave(neko, view)
                        }
                    }
                }
            }

            view.btnCloseNeko.setOnClickListener { _ ->
                nekoDialog.dismiss()
            }

            view.btnShareNeko.setOnClickListener { _ ->
                if (!connected || !isConnected(context)) {
                    showSnackbar(view, context, "No network connection", Snackbar.LENGTH_LONG)
                } else {
                    picasso.load(fullImage).into {
                        onFailed { e, _ ->
                            showSnackbar(view, context, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG)
                        }
                        onLoaded { bitmap, _ ->
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.type = "image/png"

                            val bytes = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)

                            val sdCard = Environment.getExternalStorageDirectory()
                            val dir = File(sdCard.absolutePath + "/Nekos")
                            if (dir.exists().not())
                                dir.mkdirs()

                            file = File(dir, "share-${neko.id}.png")

                            try {
                                file.createNewFile()
                                val fo = FileOutputStream(file)
                                fo.write(bytes.toByteArray())
                                fo.flush()
                                fo.close()
                            } catch (e: IOException) {
                                val message = e.message ?: "Unable to save/share image"
                                showSnackbar(view, context, message, Snackbar.LENGTH_LONG)
                            }

                            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///" + file.path))
                            intent.putExtra(Intent.EXTRA_TEXT, "Artist: ${neko.artist}\n" +
                                    "Tags: ${neko.tags.subList(0, 5).joinToString(", ")}\n" +
                                    "#catgirls #nekos\n" +
                                    "https://nekos.moe/post/${neko.id}")

                            startActivityForResult(context as NekoMain, Intent.createChooser(intent,"Share Image"),997, null)

                            file.deleteOnExit()
                        }
                    }
                }
            }

            nekoDialog.show()
        }
    }

    private fun downloadAndSave(neko: Neko, view: View) {
        val mediaStorageDir = File(Environment.getExternalStorageDirectory().toString() + "/Nekos/")
        if (!mediaStorageDir.exists()) mediaStorageDir.mkdirs()
        val file = File(mediaStorageDir, "${neko.id}.jpeg")

        Fuel.download("https://nekos.moe/image/${neko.id}").destination { response, _ ->
            response.toString()
            file
        }.response { _, _, result ->
            val (data, err) = result
            if (data != null) {
                val fileOutput = FileOutputStream(file)
                fileOutput.write(data, 0, data.size)

                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                showSnackbar(view, context, "Saved as ${neko.id}.jpeg", Snackbar.LENGTH_SHORT)
                fileOutput.close()
            } else if (err != null) {
                when (err.response.statusCode) {
                    429 -> {
                        showSnackbar(view, context, "Too many requests, please wait a few seconds", Snackbar.LENGTH_LONG)
                    }
                    else -> {
                        val nekoException = NekoException.Deserializer().deserialize(err.errorData)
                        val msg = nekoException?.message ?: err.message ?: "Something went wrong"
                        showSnackbar(view, context, msg, Snackbar.LENGTH_LONG)
                        Sentry.capture(err)
                    }
                }
            }
        }
    }
}

class NekoViewHolder(view: View, val parent: ViewGroup) : RecyclerView.ViewHolder(view) {
    val imgNeko: ImageView = view.imgNeko
}