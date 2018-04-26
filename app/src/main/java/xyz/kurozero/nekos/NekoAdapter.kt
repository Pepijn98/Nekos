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
import org.jetbrains.anko.*

class NekoAdapter(private val context: Context, private var nekos: Nekos) : BaseAdapter() {
    private val main = context as NekoMain

    override fun getCount(): Int {
        return nekos.images.size
    }

    override fun getItem(position: Int): Any {
        return nekos.images[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val neko = this.nekos.images[position]
        val thumbnailImage = "https://nekos.moe/thumbnail/${neko.id}"
        val fullImage = "https://nekos.moe/image/${neko.id}"

        val inflator = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val imageView = inflator.inflate(R.layout.nekos_entry, null)

        val cardView = imageView.nekoCardView
        cardView.preventCornerOverlap = true
        cardView.radius = 50f

        if (main.nsfw) {
            cardView.foreground = main.getDrawable(R.drawable.border_nsfw)
        } else {
            cardView.foreground = main.getDrawable(R.drawable.border_sfw)
        }

        picasso.load(thumbnailImage)
                .into(imageView.imgNeko.toProgressTarget())

        imageView.setOnClickListener {
            val builder = AlertDialog.Builder(main)
            val nekoDialog = builder.create()
            val factory = LayoutInflater.from(main)

            val view = factory.inflate(R.layout.view_neko_dialog, null)

            if (main.nsfw) {
                view.btnSaveNeko?.setTextColor(main.getColor(R.color.nsfw_colorPrimary))
                view.btnShareNeko?.setTextColor(main.getColor(R.color.nsfw_colorPrimary))
                view.btnCloseNeko?.setTextColor(main.getColor(R.color.nsfw_colorPrimary))
            } else {
                view.btnSaveNeko?.setTextColor(main.getColor(R.color.colorPrimary))
                view.btnShareNeko?.setTextColor(main.getColor(R.color.colorPrimary))
                view.btnCloseNeko?.setTextColor(main.getColor(R.color.colorPrimary))
            }

            nekoDialog.setView(view)
            picasso.load(fullImage).into(view.fullNekoImg.toProgressTarget())

            view.btnSaveNeko.setOnClickListener {
                if (!hasPermissions(main, main.permissions)) {
                    ActivityCompat.requestPermissions(main, main.permissions, 999)
                } else {
                    doAsync {
                        downloadAndSave(neko)
                    }
                }
            }

            view.btnCloseNeko.setOnClickListener {
                nekoDialog.cancel()
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

        return imageView
    }

    private fun downloadAndSave(neko: Neko) {
        val mediaStorageDir = File(Environment.getExternalStorageDirectory().toString() + "/Nekos/")
        if (!mediaStorageDir.exists()) mediaStorageDir.mkdirs()
        val file = File(mediaStorageDir, "${neko.id}.jpeg")

        Fuel.download("https://nekos.moe/image/${neko.id}").destination { response, _ ->
            Log.w("Response", response.toString())
            file
        }.response { request, response, result ->
            Log.w("Request", request.toString())
            Log.w("Response", response.toString())
            Log.w("Result", result.toString())

            val (data, err) = result
            if (data != null) {
                val fileOutput = FileOutputStream(file)
                fileOutput.write(data, 0, data.size)

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