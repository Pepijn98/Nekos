package xyz.kurozero.nekos

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.provider.MediaStore
import android.support.v7.widget.RecyclerView
import com.stfalcon.frescoimageviewer.ImageViewer
import com.github.kittinunf.fuel.Fuel
import com.hendraanggrian.pikasso.picasso
import com.hendraanggrian.pikasso.*
import io.sentry.Sentry
import org.jetbrains.anko.*
import org.jetbrains.anko.design.longSnackbar
import org.jetbrains.anko.design.snackbar
import kotlinx.android.synthetic.main.nekos_entry.view.*
import kotlinx.android.synthetic.main.view_neko_dialog.view.*
import kotlinx.serialization.json.JSON
import java.io.File
import java.io.FileOutputStream
import okhttp3.*

class NekoAdapter(private val context: Context, private var nekos: Nekos) : RecyclerView.Adapter<NekoViewHolder>() {
    private val main = context as NekoMain

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
                if (neko.nsfw) main.getDrawable(R.drawable.border_nsfw)
                else main.getDrawable(R.drawable.border_sfw)

        picasso.load(thumbnailImage)
                .square()
                .rounded(32f, 0f)
                .into(holder.imgNeko.toProgressTarget())

        holder.imgNeko.setOnClickListener {
            val view = LayoutInflater.from(main).inflate(R.layout.view_neko_dialog, holder.parent, false)
            val nekoDialog = AlertDialog.Builder(main)
                    .setView(view)
                    .create()

            view.tvUploader.text = main.getString(R.string.uploaded_by, neko.uploader.username)
            view.tvApproved.text = main.getString(R.string.approved_by, neko.approver?.username ?: "-")
            view.tvFavorites.text = main.getString(R.string.neko_favorites, neko.favorites)
            view.tvLikes.text = main.getString(R.string.neko_likes, neko.likes)
            view.tvArtist.text = main.getString(R.string.neko_artist, neko.artist)
            view.tvTags.text = main.getString(R.string.neko_tags, neko.tags.joinToString(", "))

            picasso.load(fullImage).into(view.fullNekoImg.toProgressTarget())

            view.fullNekoImg.setOnClickListener {
                ImageViewer.Builder(context, arrayOf(fullImage)).show()
            }

            if (main.isLoggedin) {
                val liked = main.user!!.likes.find { it == neko.id }
                val faved = main.user!!.favorites.find { it == neko.id }
                view.btnLikeNeko.text = if (liked.isNullOrBlank()) "Like" else "Unlike"
                view.btnFavNeko.text = if (faved.isNullOrBlank()) "Favorite" else "Unfavorite"
            }

            if (!main.connected || !isConnected(main)) {
                snackbar(view, "No network connection")
            } else {
                val token = main.sharedPreferences.getString("token", "")
                view.btnLikeNeko.setOnClickListener {
                    if (main.isLoggedin) {
                        doAsync {
                            val likedNeko = main.user!!.likes.find { it == neko.id }
                            val reqbodystr =
                                    if (likedNeko.isNullOrBlank()) "{\"create\": true, \"type\": \"like\"}"
                                    else "{\"create\": false, \"type\": \"like\"}"

                            val reqbody = RequestBody.create(MediaType.parse("application/json"), reqbodystr)

                            val headers = okhttp3.Headers.Builder()
                                    .add("Authorization", token)
                                    .add("User-Agent", userAgent)
                                    .add("Content-Type", "application/json;charset=utf-8")
                                    .build()

                            val request = Request.Builder()
                                    .url("https://nekos.moe/api/v1/image/${neko.id}/relationship")
                                    .headers(headers)
                                    .patch(reqbody)
                                    .build()

                            val response = main.httpClient.newCall(request).execute()
                            if (!response.isSuccessful && response.code() > 204) {
                                val msg = if (likedNeko.isNullOrBlank()) "Failed to like" else "Failed to unlike"
                                snackbar(view, msg)
                            } else {
                                if (likedNeko.isNullOrBlank()) {
                                    main.user!!.likes.add(neko.id)
                                    main.sharedPreferences.edit().putString("user", JSON.stringify(main.user!!)).apply()
                                    snackbar(view, "Liked")
                                    uiThread { view.btnLikeNeko.text = "Unlike" }
                                } else {
                                    main.user!!.likes.remove(neko.id)
                                    main.sharedPreferences.edit().putString("user", JSON.stringify(main.user!!)).apply()
                                    snackbar(view, "Unliked")
                                    uiThread { view.btnLikeNeko.text = "Like" }
                                }
                            }
                            response.close()
                        }
                    } else {
                        longSnackbar(view, "You need to be logged in to use this action")
                    }
                }

                view.btnFavNeko.setOnClickListener {
                    if (main.isLoggedin) {
                        doAsync {
                            val favedNeko = main.user!!.favorites.find { it == neko.id }
                            val reqbodystr =
                                    if (favedNeko.isNullOrBlank()) "{\"create\": true, \"type\": \"favorite\"}"
                                    else "{\"create\": false, \"type\": \"favorite\"}"

                            val reqbody = RequestBody.create(MediaType.parse("application/json"), reqbodystr)

                            val headers = okhttp3.Headers.Builder()
                                    .add("Authorization", token)
                                    .add("User-Agent", userAgent)
                                    .build()

                            val request = Request.Builder()
                                    .url("https://nekos.moe/api/v1/image/${neko.id}/relationship")
                                    .headers(headers)
                                    .patch(reqbody)
                                    .build()

                            val response = main.httpClient.newCall(request).execute()
                            if (!response.isSuccessful && response.code() > 204) {
                                val msg = if (favedNeko.isNullOrBlank()) "Failed to favorite" else "Failed to unfavorite"
                                snackbar(view, msg)
                            } else {
                                if (favedNeko.isNullOrBlank()) {
                                    main.user!!.favorites.add(neko.id)
                                    main.sharedPreferences.edit().putString("user", JSON.stringify(main.user!!)).apply()
                                    snackbar(view, "Favorited")
                                    uiThread { view.btnFavNeko.text = "Unfavorite" }
                                } else {
                                    main.user!!.favorites.remove(neko.id)
                                    main.sharedPreferences.edit().putString("user", JSON.stringify(main.user!!)).apply()
                                    snackbar(view, "Unfavorited")
                                    uiThread { view.btnFavNeko.text = "Favorite" }
                                }
                            }
                            response.close()
                        }
                    } else {
                        longSnackbar(view, "You need to be logged in to use this action")
                    }
                }
            }

            view.btnSaveNeko.setOnClickListener {
                if (!main.connected || !isConnected(main)) {
                    snackbar(view, "No network connection")
                } else {
                    if (!hasPermissions(main, main.permissions)) {
                        ActivityCompat.requestPermissions(main, main.permissions, 999)
                    } else {
                        doAsync {
                            downloadAndSave(neko, view)
                        }
                    }
                }
            }

            view.btnCloseNeko.setOnClickListener {
                nekoDialog.dismiss()
            }

            view.btnShareNeko.setOnClickListener {
                if (!main.connected || !isConnected(main)) {
                    snackbar(view, "No network connection")
                } else {
                    picasso.load(fullImage).into {
                        onFailed { e, _ ->
                            snackbar(view, e.message ?: "Something went wrong")
                        }
                        onLoaded { bitmap, _ ->
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.putExtra(Intent.EXTRA_TEXT, "Artist: ${neko.artist}\n" +
                                    "Tags: ${neko.tags.subList(0, 5).joinToString(", ")}\n" +
                                    "#catgirls #nekos\n" +
                                    "https://nekos.moe/post/${neko.id}")
                            val path = MediaStore.Images.Media.insertImage(main.contentResolver, bitmap, "", null)
                            val uri = Uri.parse(path)

                            intent.putExtra(Intent.EXTRA_STREAM, uri)
                            intent.type = "image/*"
                            main.startActivity(Intent.createChooser(intent, "Share image via..."))
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

                main.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                snackbar(view, "Saved as ${neko.id}.jpeg")
                fileOutput.close()
            } else if (err != null) {
                when (err.response.statusCode) {
                    429 -> {
                        longSnackbar(view, "Too many requests, please wait a few seconds")
                    }
                    else -> {
                        val nekoException = NekoException.Deserializer().deserialize(err.errorData)
                        val msg = nekoException?.message ?: err.message ?: "Something went wrong"
                        longSnackbar(view, msg)
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