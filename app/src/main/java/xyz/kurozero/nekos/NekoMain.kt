package xyz.kurozero.nekos

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.net.ConnectivityManager
import java.io.FileOutputStream
import java.io.File
import com.github.kittinunf.fuel.android.extension.responseJson
import com.google.gson.Gson
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.selector
import kotlinx.android.synthetic.main.activity_neko_main.*

class NekoMain : AppCompatActivity(), ConnectivityReceiver.ConnectivityReceiverListener {

    private val url = "https://nekos.moe/api/v1"
    private var toSkip = 0
    private var page = 0
    private var isNew = true
    private var nsfw = false
    private var sort = "newest"
    private var nekos: Nekos? = null
    private var adapter: NekoAdapter? = null
    private var optionsMenu: Menu? = null
    private var connected: Boolean = true
    val permissions = arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        setContentView(R.layout.activity_neko_main)
        setSupportActionBar(toolbar)

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
            Snackbar.make(nekoImages, "Soon™", Snackbar.LENGTH_SHORT).show()
            true
        }

        navPrev.setOnMenuItemClickListener {
            requestNeko(false)
            true
        }

        FuelManager.instance.basePath = url
        FuelManager.instance.baseHeaders = mapOf("User-Agent" to "NekosApp/v0.2.0 (https://github.com/KurozeroPB/nekos-app)")
        requestNeko(true)
    }

    override fun onResume() {
        super.onResume()
        ConnectivityReceiver.connectivityReceiverListener = this
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        connected = isConnected
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            999 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(
                            nekoImages,
                            "The app can now save images to your storage",
                            Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    Snackbar.make(
                            nekoImages,
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
                Snackbar.make(nekoImages, "Soon™", Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.view_account -> {
                Snackbar.make(nekoImages, "Soon™", Snackbar.LENGTH_SHORT).show()
                true
            }
            R.id.switch_nsfw -> {
                val switchNsfw = optionsMenu?.findItem(R.id.switch_nsfw)

                isNew = true
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
            R.id.sort -> {
                val buttons = listOf("New", "Old", "Likes")
                selector(null, buttons, { _, i ->
                    when (i) {
                        0 -> {
                            sort = "newest"
                            requestNeko(false)
                        }
                        1 -> {
                            sort = "oldest"
                            requestNeko(false)
                        }
                        2 -> {
                            sort = "likes"
                            requestNeko(false)
                        }
                        else -> return@selector
                    }
                })
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestNeko(next: Boolean) {
        if (!connected || !isConnected(this)) {
            Snackbar.make(nekoImages, "No network connection", Snackbar.LENGTH_INDEFINITE).show()
            return
        }

        doAsync {
            toSkip = if (isNew || page == 0) {
                0
            } else {
                if (next) toSkip + 10 else toSkip - 10
            }

            Fuel.post("/images/search")
                    .header(mapOf("Content-Type" to "application/json"))
                    .body("{\"nsfw\": $nsfw, \"limit\": 10, \"skip\": $toSkip, \"sort\": \"$sort\"}")
                    .responseJson { _, _, result ->
                        val (neko, error) = result
                        if (neko != null) {
                            val newNekos = checkNotNull(Nekos.Deserializer().deserialize(neko.content))
                            nekos = newNekos
                            adapter = NekoAdapter(this@NekoMain, newNekos)
                            nekoImages.adapter = adapter
                            page++
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

fun downloadAndSave(neko: Neko, main: NekoMain) {
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
            val msg = err.message ?: "Something went wrong"
            Snackbar.make(main.nekoImages, msg, Snackbar.LENGTH_LONG).show()
        }
    }
}

fun hasPermissions(context: Context?, permissions: Array<String>): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && context != null) {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
    }
    return true
}

fun isConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnected
}

// Deserialize response json
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