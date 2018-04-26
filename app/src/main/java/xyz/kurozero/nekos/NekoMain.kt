package xyz.kurozero.nekos

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.icu.text.SimpleDateFormat
import android.net.*
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.widget.*
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.provider.MediaStore
import android.text.format.DateUtils
import com.github.kittinunf.fuel.android.extension.responseJson
import com.google.gson.Gson
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.core.Json
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.httpGet
import org.jetbrains.anko.*
import org.json.JSONException
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import kotlinx.android.synthetic.main.activity_neko_main.*
import kotlinx.android.synthetic.main.alert_dialog.view.*
import kotlinx.android.synthetic.main.view_neko_dialog.*
import okhttp3.*
import okhttp3.Request
import java.io.File

const val userAgent = "NekosApp/v0.5.1 (https://github.com/KurozeroPB/nekos-app)"
val File.extension: String
    get() = name.substringAfterLast('.', "")

class NekoMain : AppCompatActivity(), ConnectivityReceiver.ConnectivityReceiverListener {

    private val url = "https://nekos.moe/api/v1"
    private var toSkip = 0
    private var page = 1
    private var isNew = true
    private var init = true
    private var sort = "newest"
    private var nekos: Nekos? = null
    private var adapter: NekoAdapter? = null
    private var optionsMenu: Menu? = null
    private var sharedPreferences: SharedPreferences? = null
    private var typeFace: Typeface? = null
    var connected: Boolean = true
    var nsfw = false
    val permissions = arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        setContentView(R.layout.activity_neko_main)
        setSupportActionBar(toolbar)
        supportActionBar?.title = null
        typeFace = Typeface.createFromAsset(assets, "fonts/nunito.ttf")
        toolbar_title.typeface = typeFace

        FontsOverride.setDefaultFont(this, "DEFAULT", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "SERIF", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "fonts/nunito.ttf")

        sharedPreferences = getSharedPreferences("nekos.moe", Context.MODE_PRIVATE)

        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 999)
        }

        val navNext = navigationView.menu.findItem(R.id.navigation_next)
        val navRefrsh = navigationView.menu.findItem(R.id.navigation_refresh)
        val navPrev = navigationView.menu.findItem(R.id.navigation_previous)
        navNext.setOnMenuItemClickListener {
            requestNeko(true)
            true
        }

        navRefrsh.setOnMenuItemClickListener {
            isNew = true
            page = 1
            toSkip = 0
            requestNeko(false)
            true
        }

        navPrev.setOnMenuItemClickListener {
            requestNeko(false)
            true
        }

        FuelManager.instance.basePath = url

        val token = sharedPreferences!!.getString("token", "")
        if (token.isNullOrBlank() || token.isNullOrEmpty()) {
            FuelManager.instance.baseHeaders = mapOf("User-Agent" to userAgent)
        } else {
            FuelManager.instance.baseHeaders = mapOf(
                    "User-Agent" to userAgent,
                    "Authorization" to token
            )
        }

        requestNeko(false)
        setInit()
    }

    override fun onResume() {
        super.onResume()
        ConnectivityReceiver.connectivityReceiverListener = this
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        connected = isConnected

        val isMobile = checkConnectionType()
        if (isMobile) return
        if (!connected || !isConnected(this)) {
            longToast("Lost network connection")
        } else {
            if (!init) toast("Network connection restored")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            999 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    longToast("The app can now save images to your storage")
                } else {
                    longToast("The app was not allowed to write in your storage")
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val loginOut = menu.findItem(R.id.login_out)
        val switchNsfw = menu.findItem(R.id.switch_nsfw)

        val token = sharedPreferences!!.getString("token", "")
        if (token.isNullOrBlank() || token.isNullOrEmpty()) {
            loginOut.title = getString(R.string.login_out, "Login")
        } else {
            loginOut.title = getString(R.string.login_out, "Logout")
        }

        switchNsfw.title = getString(R.string.switch_nsfw, "Enable")
        optionsMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.login_out -> {
                if (item.title == "Login") {
                    login()
                } else {
                    updateUI(false)
                }
                true
            }
            R.id.view_account -> {
                loadMe()
                true
            }
            R.id.switch_nsfw -> {
                val switchNsfw = optionsMenu?.findItem(R.id.switch_nsfw)

                isNew = true
                page = 1
                if (!nsfw) {
                    updateNsfwUI(true)
                    nsfw = true
                    switchNsfw?.title = getString(R.string.switch_nsfw, "Disable")
                    toast("Enabled nsfw images")
                } else {
                    updateNsfwUI(false)
                    nsfw = false
                    switchNsfw?.title = getString(R.string.switch_nsfw, "Enable")
                    toast("Disabled nsfw images")
                }
                requestNeko(false)
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
            R.id.upload -> {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 998)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == RESULT_CANCELED) return
        when (requestCode) {
            998 -> uploadNeko(intent!!.data)
        }
    }

    @SuppressLint("InflateParams")
    private fun loadMe() {
        if (!connected || !isConnected(this)) {
            toast("No network connection")
            return
        }
        val token = sharedPreferences!!.getString("token", "")
        if (token.isNullOrBlank() || token.isNullOrEmpty()) {
            longToast("You need to login first")
            return
        }
        doAsync {
            "user/@me".httpGet().responseJson { _, _, result ->
                val (resp, error) = result
                if (resp != null) {
                    val user = checkNotNull(User.Deserializer().deserialize(resp.obj().get("user").toString()))
                    val userDialog = AlertDialog.Builder(this@NekoMain)
                    val factory = LayoutInflater.from(this@NekoMain)
                    val view = factory.inflate(R.layout.user_dialog, null)
                    userDialog.setView(view)

                    val username = view.findViewById<TextView>(R.id.tvUsername)
                    val likes = view.findViewById<TextView>(R.id.tvLikes)
                    val favorites = view.findViewById<TextView>(R.id.tvFavorites)
                    val joined = view.findViewById<TextView>(R.id.tvJoined)
                    val posted = view.findViewById<TextView>(R.id.tvPosted)
                    val given = view.findViewById<TextView>(R.id.tvGiven)

                    val suffix = if (user.uploads == 1) "image" else "images"

                    username.text = getString(R.string.acc_username, user.username)
                    likes.text = getString(R.string.likes, user.likesReceived)
                    favorites.text = getString(R.string.favorites, user.favoritesReceived)
                    joined.text = getString(R.string.joined, timestamp(user.createdAt))
                    posted.text = getString(R.string.posted, "${user.uploads} $suffix")
                    given.text = getString(R.string.given, user.likes.size, user.favorites.size)
                    userDialog.show()
                } else if (error != null) {
                    val msg = try {
                        Json(String(error.errorData)).obj().get("message") as String?
                                ?: error.message
                                ?: "Something went wrong"
                    } catch (e: JSONException) {
                        e.message ?: "Something went wrong"
                    }
                    longToast(msg)
                }
            }
        }
    }

    private fun requestNeko(next: Boolean) {
        if (!connected || !isConnected(this)) {
            toast("No network connection")
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
                        } else if (error != null) {
                            val msg =
                                    Json(String(error.errorData)).obj().get("message") as String?
                                            ?: error.message
                                            ?: "Something went wrong"
                            longToast(msg)
                        }
                    }
            isNew = false
        }
    }

    @SuppressLint("InflateParams")
    private fun uploadNeko(uri: Uri) {
        if (!connected || !isConnected(this)) {
            toast("No network connection")
            return
        }
        val token = sharedPreferences!!.getString("token", "")
        if (token.isNullOrBlank() || token.isNullOrEmpty()) {
            longToast("You need to login first")
            return
        }

        val imgPath = FilePickUtils.getSmartFilePath(this, uri)
        val file = File(imgPath)

        val uploadDialog = AlertDialog.Builder(this)
        val factory = LayoutInflater.from(this)
        val view = factory.inflate(R.layout.upload_dialog, null)
        uploadDialog.setView(view)

        val uploadImage = view.findViewById<ImageView>(R.id.uploadImage)
        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        uploadImage.setImageBitmap(bitmap)

        uploadDialog.setNegativeButton("Cancel", null)
        uploadDialog.setPositiveButton("Upload", { _, _ ->
            val strtags = view.findViewById<EditText>(R.id.etTags).text.toString()
            val artist = view.findViewById<EditText>(R.id.etArtist).text.toString()
            val nsfw = view.findViewById<Switch>(R.id.swNsfw).isChecked
            val tags = strtags.split(Regex(", ?"), 0)

            doAsync {
                val mediaType = MediaType.parse("image/${file.extension}")
                val client = OkHttpClient()
                val builder = MultipartBody.Builder()

                builder.setType(MultipartBody.FORM)
                builder.addFormDataPart("image", file.nameWithoutExtension, RequestBody.create(mediaType, file))
                tags.forEach { tag -> builder.addFormDataPart("tags[]", tag) }
                builder.addFormDataPart("artist", artist)
                builder.addFormDataPart("nsfw", nsfw.toString())
                builder.setType(MediaType.parse("multipart/form-data")!!)
                val requestBody = builder.build()

                val headers = Headers.Builder()
                        .add("Authorization", token)
                        .add("User-Agent", userAgent)
                        .build()

                val request = Request.Builder()
                        .url("$url/images")
                        .headers(headers)
                        .post(requestBody)
                        .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    longToast(Json(response.body()?.string()!!).obj().get("message") as String)
                } else {
                    toast("Success uploading neko, awaiting approval of an admin")
                }
                response.close()
            }
        })

        uploadDialog.show()
    }

    @SuppressLint("InflateParams")
    private fun login() {
        if (!connected || !isConnected(this)) {
            toast("No network connection")
            return
        }
        val loginDialog = AlertDialog.Builder(this)
        val factory = LayoutInflater.from(this)
        val view = factory.inflate(R.layout.login_dialog, null)
        loginDialog.setView(view)
        loginDialog.setNegativeButton("Cancel", { dialog, _ -> dialog.cancel() })
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
                                toast("Success logging in")
                            } else if (error != null) {
                                updateUI(false)
                                val msg =
                                        Json(String(error.errorData)).obj().get("message") as String?
                                                ?: error.message
                                                ?: "Something went wrong"
                                longToast(msg)
                            }
                        }
            }
        })
        loginDialog.show()
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

    private fun updateNsfwUI(nsfw: Boolean) {
        if (nsfw) {
            setTheme(R.style.AppThemeNsfw)
            window.statusBarColor = getColor(R.color.nsfw_colorPrimaryDark)
            toolbar.backgroundColor = getColor(R.color.nsfw_colorPrimary)
            navigationView.setBackgroundColor(getColor(R.color.nsfw_colorPrimary))
            btnSaveNeko?.setTextColor(getColor(R.color.nsfw_colorPrimary))
            btnShareNeko?.setTextColor(getColor(R.color.nsfw_colorPrimary))
            btnCloseNeko?.setTextColor(getColor(R.color.nsfw_colorPrimary))
        } else {
            setTheme(R.style.AppTheme)
            window.statusBarColor = getColor(R.color.colorPrimaryDark)
            toolbar.backgroundColor = getColor(R.color.colorPrimary)
            navigationView.setBackgroundColor(getColor(R.color.colorPrimary))
            btnSaveNeko?.setTextColor(getColor(R.color.colorPrimary))
            btnShareNeko?.setTextColor(getColor(R.color.colorPrimary))
            btnCloseNeko?.setTextColor(getColor(R.color.colorPrimary))
        }
    }

    @SuppressLint("InflateParams")
    private fun checkConnectionType(): Boolean {
        val conn = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = conn.activeNetworkInfo
        if (netInfo?.type == ConnectivityManager.TYPE_MOBILE) {
            val alertDialog = AlertDialog.Builder(this)
            val factory = LayoutInflater.from(this)
            val view = factory.inflate(R.layout.alert_dialog, null)
            alertDialog.setView(view)
            alertDialog.setNegativeButton("Close", null)
            alertDialog.setPositiveButton("Switch", { _, _ ->
                val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifi.isWifiEnabled = true
                toast("Enabled wifi, have fun browsing!")
            })
            view.alertMessage.text = getString(R.string.network_usage_alert)
            alertDialog.show()
            return true
        }
        return false
    }

    // I wish I could hide functions like these, this is disgusting
    private fun setInit() {
        task {
            var i = 0
            while (i<100000000) i++
            i
        } then {
            i -> i != 100000000
        } success {
            value -> init = value; println(init)
        }
    }
}

fun timestamp(timeCreated: String): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    val timeCreatedDate = dateFormat.parse(timeCreated)
    return DateUtils.getRelativeTimeSpanString(timeCreatedDate.time, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS) as String
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