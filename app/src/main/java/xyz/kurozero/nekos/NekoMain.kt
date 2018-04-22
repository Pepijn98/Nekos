package xyz.kurozero.nekos

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
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
import android.text.format.DateUtils
import java.io.FileOutputStream
import java.io.File
import com.github.kittinunf.fuel.android.extension.responseJson
import com.google.gson.Gson
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.core.Json
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.httpGet
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.selector
import kotlinx.android.synthetic.main.activity_neko_main.*

class NekoMain : AppCompatActivity(), ConnectivityReceiver.ConnectivityReceiverListener {

    private val url = "https://nekos.moe/api/v1"
    private val userAgent = "NekosApp/v0.3.0 (https://github.com/KurozeroPB/nekos-app)"
    private var toSkip = 0
    private var page = 1
    private var isNew = true
    private var nsfw = false
    private var sort = "newest"
    private var nekos: Nekos? = null
    private var user: User? = null
    private var adapter: NekoAdapter? = null
    private var optionsMenu: Menu? = null
    private var connected: Boolean = true
    private var sharedPreferences: SharedPreferences? = null
    val permissions = arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        setContentView(R.layout.activity_neko_main)
        setSupportActionBar(toolbar)
        sharedPreferences = getSharedPreferences("nekos.moe", Context.MODE_PRIVATE)
        val token = sharedPreferences!!.getString("token", "")

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

        if (token.isNullOrBlank()) {
            FuelManager.instance.baseHeaders = mapOf("User-Agent" to userAgent)
        } else {
            FuelManager.instance.baseHeaders = mapOf(
                    "User-Agent" to userAgent,
                    "Authorization" to token
            )
        }

        requestNeko(false)
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

        val token = sharedPreferences!!.getString("token", "")
        if (token.isNullOrBlank()) {
            loginOut.title = getString(R.string.login_out, "Login")
        } else {
            loginOut.title = getString(R.string.login_out, "Logout")
        }

        switchNsfw.title = getString(R.string.switch_nsfw, "Enable")
        optionsMenu = menu
        return true
    }

    @SuppressLint("InflateParams")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.login_out -> {
                if (!connected || !isConnected(this)) {
                    Snackbar.make(nekoImages, "No network connection", Snackbar.LENGTH_INDEFINITE).show()
                    return false
                }
                if (item.title == "Login") {
                    val loginDialog = AlertDialog.Builder(this)
                    val factory = LayoutInflater.from(this)
                    val view = factory.inflate(R.layout.login_dialog, null)
                    loginDialog.setView(view)
                    loginDialog.setNegativeButton("Cancel", { dialog, _ -> dialog.dismiss() })
                    loginDialog.setPositiveButton("Login", { _, _ ->
                        doAsync {
                            val username = view.findViewById<EditText>(R.id.usernameInput)
                            val password = view.findViewById<EditText>(R.id.passwordInput)
                            Fuel.post("/auth")
                                    .header(mapOf("Content-Type" to "application/json"))
                                    .body("{\"username\": \"${username.text}\", \"password\": \"${password.text}\"}")
                                    .responseJson { _, _, result ->
                                        val (data, error) = result
                                        if (data != null) {
                                            val token = data.obj().get("token") as String
                                            sharedPreferences!!.edit().putString("token", token).apply()
                                            updateUI(true)
                                            FuelManager.instance.baseHeaders = mapOf(
                                                    "User-Agent" to userAgent,
                                                    "Authorization" to token
                                            )
                                            Snackbar.make(nekoImages, "Success logging in", Snackbar.LENGTH_SHORT).show()
                                        } else if (error != null) {
                                            updateUI(false)
                                            val msg =
                                                    Json(String(error.errorData)).obj().get("message") as String?
                                                            ?: error.message
                                                            ?: "Something went wrong"
                                            Snackbar.make(nekoImages, msg, Snackbar.LENGTH_LONG).show()
                                        }
                                    }
                        }
                    })
                    loginDialog.show()
                } else {
                    updateUI(false)
                }
                true
            }
            R.id.view_account -> {
                if (user == null) requestMe()
                else loadUser()
                true
            }
            R.id.switch_nsfw -> {
                val switchNsfw = optionsMenu?.findItem(R.id.switch_nsfw)

                isNew = true
                page = 1
                requestNeko(false)

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
                            isNew = true
                            page = 1
                            sort = "newest"
                            requestNeko(false)
                        }
                        1 -> {
                            isNew = true
                            page = 1
                            sort = "oldest"
                            requestNeko(false)
                        }
                        2 -> {
                            isNew = true
                            page = 1
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

    private fun requestMe() {
        val token = sharedPreferences!!.getString("token", "")
        if (token.isNullOrBlank()) {
            Snackbar.make(nekoImages, "You need to login first", Snackbar.LENGTH_LONG).show()
            return
        }
        doAsync {
            "user/@me".httpGet().responseJson { _, _, result ->
                val (data, error) = result
                if (data != null) {
                    val newUser = checkNotNull(User.Deserializer().deserialize(data.obj().get("user").toString()))
                    user = newUser
                    loadUser()
                } else if (error != null) {
                    val msg =
                            Json(String(error.errorData)).obj().get("message") as String?
                                    ?: error.message
                                    ?: "Something went wrong"
                    Snackbar.make(nekoImages, msg, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun loadUser() {
        val userDialog = AlertDialog.Builder(this)
        val factory = LayoutInflater.from(this)
        val view = factory.inflate(R.layout.user_dialog, null)
        userDialog.setView(view)

        val username = view.findViewById<TextView>(R.id.tvUsername)
        val likes = view.findViewById<TextView>(R.id.tvLikes)
        val favorites = view.findViewById<TextView>(R.id.tvFavorites)
        val joined = view.findViewById<TextView>(R.id.tvJoined)
        val posted = view.findViewById<TextView>(R.id.tvPosted)
        val given = view.findViewById<TextView>(R.id.tvGiven)

        val suffix = if (user?.uploads == 1) "image" else "images"

        if (user != null) {
            username.text = getString(R.string.acc_username, user?.username)
            likes.text = getString(R.string.likes, user?.likesReceived)
            favorites.text = getString(R.string.favorites, user?.favoritesReceived)
            joined.text = getString(R.string.joined, timestamp(user?.createdAt!!))
            posted.text = getString(R.string.posted, "${user?.uploads} $suffix")
            given.text = getString(R.string.given, user?.likes!!.size, user?.favorites!!.size)
        } else {
            requestMe()
        }
        userDialog.show()
    }

    private fun timestamp(timeCreated: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        val timeCreatedDate = dateFormat.parse(timeCreated)
        return DateUtils.getRelativeTimeSpanString(timeCreatedDate.time, System.currentTimeMillis(),  DateUtils.SECOND_IN_MILLIS) as String
    }

    private fun requestNeko(next: Boolean) {
        if (!connected || !isConnected(this)) {
            Snackbar.make(nekoImages, "No network connection", Snackbar.LENGTH_INDEFINITE).show()
            return
        }
        val oldPage = page
        page = if (next) {
            page + 1
        } else {
            if (page > 1) page - 1
            else 1
        }

        toSkip = if (isNew || page <= 1) {
            0
        } else {
            if (next) toSkip + 10 else toSkip - 10
        }

        if (page <= 1 && !isNew && oldPage != 2) return

        doAsync {
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
                            // nekos.images.find { s -> s.id == "" }
                        } else if (error != null) {
                            val msg =
                                    Json(String(error.errorData)).obj().get("message") as String?
                                            ?: error.message
                                            ?: "Something went wrong"
                            Snackbar.make(nekoImages, msg, Snackbar.LENGTH_LONG).show()
                        }
                    }
            isNew = false
        }
    }

    private fun updateUI(success: Boolean) {
        val loginOut = optionsMenu!!.findItem(R.id.login_out)
        if (success) {
            loginOut.title = getString(R.string.login_out, "Logout")
        } else {
            sharedPreferences!!.edit().remove("token").apply()
            loginOut.title = getString(R.string.login_out, "Login")
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
            val msg =
                    Json(String(err.errorData)).obj().get("message") as String?
                            ?: err.message
                            ?: "Something went wrong"
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

data class User(
        val id: String,
        val username: String,
        val createdAt: String,
        val favoritesReceived: Int,
        val likesReceived: Int,
        val favorites: ArrayList<String>,
        val likes: ArrayList<String>,
        val uploads: Int,
        val roles: ArrayList<String>,
        val verified: Boolean,
        val savedTags: ArrayList<String>
) {
    class Deserializer : ResponseDeserializable<User> {
        override fun deserialize(content: String): User? = Gson().fromJson(content, User::class.java)
    }
}