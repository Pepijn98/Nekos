package xyz.kurozero.nekos

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.core.Json
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import kotlinx.android.synthetic.main.activity_neko_main.*
import kotlinx.android.synthetic.main.nekos_entry.view.*
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.FileOutputStream

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

        val inflator = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val imageView = inflator.inflate(R.layout.nekos_entry, null)

        imageView.imgNeko.foreground = main.getDrawable(R.drawable.border)

        val radius = 50
        val margin = 0
        val transformation = RoundedCornersTransformation(radius, margin)

        Picasso.get()
                .load("https://nekos.moe/thumbnail/${neko.id}")
                .transform(transformation)
                .fit()
                .into(imageView.imgNeko)

        imageView.setOnClickListener {
            val alertadd = AlertDialog.Builder(main)
            val factory = LayoutInflater.from(main)

            val view = factory.inflate(R.layout.alert_dialog, null)
            alertadd.setView(view)
            Picasso.get().load("https://nekos.moe/image/${neko.id}").into(view.findViewById<ImageView>(R.id.dialog_imageview))

            alertadd.setNeutralButton("Save", { _, _ ->
                if (!hasPermissions(main, main.permissions)) {
                    ActivityCompat.requestPermissions(main, main.permissions, 999)
                } else {
                    doAsync {
                        downloadAndSave(neko)
                    }
                }
            })

            alertadd.setPositiveButton("Close", { dialog, _ -> dialog.dismiss() })

            alertadd.show()
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
                Snackbar.make(main.findViewById<GridView>(R.id.nekoImages), "Saved as ${neko.id}.jpeg", Snackbar.LENGTH_SHORT).show()
                fileOutput.close()
            } else if (err != null) {
                val msg =
                        Json(String(err.errorData)).obj().get("message") as String?
                                ?: err.message
                                ?: "Something went wrong"
                Snackbar.make(main.nekoImages, msg, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}