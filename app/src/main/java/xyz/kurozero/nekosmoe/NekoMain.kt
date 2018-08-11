package xyz.kurozero.nekosmoe

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.*
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.*
import android.support.v4.app.ActivityCompat
import android.provider.MediaStore
import android.provider.Settings
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*
import com.github.kittinunf.fuel.httpGet
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import com.github.kittinunf.result.Result
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import org.jetbrains.anko.*
import org.json.JSONException
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import io.sentry.Sentry
import io.sentry.android.AndroidSentryClientFactory
import kotlinx.android.synthetic.main.activity_neko_main.*
import kotlinx.android.synthetic.main.app_bar_neko_main.*
import kotlinx.android.synthetic.main.alert_dialog.view.*
import kotlinx.android.synthetic.main.content_neko_main.*
import kotlinx.android.synthetic.main.nav_header_neko_main.view.*
import kotlinx.android.synthetic.main.login_dialog.view.*
import kotlinx.android.synthetic.main.register_dialog.view.*
import kotlinx.android.synthetic.main.upload_dialog.view.*
import kotlinx.android.synthetic.main.user_dialog.view.*
import okhttp3.*
import okhttp3.Request
import java.io.File
import java.io.IOException

// https://nekos.moe/api/v1
// http://localhost:8080/api/v1
const val baseUrl = "https://nekos.moe/api/v1"
const val userAgent = "NekosApp/v0.11.0 (https://github.com/KurozeroPB/nekos-app)"

val File.extension: String
    get() = name.substringAfterLast('.', "")

// Permissions needed to save and upload images
val permissions = arrayOf(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
)

// Important data used across the app
lateinit var nekos: Nekos
lateinit var sharedPreferences: SharedPreferences
lateinit var httpClient: OkHttpClient
lateinit var bundle: Bundle
lateinit var firebaseAnalytics: FirebaseAnalytics
lateinit var typeFace: Typeface
lateinit var interstitialAd: InterstitialAd
lateinit var deviceID: String
var user: User? = null

// Checks
var isLoggedin: Boolean = false
var connected: Boolean = true

class NekoMain : AppCompatActivity(), ConnectivityReceiver.ConnectivityReceiverListener, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var adapter: NekoAdapter

    // Pagination and sorting stuff
    private var toSkip = 0
    private var page = 1
    private var isNew = true
    private var init = true
    private var sort = "newest"

    // Whenever the app is started
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        setContentView(R.layout.activity_neko_main)
        setSupportActionBar(toolbar)
        supportActionBar?.title = null
        httpClient = OkHttpClient()
        Sentry.init("https://4cd9a20178ce47b1a31b9a9b251510d6@sentry.io/1217022", AndroidSentryClientFactory(applicationContext))

        // Do some fonts magic
        typeFace = Typeface.createFromAsset(assets, "fonts/nunito.ttf")
        toolbar_title.typeface = typeFace
        FontsOverride.setDefaultFont(this, "DEFAULT", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "SERIF", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "fonts/nunito.ttf")

        // Get our shared preferences, this includes the user and token if the user has logged in
        sharedPreferences = getSharedPreferences("nekos.moe", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", "")
        val userstr = sharedPreferences.getString("user", "")

        // Populate public variables
        user = if (userstr.isNotEmpty())
            User.Deserializer().deserialize(userstr) else null
        isLoggedin = token.isNotEmpty()

        // Add our drawer
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        val loginOut = nav_view.menu.findItem(R.id.login_out)

        // Change text based on whether you're logged in
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

        // If we don't yet have our permissions ask for them
        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 999)
        }

        // Find bottom nav items and set OnClick listeners
        val navNext = navigationView.menu.findItem(R.id.navigation_next)
        val navRefrsh = navigationView.menu.findItem(R.id.navigation_refresh)
        val navPrev = navigationView.menu.findItem(R.id.navigation_previous)
        navNext.setOnMenuItemClickListener {
            val rand = Math.floor((Math.random() * 8) + 1).toInt()
            if (rand == 2 && interstitialAd.isLoaded) {
                interstitialAd.show()
            }
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, deviceID)
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Button Next")
            requestNeko(true)
            true
        }
        navRefrsh.setOnMenuItemClickListener {
            val rand = Math.floor((Math.random() * 8) + 1).toInt()
            if (rand == 2 && interstitialAd.isLoaded) {
                interstitialAd.show()
            }
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, deviceID)
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Button Refresh")
            isNew = true
            page = 1
            toSkip = 0
            requestNeko(false)
            true
        }
        navPrev.setOnMenuItemClickListener {
            val rand = Math.floor((Math.random() * 8) + 1).toInt()
            if (rand == 2 && interstitialAd.isLoaded) {
                interstitialAd.show()
            }
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, deviceID)
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Button Previous")
            requestNeko(false)
            true
        }

        // Set fuel default settings
        FuelManager.instance.basePath = baseUrl
        if (isLoggedin) {
            FuelManager.instance.baseHeaders = mapOf(
                    "User-Agent" to userAgent,
                    "Authorization" to token
            )
        } else {
            FuelManager.instance.baseHeaders = mapOf("User-Agent" to userAgent)
        }

        // Initialize fresco for loading fullscreen images
        val config = ImagePipelineConfig.newBuilder(this)
                .setProgressiveJpegConfig(SimpleProgressiveJpegConfig())
                .setResizeAndRotateEnabledForNetwork(true)
                .setDownsampleEnabled(true)
                .build()
        Fresco.initialize(this, config)

        deviceID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        bundle = Bundle()

        MobileAds.initialize(this, "ca-app-pub-3737697469836770~8061357425")
        interstitialAd = InterstitialAd(this)
        // Test ads: ca-app-pub-3940256099942544/1033173712
        // My ads: ca-app-pub-3737697469836770/6397346995
        interstitialAd.adUnitId = "ca-app-pub-3737697469836770/6397346995"
        interstitialAd.loadAd(AdRequest.Builder().build())
        interstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                interstitialAd.loadAd(AdRequest.Builder().build())
            }
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, deviceID)
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "App Open")

        // Set the main layout manager, request the first nekos and complete the initialization
        nekoImages.layoutManager = GridLayoutManager(this, 2)
        requestNeko(false)
        setInit()
    }

    // On resume re-set the conn receiver
    override fun onResume() {
        super.onResume()
        ConnectivityReceiver.connectivityReceiverListener = this
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        // Set the new connection
        connected = isConnected

        // Check network conn type
        val isMobile = checkConnectionType()
        if (isMobile) return

        // Check whether we have a network connection
        if (!connected || !isConnected(this)) {
            showSnackbar(nekoImages, this, "Lost network connection", Snackbar.LENGTH_LONG)
        } else {
            if (!init)
                showSnackbar(nekoImages, this, "Network connection restored", Snackbar.LENGTH_SHORT)
        }
    }

    // Whenever we requested the needed permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            999 -> {
                // Check whether the user granted us the needed permissions
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showSnackbar(nekoImages, this, "The app can now save images", Snackbar.LENGTH_SHORT)
                } else {
                    showSnackbar(nekoImages, this, "The app can not save images", Snackbar.LENGTH_SHORT)
                }
            }
        }
    }

    // Drawer nav functionality
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.login_out -> {
                // Check whether we're already logged in
                if (isLoggedin) {
                    updateUI(false)
                } else {
                    login()
                }
            }
            R.id.view_account -> {
                val rand = Math.floor((Math.random() * 8) + 1).toInt()
                if (rand == 2 && interstitialAd.isLoaded) {
                    interstitialAd.show()
                }
                viewProfile()
            } // Check our profile data
            R.id.sort -> {
                // The sorting options the user can choose from
                val buttons = listOf("New", "Old", "Likes")

                // Set these back to the default to make requestNeko start over again with the new sorting options
                isNew = true
                page = 1

                // Dialog with options of whatever the user wishes to see
                selector(null, buttons) { _, i ->
                    when (i) {
                        0 -> {
                            val rand = Math.floor((Math.random() * 8) + 1).toInt()
                            if (rand == 2 && interstitialAd.isLoaded) {
                                interstitialAd.show()
                            }
                            sort = "newest"
                            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, deviceID)
                            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Sort $sort")
                            requestNeko(false)
                        }
                        1 -> {
                            val rand = Math.floor((Math.random() * 8) + 1).toInt()
                            if (rand == 2 && interstitialAd.isLoaded) {
                                interstitialAd.show()
                            }
                            sort = "oldest"
                            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, deviceID)
                            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Sort $sort")
                            requestNeko(false)
                        }
                        2 -> {
                            val rand = Math.floor((Math.random() * 8) + 1).toInt()
                            if (rand == 2 && interstitialAd.isLoaded) {
                                interstitialAd.show()
                            }
                            sort = "likes"
                            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, deviceID)
                            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Sort $sort")
                            requestNeko(false)
                        }
                        else -> return@selector
                    }
                }
            }
            R.id.upload -> {
                // Create an intent with any image format
                val intent = Intent()
                intent.type = "image/*"
                intent.action = Intent.ACTION_GET_CONTENT
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 998)
            }
        }

        // Close drawer again when an item is clicked
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    // Whenever we get a result from an intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        // If the user canceled the action just return on do nothing
        if (resultCode == RESULT_CANCELED) return
        when (requestCode) {
            998 -> uploadNeko(intent!!.data) // Open the upload dialog
            997 -> { // Delete temp file after sharing an image
                try {
                    if (file.exists() && resultCode == RESULT_OK) {
                        Thread.sleep(1_000) // Stupid result doesn't actually wait for the intent to finish sending...
                        val isDeleted = file.delete()
                        if (isDeleted)
                            Log.w("Deleted", isDeleted.toString())
                    }
                } catch (e: Exception) {
                    showSnackbar(nekoImages, this@NekoMain, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG)
                }
            }
        }
    }

    // Get the currently logged in user
    private fun getCurrentUser() {
        if (connected || isConnected(this)) {
            doAsync {
                "user/@me".httpGet().responseJson { _, _, result ->
                    val (resp, error) = result
                    when (result) {
                        is Result.Failure -> {
                            if (error != null) {
                                when (error.response.statusCode) {
                                    429 -> {
                                        showSnackbar(nekoImages, this@NekoMain, "Too many requests, please wait a few seconds", Snackbar.LENGTH_LONG)
                                    }
                                    else -> {
                                        val nekoException = NekoException.Deserializer().deserialize(error.errorData)
                                        val msg = nekoException?.message ?: error.message ?: "Something went wrong"
                                        showSnackbar(nekoImages, this@NekoMain, msg, Snackbar.LENGTH_LONG)
                                        Sentry.capture(error)
                                    }
                                }
                            }
                        }
                        is Result.Success -> {
                            if (resp != null) {
                                try {
                                    // Deserialize and save the logged in user for later
                                    user = User.Deserializer().deserialize(resp.obj().get("user").toString())
                                    sharedPreferences.edit().putString("user", resp.obj().get("user").toString()).apply()
                                    updateUI(true) // Update UI to show that we're logged in
                                } catch (e: JSONException) {
                                    showSnackbar(nekoImages, this@NekoMain, e.message ?: "A JSON exception occured", Snackbar.LENGTH_LONG)
                                    Sentry.capture(e)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun viewProfile() {
        if (!isLoggedin) {
            showSnackbar(nekoImages, this, "Login to use this action", Snackbar.LENGTH_LONG)
            return
        }

        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, deviceID)
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "View Profile")

        // Get the currently logged in user
        getCurrentUser()

        // Create view from layout
        val factory = LayoutInflater.from(this@NekoMain)
        val view = factory.inflate(R.layout.user_dialog, null)

        // Create the user dialog
        val userDialog = AlertDialog.Builder(this@NekoMain)
                .setView(view)
                .setNeutralButton("Close") { dialog, _ -> dialog.dismiss() }
                .create()

        val suffix = if (user!!.uploads == 1) "image" else "images"
        view.tvUsername.text = getString(R.string.acc_username, user!!.username)
        view.tvLikes.text = getString(R.string.likes, user!!.likesReceived)
        view.tvFavorites.text = getString(R.string.favorites, user!!.favoritesReceived)
        view.tvJoined.text = getString(R.string.joined, timestamp(user!!.createdAt))
        view.tvPosted.text = getString(R.string.posted, "${user!!.uploads} $suffix")
        view.tvGiven.text = getString(R.string.given, user!!.likes.size, user!!.favorites.size)

        userDialog.show()
    }

    // Request the neko images
    private fun requestNeko(next: Boolean) {
        if (!connected || !isConnected(this)) {
            showSnackbar(nekoImages, this, "No network connection", Snackbar.LENGTH_SHORT)
            return
        }

        // Save "page" number in new variable
        val oldPage = page

        // If next is true +1 else if the page is more than 1 we do -1.
        // This check is to prevent page going below 1 because 0, -1.... don't exist
        page = if (next) {
            page + 1
        } else {
            if (page > 1) page - 1
            else 1
        }

        // If it's a new request or page is 1 we don't need to skip anything
        // else if next is true we skip 10 if next is false we go back 10
        toSkip = if (isNew || page <= 1) {
            0
        } else {
            if (next) toSkip + 10 else toSkip - 10
        }

        // If the page is less or same as 1 and is not new and the old page is not 2 we do nothing
        // This is to prevent making useless requests
        if (page <= 1 && !isNew && oldPage != 2) return

        val reqbody = "{\"nsfw\": false, \"limit\": 10, \"skip\": $toSkip, \"sort\": \"$sort\"}"

        doAsync {
            Fuel.post("/images/search")
                    .header(mapOf("Content-Type" to "application/json"))
                    .body(reqbody)
                    .responseJson { _, _, result ->
                        val (neko, error) = result
                        when (result) {
                            is Result.Failure -> {
                                if (error != null) {
                                    when (error.response.statusCode) {
                                        429 -> {
                                            showSnackbar(nekoImages, this@NekoMain, "Too many requests, please wait a few seconds", Snackbar.LENGTH_LONG)
                                        }
                                        else -> {
                                            val nekoException = NekoException.Deserializer().deserialize(error.errorData)
                                            val msg = nekoException?.message ?: error.message ?: "Something went wrong"
                                            showSnackbar(nekoImages, this@NekoMain, msg, Snackbar.LENGTH_LONG)
                                            Sentry.capture(error)
                                        }
                                    }
                                }
                            }
                            is Result.Success -> {
                                if (neko != null) {
                                    // Deserialize and save the nekos
                                    val newNekos = Nekos.Deserializer().deserialize(neko.content)
                                    if (newNekos != null && newNekos.images.isNotEmpty()) {
                                        nekos = newNekos
                                        adapter = NekoAdapter(this@NekoMain, newNekos)
                                        nekoImages.adapter = adapter
                                    } else {
                                        // Probably reached the end if we don't get any new images anymore?
                                        showSnackbar(nekoImages, this@NekoMain, "You reached the end", Snackbar.LENGTH_LONG)
                                    }
                                }
                            }
                        }
                    }
            // Set new to false after new request
            if (isNew)
                isNew = false
        }
    }

    @SuppressLint("InflateParams")
    private fun uploadNeko(uri: Uri) {
        if (!connected || !isConnected(this)) {
            showSnackbar(nekoImages, this, "No network connection", Snackbar.LENGTH_SHORT)
            return
        }

        if (!isLoggedin) {
            showSnackbar(nekoImages, this, "Login to use this action", Snackbar.LENGTH_LONG)
            return
        }

        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, deviceID)
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Upload Neko")

        // Get important data for uploading an image
        val token = sharedPreferences.getString("token", "")
        val imgPath = FilePickUtils.getSmartFilePath(this, uri)
        val file = File(imgPath)

        val factory = LayoutInflater.from(this)
        val uploadView = factory.inflate(R.layout.upload_dialog, null)

        // Get selected image and set it on the view
        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        uploadView.uploadImage.setImageBitmap(bitmap)

        // Create upload dialog
        val uploadDialog = AlertDialog.Builder(this)
                .setView(uploadView)
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() } // Close dialog
                .setPositiveButton("Upload") { _, _ ->
                    // Get the users input
                    val strtags = uploadView.etTags.text.toString()
                    val artist = uploadView.etArtist.text.toString()
                    val nsfw = uploadView.swNsfw.isChecked
                    val tags = strtags.split(Regex(", ?"), 0)

                    doAsync {
                        val mediaType = MediaType.parse("image/${file.extension}")
                        val client = OkHttpClient()

                        // Create multipart body (multipart was a bitch to figure out how it worked)
                        val builder = MultipartBody.Builder()
                        builder.setType(MultipartBody.FORM)
                        builder.addFormDataPart("image", file.nameWithoutExtension, RequestBody.create(mediaType, file))
                        tags.forEach { tag -> builder.addFormDataPart("tags[]", tag) }
                        builder.addFormDataPart("artist", artist)
                        builder.addFormDataPart("nsfw", nsfw.toString())
                        builder.setType(MediaType.parse("multipart/form-data")!!)
                        val requestBody = builder.build()

                        // Set the headers
                        val headers = Headers.Builder()
                                .add("Authorization", token)
                                .add("User-Agent", userAgent)
                                .build()

                        // Build the actual request
                        val request = Request.Builder()
                                .url("$baseUrl/images")
                                .headers(headers)
                                .post(requestBody)
                                .build()

                        // Execute our request
                        try {
                            val response = client.newCall(request).execute()
                            if (!response.isSuccessful) {
                                val nekoException = NekoException.Deserializer().deserialize(response.body()?.string() ?: "{\"message\": \"Request failed with unknown error\"}")
                                val msg = nekoException?.message ?: "Request failed with unknown error"
                                showSnackbar(nekoImages, this@NekoMain, msg, Snackbar.LENGTH_LONG)
                            } else {
                                showSnackbar(nekoImages, this@NekoMain, "Success uploading neko, awaiting approval of an admin", Snackbar.LENGTH_LONG)
                            }
                            response.close()
                        } catch (e: IOException) {
                            showSnackbar(nekoImages, this@NekoMain, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG)
                            Sentry.capture(e)
                        }
                    }
                }.create()

        uploadDialog.show()
    }

    @SuppressLint("InflateParams")
    private fun login() {
        if (!connected || !isConnected(this)) {
            showSnackbar(nekoImages, this, "No network connection", Snackbar.LENGTH_SHORT)
            return
        }

        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, deviceID)
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Login")

        val loginFactory = LayoutInflater.from(this)
        val loginView = loginFactory.inflate(R.layout.login_dialog, null)

        // Create login dialog
        val loginDialog = AlertDialog.Builder(this)
                .setView(loginView)
                .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
                .setNeutralButton("Register") { dialog, _ ->
                    // Dismiss old dialog
                    dialog.dismiss()

                    val registerFactory = LayoutInflater.from(this)
                    val registerView = registerFactory.inflate(R.layout.register_dialog, null)

                    // Create register dialog
                    val registerDialog = AlertDialog.Builder(this)
                            .setView(registerView)
                            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                            .setPositiveButton("Confirm") { _, _ ->
                                // Input checks
                                if (registerView.userInput.text.isNullOrEmpty() || registerView.passInput.text.isNullOrEmpty() || registerView.emailInput.text.isNullOrEmpty()) {
                                    showSnackbar(nekoImages, this, "Please complete all fields", Snackbar.LENGTH_LONG)
                                    return@setPositiveButton
                                }
                                if (registerView.userInput.text.contains("@")) {
                                    showSnackbar(nekoImages, this, "Usernames cannot contain an @ symbol", Snackbar.LENGTH_LONG)
                                    return@setPositiveButton
                                }
                                if (registerView.passInput.text.length < 8) {
                                    showSnackbar(nekoImages, this, "Password needs to be atleast 8 characters long", Snackbar.LENGTH_LONG)
                                    return@setPositiveButton
                                }
                                if (registerView.passInput.text.toString() != registerView.cPasswordInput.text.toString()) {
                                    showSnackbar(nekoImages, this, "Passwords do not match", Snackbar.LENGTH_LONG)
                                    return@setPositiveButton
                                }
                                if (registerView.ageCheckBox.isChecked.not()) {
                                    showSnackbar(nekoImages, this, "You must be at least 13 years old to make an account", Snackbar.LENGTH_LONG)
                                    return@setPositiveButton
                                }

                                // Register new user
                                Fuel.post("/register")
                                        .header(mapOf("Content-Type" to "application/json"))
                                        .body("""
                                            {
                                            "email": "${registerView.emailInput.text}",
                                            "username": "${registerView.userInput.text}",
                                            "password": "${registerView.passInput.text}"
                                            }
                                            """.trimMargin())
                                        .responseJson { _, response, result ->
                                            when (result) {
                                                is Result.Failure -> {
                                                    val (_, error) = result
                                                    if (error != null) {
                                                        when (error.response.statusCode) {
                                                            429 -> {
                                                                showSnackbar(nekoImages, this, "Too many requests, please wait a few seconds", Snackbar.LENGTH_LONG)
                                                            }
                                                            else -> {
                                                                val nekoException = NekoException.Deserializer().deserialize(error.errorData)
                                                                val msg = nekoException?.message ?: error.message ?: "Something went wrong"
                                                                showSnackbar(nekoImages, this, msg, Snackbar.LENGTH_LONG)
                                                                Sentry.capture(error)
                                                            }
                                                        }
                                                    }
                                                }
                                                is Result.Success -> {
                                                    if (response.statusCode == 201) {
                                                        showSnackbar(nekoImages, this, "A confirmation mail has been send to your email", Snackbar.LENGTH_LONG)
                                                    }
                                                }
                                            }
                                        }
                            }.create()
                    registerDialog.show()
                }
                .setPositiveButton("Login") { _, _ ->
                    if (loginView.usernameInput.text.isNullOrEmpty() || loginView.passwordInput.text.isNullOrEmpty()) {
                        showSnackbar(nekoImages, this, "Please complete all fields", Snackbar.LENGTH_LONG)
                        return@setPositiveButton
                    }

                    doAsync {
                        Fuel.post("/auth")
                                .header(mapOf("Content-Type" to "application/json"))
                                .body("{\"username\": \"${loginView.usernameInput.text}\", \"password\": \"${loginView.passwordInput.text}\"}")
                                .responseJson { _, _, result ->
                                    val (data, error) = result
                                    when (result) {
                                        is Result.Failure -> {
                                            if (error != null) {
                                                updateUI(false)
                                                when (error.response.statusCode) {
                                                    429 -> {
                                                        showSnackbar(nekoImages, this@NekoMain, "Too many requests, please wait a few seconds", Snackbar.LENGTH_LONG)
                                                    }
                                                    else -> {
                                                        val nekoException = NekoException.Deserializer().deserialize(error.errorData)
                                                        val msg = nekoException?.message ?: error.message ?: "Something went wrong"
                                                        showSnackbar(nekoImages, this@NekoMain, msg, Snackbar.LENGTH_LONG)
                                                        Sentry.capture(error)
                                                    }
                                                }
                                            }
                                        }
                                        is Result.Success -> {
                                            if (data != null) {
                                                // Try getting the token
                                                val token = try {
                                                    data.obj().getString("token")
                                                } catch (e: Throwable) {
                                                    Sentry.capture(e)
                                                    null
                                                }

                                                if (token != null) {
                                                    // Save token and update the base headers
                                                    sharedPreferences.edit().putString("token", token).apply()
                                                    FuelManager.instance.baseHeaders = mapOf(
                                                            "User-Agent" to userAgent,
                                                            "Authorization" to token
                                                    )
                                                } else {
                                                    showSnackbar(nekoImages, this@NekoMain, "Could not login", Snackbar.LENGTH_LONG)
                                                    return@responseJson
                                                }

                                                // Get the logged in user
                                                getCurrentUser()
                                                showSnackbar(nekoImages, this@NekoMain, "Success logging in", Snackbar.LENGTH_SHORT)
                                            }
                                        }
                                    }
                                }
                    }
                }.create()
        loginDialog.show()
    }

    // Update the UI depending on whether you're logged in
    private fun updateUI(success: Boolean) {
        val loginOut = nav_view.menu.findItem(R.id.login_out)
        val headerView = nav_view.getHeaderView(0)
        if (success) {
            isLoggedin = true
            headerView.headerTitle.text = user!!.username
            loginOut.title = getString(R.string.login_out, "Logout")
            loginOut.icon = getDrawable(R.drawable.ic_menu_logout)
        } else {
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, deviceID)
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "Logout")
            isLoggedin = false
            sharedPreferences.edit().remove("token").apply()
            headerView.headerTitle.text = "-"
            loginOut.title = getString(R.string.login_out, "Login")
            loginOut.icon = getDrawable(R.drawable.ic_menu_login)
            showSnackbar(nekoImages, this, "You're now logged out", Snackbar.LENGTH_SHORT)
        }
    }

    @SuppressLint("InflateParams")
    // Check network conn type
    private fun checkConnectionType(): Boolean {
        val conn = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = conn.activeNetworkInfo

        // If mobile network conn ask the user if he/she wants to switch to wifi
        if (netInfo?.type == ConnectivityManager.TYPE_MOBILE) {
            val factory = LayoutInflater.from(this)
            val view = factory.inflate(R.layout.alert_dialog, null)
            view.alertMessage.text = getString(R.string.network_usage_alert)

            val alertDialog = AlertDialog.Builder(this)
            .setView(view)
            .setNegativeButton("Close", null)
            .setPositiveButton("Switch") { _, _ ->
                val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifi.isWifiEnabled = true
                showSnackbar(nekoImages, this, "Enabled wifi, have fun browsing cute nekos", Snackbar.LENGTH_SHORT)
            }.create()

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
