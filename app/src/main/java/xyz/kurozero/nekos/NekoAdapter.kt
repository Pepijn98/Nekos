package xyz.kurozero.nekos

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.core.Json
import com.hendraanggrian.pikasso.picasso
import com.hendraanggrian.pikasso.*
import kotlinx.android.synthetic.main.nekos_entry.view.*
import kotlinx.android.synthetic.main.view_neko_dialog.view.*
import java.io.File
import java.io.FileOutputStream
import android.provider.MediaStore
import android.support.v7.widget.RecyclerView
import com.stfalcon.frescoimageviewer.ImageViewer
import org.jetbrains.anko.*

class NekoAdapter(private val context: Context, private var nekos: Nekos) : RecyclerView.Adapter<NekoViewHolder>() {
    private val main = context as NekoMain

    // Gets the number of animals in the list
    override fun getItemCount(): Int {
        return nekos.images.size
    }

    // Inflates the item views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NekoViewHolder {
        return NekoViewHolder(LayoutInflater.from(context).inflate(R.layout.nekos_entry, parent, false), parent)
    }

    // Binds each animal in the ArrayList to a view
    override fun onBindViewHolder(holder: NekoViewHolder, position: Int) {
        val neko = nekos.images[position]
        val thumbnailImage = "https://nekos.moe/thumbnail/${neko.id}"
        val fullImage = "https://nekos.moe/image/${neko.id}"

        if (neko.nsfw) {
            holder.imgNeko.foreground = main.getDrawable(R.drawable.border_nsfw)
        } else {
            holder.imgNeko.foreground = main.getDrawable(R.drawable.border_sfw)
        }

        picasso.load(thumbnailImage)
                .square()
                .rounded(32f, 0f)
                .into(holder.imgNeko.toProgressTarget())

        holder.imgNeko.setOnClickListener {
            val builder = AlertDialog.Builder(main)
            val nekoDialog = builder.create()
            val view = LayoutInflater.from(main).inflate(R.layout.view_neko_dialog, holder.parent, false)
            nekoDialog.setView(view)

            view.tvUploader.text = main.getString(R.string.uploaded_by, neko.uploader.username)
            view.tvApproved.text = main.getString(R.string.approved_by, neko.approver.username)
            view.tvFavorites.text = main.getString(R.string.neko_favorites, neko.favorites)
            view.tvLikes.text = main.getString(R.string.neko_likes, neko.likes)
            view.tvArtist.text = main.getString(R.string.neko_artist, neko.artist)
            view.tvTags.text = main.getString(R.string.neko_tags, neko.tags.joinToString(", "))

            picasso.load(fullImage).into(view.fullNekoImg.toProgressTarget())

            view.fullNekoImg.setOnClickListener {
                ImageViewer.Builder(context, arrayOf(fullImage)).show()
            }

            view.btnSaveNeko.setOnClickListener {
                if (!main.connected || !isConnected(main)) {
                    main.toast("No network connection")
                } else {
                    if (!hasPermissions(main, main.permissions)) {
                        ActivityCompat.requestPermissions(main, main.permissions, 999)
                    } else {
                        doAsync {
                            downloadAndSave(neko, nekoDialog)
                        }
                    }
                }
            }

            view.btnCloseNeko.setOnClickListener {
                nekoDialog.dismiss()
            }

            view.btnShareNeko.setOnClickListener {
                if (!main.connected || !isConnected(main)) {
                    main.toast("No network connection")
                } else {
                    picasso.load(fullImage).into {
                        onFailed { e, _ ->
                            main.toast(e.message ?: "Something went wrong")
                        }
                        onLoaded { bitmap, _ ->
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.putExtra(Intent.EXTRA_TEXT, "Want to see more cute nekos like this? Visit https://nekos.moe or download the the android app from https://github.com/KurozeroPB/Nekos")
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

    private fun downloadAndSave(neko: Neko, dialog: AlertDialog) {
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
                dialog.dismiss()

                main.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
                main.toast("Saved as ${neko.id}.jpeg")
                fileOutput.close()
            } else if (err != null) {
                val msg =
                        Json(String(err.errorData)).obj().get("message") as String?
                                ?: err.message
                                ?: "Something went wrong"
                main.toast(msg)
            }
        }
    }
}

class NekoViewHolder (view: View, val parent: ViewGroup) : RecyclerView.ViewHolder(view) {
    val imgNeko: ImageView = view.imgNeko
}