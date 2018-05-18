package xyz.kurozero.nekos

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.*
import android.net.wifi.WifiManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.*
import android.support.v4.app.ActivityCompat
import android.provider.MediaStore
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.GridLayoutManager
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.android.core.Json
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.httpGet
import kotlinx.android.synthetic.main.activity_neko_main.*
import org.jetbrains.anko.*
import org.json.JSONException
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import kotlinx.android.synthetic.main.app_bar_neko_main.*
import kotlinx.android.synthetic.main.alert_dialog.view.*
import kotlinx.android.synthetic.main.content_neko_main.*
import kotlinx.android.synthetic.main.nav_header_neko_main.view.*
import okhttp3.*
import okhttp3.Request
import java.io.File
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import kotlinx.android.synthetic.main.login_dialog.view.*
import kotlinx.android.synthetic.main.upload_dialog.view.*
import kotlinx.android.synthetic.main.user_dialog.view.*

const val userAgent = "NekosApp/v0.6.0 (https://github.com/KurozeroPB/nekos-app)"
val File.extension: String
    get() = name.substringAfterLast('.', "")

class NekoMain : AppCompatActivity(), ConnectivityReceiver.ConnectivityReceiverListener, NavigationView.OnNavigationItemSelectedListener {

    private val url = "https://nekos.moe/api/v1"
    private var toSkip = 0
    private var page = 1
    private var isNew = true
    private var init = true
    private var sort = "newest"
    private var nekos: Nekos? = null
    private var adapter: NekoAdapter? = null
    private var typeFace: Typeface? = null
    var sharedPreferences: SharedPreferences? = null
    var user: User? = null
    var isLoggedin: Boolean = false
    var connected: Boolean = true
    var nsfw: Boolean? = false
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
        sharedPreferences = getSharedPreferences("nekos.moe", Context.MODE_PRIVATE)

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        val loginOut = nav_view.menu.findItem(R.id.login_out)
        val token = sharedPreferences!!.getString("token", "")
        val userstr = sharedPreferences!!.getString("user", "")
        user = if (!userstr.isNullOrEmpty() || !userstr.isNullOrBlank()) User.Deserializer().deserialize(userstr) else null

        isLoggedin = !token.isNullOrBlank() || !token.isNullOrEmpty()
        val headerView = nav_view.getHeaderView(0)

        if (isLoggedin) {
            loginOut.title = getString(R.string.login_out, "Logout")
            loginOut.icon = getDrawable(R.drawable.ic_menu_logout)
            headerView.headerTitle.text = user?.username ?: "-"
        } else {
            loginOut.title = getString(R.string.login_out, "Login")
            loginOut.icon = getDrawable(R.drawable.ic_menu_login)
            headerView.headerTitle.text = "-"
        }

        FontsOverride.setDefaultFont(this, "DEFAULT", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "SERIF", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "fonts/nunito.ttf")

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

        if (isLoggedin) {
            FuelManager.instance.baseHeaders = mapOf(
                    "User-Agent" to userAgent,
                    "Authorization" to token
            )
        } else {
            FuelManager.instance.baseHeaders = mapOf("User-Agent" to userAgent)
        }

        val config = ImagePipelineConfig.newBuilder(this)
                .setProgressiveJpegConfig(SimpleProgressiveJpegConfig())
                .setResizeAndRotateEnabledForNetwork(true)
                .setDownsampleEnabled(true)
                .build()
        Fresco.initialize(this, config)

        nekoImages.layoutManager = GridLayoutManager(this, 2)
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.login_out -> {
                if (!isLoggedin) {
                    login()
                } else {
                    updateUI(false)
                }
            }
            R.id.view_account -> viewProfile()
            R.id.switch_nsfw -> {
                val buttons = listOf("Show me everything", "Only NSFW", "Block NSFW")
                isNew = true
                page = 1
                selector(null, buttons, { _, i ->
                    when (i) {
                        0 -> {
                            nsfw = null
                            requestNeko(false)
                        }
                        1 -> {
                            nsfw = true
                            requestNeko(false)
                        }
                        2 -> {
                            nsfw = false
                            requestNeko(false)
                        }
                        else -> return@selector
                    }
                })
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
            }
            R.id.upload -> {
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 998)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == RESULT_CANCELED) return
        when (requestCode) {
            998 -> uploadNeko(intent!!.data)
        }
    }

    private fun getMe() {
        if (!connected || !isConnected(this)) return
        doAsync {
            "user/@me".httpGet().responseJson { _, _, result ->
                val (resp, _) = result
                if (resp != null) {
                    user = User.Deserializer().deserialize(resp.obj().get("user").toString())
                    sharedPreferences!!.edit().putString("user", resp.obj().get("user").toString()).apply()
                    updateUI(true)
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun viewProfile() {
        if (!isLoggedin) {
            longToast("You need to be logged in")
            return
        }

        val userDialog = AlertDialog.Builder(this@NekoMain)
        val factory = LayoutInflater.from(this@NekoMain)
        val view = factory.inflate(R.layout.user_dialog, null)
        userDialog.setView(view)

        val suffix = if (user!!.uploads == 1) "image" else "images"

        view.tvUsername.text = getString(R.string.acc_username, user!!.username)
        view.tvLikes.text = getString(R.string.likes, user!!.likesReceived)
        view.tvFavorites.text = getString(R.string.favorites, user!!.favoritesReceived)
        view.tvJoined.text = getString(R.string.joined, timestamp(user!!.createdAt))
        view.tvPosted.text = getString(R.string.posted, "${user!!.uploads} $suffix")
        view.tvGiven.text = getString(R.string.given, user!!.likes.size, user!!.favorites.size)

        view.btnSync.setOnClickListener {
            if (!connected || !isConnected(this@NekoMain)) {
                longToast("No network connection")
                return@setOnClickListener
            }
            doAsync {
                "user/@me".httpGet().responseJson { _, _, result ->
                    val (resp, error) = result
                    if (resp != null) {
                        user = User.Deserializer().deserialize(resp.obj().get("user").toString())
                        sharedPreferences!!.edit().putString("user", resp.obj().get("user").toString()).apply()
                        view.tvUsername.text = getString(R.string.acc_username, user!!.username)
                        view.tvLikes.text = getString(R.string.likes, user!!.likesReceived)
                        view.tvFavorites.text = getString(R.string.favorites, user!!.favoritesReceived)
                        view.tvJoined.text = getString(R.string.joined, timestamp(user!!.createdAt))
                        view.tvPosted.text = getString(R.string.posted, "${user!!.uploads} $suffix")
                        view.tvGiven.text = getString(R.string.given, user!!.likes.size, user!!.favorites.size)
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

        userDialog.show()
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

        val reqbody =
                if (nsfw != null) "{\"nsfw\": $nsfw, \"limit\": 10, \"skip\": $toSkip, \"sort\": \"$sort\"}"
                else "{\"limit\": 10, \"skip\": $toSkip, \"sort\": \"$sort\"}"

        doAsync {
            Fuel.post("/images/search")
                    .header(mapOf("Content-Type" to "application/json"))
                    .body(reqbody)
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

        if (!isLoggedin) {
            longToast("You need to login first")
            return
        }

        val token = sharedPreferences!!.getString("token", "")
        val imgPath = FilePickUtils.getSmartFilePath(this, uri)
        val file = File(imgPath)

        val uploadDialog = AlertDialog.Builder(this)
        val factory = LayoutInflater.from(this)
        val view = factory.inflate(R.layout.upload_dialog, null)
        uploadDialog.setView(view)

        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        view.uploadImage.setImageBitmap(bitmap)

        uploadDialog.setNegativeButton("Cancel", null)
        uploadDialog.setPositiveButton("Upload", { _, _ ->
            val strtags = view.etTags.text.toString()
            val artist = view.etArtist.text.toString()
            val nsfw = view.swNsfw.isChecked
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
                Fuel.post("/auth")
                        .header(mapOf("Content-Type" to "application/json"))
                        .body("{\"username\": \"${view.usernameInput.text}\", \"password\": \"${view.passwordInput.text}\"}")
                        .responseJson { _, _, result ->
                            val (data, error) = result
                            if (data != null) {
                                val token = data.obj().get("token") as String
                                sharedPreferences!!.edit().putString("token", token).apply()
                                FuelManager.instance.baseHeaders = mapOf(
                                        "User-Agent" to userAgent,
                                        "Authorization" to token
                                )
                                getMe()
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
        val loginOut = nav_view.menu.findItem(R.id.login_out)
        val headerView = nav_view.getHeaderView(0)
        if (success) {
            isLoggedin = true
            headerView.headerTitle.text = user!!.username
            loginOut.title = getString(R.string.login_out, "Logout")
            loginOut.icon = getDrawable(R.drawable.ic_menu_logout)
        } else {
            isLoggedin = false
            sharedPreferences!!.edit().remove("token").apply()
            headerView.headerTitle.text = "-"
            loginOut.title = getString(R.string.login_out, "Login")
            loginOut.icon = getDrawable(R.drawable.ic_menu_login)
            toast("You're now logged out")
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
            while (i < 100000000) i++
            i
        } then { i ->
            i != 100000000
        } success { value ->
            init = value; println(init)
        }
    }
}
