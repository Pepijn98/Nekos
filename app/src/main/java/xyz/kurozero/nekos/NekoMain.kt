package xyz.kurozero.nekos

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.*
import android.view.LayoutInflater
import android.view.ViewGroup
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.util.Log
import java.io.FileOutputStream
import java.io.File
import com.github.kittinunf.fuel.android.extension.responseJson
import com.squareup.picasso.Picasso
import com.google.gson.Gson
import com.bumptech.glide.Glide
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
import org.jetbrains.anko.doAsync
import kotlinx.android.synthetic.main.activity_neko_main.*
import kotlinx.android.synthetic.main.nekos_entry.view.*


class NekoMain : AppCompatActivity() {

    private val url = "https://nekos.moe/api/v1"
    private var toSkip = 0
    private var isNew = true
    private var nsfw = false
    private var nekos: Nekos? = null
    private var adapter: NekoAdapter? = null
    private var nekosList = ArrayList<Neko>()
    private var optionsMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_neko_main)
        setSupportActionBar(toolbar)

        val permissions = arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.INTERNET
        )

        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 999)
        }

        val navNext = navigationView.menu.findItem(R.id.navigation_next)
        val navRand = navigationView.menu.findItem(R.id.navigation_random)
        val navPrev = navigationView.menu.findItem(R.id.navigation_previous)
        navNext.setOnMenuItemClickListener {
            requestNeko(true)
            true
        }

        navRand.setOnMenuItemClickListener {
            Snackbar.make(it.actionView, "Soon™", Snackbar.LENGTH_INDEFINITE)
            true
        }

        navPrev.setOnMenuItemClickListener {
            requestNeko(false)
            true
        }

        FuelManager.instance.basePath = url
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to "NekosApp/v0.1.0 (https://github.com/KurozeroPB/nekos-app)")
        requestNeko(true)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            999 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(
                            findViewById<ImageView>(R.id.nekoImages),
                            "The app can now save images to your storage",
                            Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    Snackbar.make(
                            findViewById<ImageView>(R.id.nekoImages),
                            "The app was not allowed to write in your storage",
                            Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val loginOut = menu.findItem(R.id.login_out)
        val switchNsfw = menu.findItem(R.id.switch_nsfw)
        loginOut.title = getString(R.string.login_out, "Login")
        switchNsfw.title = getString(R.string.switch_nsfw, "Enable")
        optionsMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.login_out -> {
                showSoonTMAlert()
                true
            }
            R.id.view_account -> {
                showSoonTMAlert()
                true
            }
            R.id.switch_nsfw -> {
                val switchNsfw = optionsMenu?.findItem(R.id.switch_nsfw)

                isNew = true
                nekosList.clear()
                requestNeko(true)

                if (!nsfw) {
                    nsfw = true
                    switchNsfw?.title = getString(R.string.switch_nsfw, "Disable")
                    Snackbar.make(nekoImages, "Enabled nsfw images", Snackbar.LENGTH_SHORT).show()
                } else {
                    nsfw = false
                    switchNsfw?.title = getString(R.string.switch_nsfw, "Enable")
                    Snackbar.make(nekoImages, "Disabled nsfw images", Snackbar.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("InflateParams")
    private fun showSoonTMAlert() {
        val alertadd = AlertDialog.Builder(this@NekoMain)
        val factory = LayoutInflater.from(this@NekoMain)

        val view = factory.inflate(R.layout.alert_dialog, null)
        alertadd.setView(view)
        alertadd.setNeutralButton("Soon™", null)

        Glide.with(this).load(R.drawable.placeholder).into(view.findViewById(R.id.dialog_imageview))

        alertadd.show()
    }

    private fun requestNeko(next: Boolean) {
        doAsync {

            toSkip = if (isNew) {
                0
            } else {
                if (next) toSkip + 10 else toSkip - 10
            }

            Log.w("Skipping", toSkip.toString())

            Fuel.post("/images/search")
                    .header(mapOf("Content-Type" to "application/json"))
                    .body("{\"nsfw\": $nsfw, \"skip\": $toSkip, \"limit\": 10}")
                    .responseJson { _, _, result ->
                        val (neko, error) = result
                        if (neko != null) {
                            val nekos = checkNotNull(Nekos.Deserializer().deserialize(neko.content))
                            nekosList.clear()
                            nekosList.addAll(nekos.images)

                            val ctx = this@NekoMain
                            ctx.nekos = nekos
                            adapter = NekoAdapter(ctx, nekosList)
                            nekoImages.adapter = adapter
                            // nekos.images.find { s -> s.id == "" }
                        } else if (error != null) {
                            val msg = error.message ?: "Something went wrong"
                            Snackbar.make(nekoImages, msg, Snackbar.LENGTH_LONG).show()
                        }
                    }
            isNew = false
        }
    }
}

class NekoAdapter(context: Context, private var nekosList: ArrayList<Neko>) : BaseAdapter() {
    private var context: Context? = context
    private val main = context as NekoMain

    override fun getCount(): Int {
        return nekosList.size
    }

    override fun getItem(position: Int): Any {
        return nekosList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val neko = this.nekosList[position]

        val inflator = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
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
                val permissions = arrayOf(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.INTERNET
                )

                if (!hasPermissions(main, permissions)) {
                    ActivityCompat.requestPermissions(main, permissions, 999)
                } else {
                    doAsync {
                        downloadAndSave(neko, main)
                    }
                }
            })

            alertadd.setPositiveButton("Close", { dialog, _ -> dialog.dismiss() })

            alertadd.show()
        }

        return imageView
    }
}

private fun downloadAndSave(neko: Neko, main: NekoMain) {
    val mediaStorageDir = File(Environment.getExternalStorageDirectory().toString() + "/Nekos/")
    if (!mediaStorageDir.exists()) mediaStorageDir.mkdirs()
    var file: File? = null

    Fuel.download("https://nekos.moe/image/${neko.id}").destination { _, _ ->
        file = File(mediaStorageDir, "${neko.id}.jpeg")
        file!!
    }.response { _, response, _ ->
        val fileOutput = FileOutputStream(file)
        fileOutput.write(response.data, 0, response.data.size)
        fileOutput.close()

        main.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
        Snackbar.make(main.findViewById<ImageView>(R.id.nekoImages), "Saved as ${neko.id}.jpeg", Snackbar.LENGTH_INDEFINITE).show()
    }
}

private fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && context != null) {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
    }
    return true
}

data class Nekos(
        val images: ArrayList<Neko>
) {
    class Deserializer : ResponseDeserializable<Nekos> {
        override fun deserialize(content: String): Nekos? = Gson().fromJson(content, Nekos::class.java)
    }
}

data class Neko(
        val id: String,
        val originalHash: String,
        val uploader: NekoUploader,
        val approver: NekoApprover,
        val nsfw: Boolean,
        val artist: String,
        val tags: ArrayList<String>,
        val comments: ArrayList<String>,
        val createdAt: String,
        val likes: Int,
        val favorites: Int
)

data class NekoUploader(
        val id: String,
        val username: String
)

data class NekoApprover(
        val id: String,
        val username: String
)