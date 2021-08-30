@file:Suppress("DEPRECATION")

package xyz.kurozero.nekosmoe

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.RecyclerView
import xyz.kurozero.nekosmoe.adapter.NekosGridRecyclerAdapter
import xyz.kurozero.nekosmoe.adapter.file
import xyz.kurozero.nekosmoe.helper.*
import xyz.kurozero.nekosmoe.model.*
import kotlinx.android.synthetic.main.activity_main.*
import androidx.recyclerview.widget.GridLayoutManager
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.ImagePipelineConfig
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.dialog_alert.view.*
import kotlinx.android.synthetic.main.dialog_login.view.*
import kotlinx.android.synthetic.main.dialog_register.view.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.jetbrains.anko.selector
import org.json.JSONException
import java.util.*

// Permissions needed to save and upload images
val permissions = arrayOf(
    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
    android.Manifest.permission.READ_EXTERNAL_STORAGE
)

lateinit var sharedPreferences: SharedPreferences
lateinit var typeFace: Typeface
lateinit var token: String
lateinit var adapter: NekosGridRecyclerAdapter
lateinit var httpClient: OkHttpClient
var allNekos: MutableList<Neko>? = null
var user: User? = null
var connected = true
var isLoggedin = false

class MainActivity : AppCompatActivity(), ConnectivityReceiver.ConnectivityReceiverListener, NavigationView.OnNavigationItemSelectedListener {

    private var scrollListener: EndlessRecyclerViewScrollListener? = null

    // Pagination and sorting stuff
    private var toSkip = 0
    private var page = 1
    private var isNew = true
    private var init = true
    private var sort = "newest"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val (version, versionCode) = Api.getVersions(this)
        Api.version = version
        Api.versionCode = versionCode
        Api.userAgent = "NekosApp/v$version (https://github.com/Pepijn98/Nekos)"

        httpClient = OkHttpClient()

        // Do some fonts magic
        typeFace = Typeface.createFromAsset(assets, "fonts/nunito.ttf")
        toolbar_title.typeface = typeFace
        FontsOverride.setDefaultFont(this, "DEFAULT", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "MONOSPACE", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "SERIF", "fonts/nunito.ttf")
        FontsOverride.setDefaultFont(this, "SANS_SERIF", "fonts/nunito.ttf")

        // If we don't yet have our permissions ask for them
        if (!hasPermissions(this, permissions)) {
            ActivityCompat.requestPermissions(this, permissions, 999)
        }

        // Get our shared preferences, this includes the user and token if the user has logged in
        sharedPreferences = getSharedPreferences("nekos.moe", Context.MODE_PRIVATE)
        token = sharedPreferences.getString("token", "") ?: ""
        val userstr = sharedPreferences.getString("user", "") ?: ""

        // Populate public variables
        user = if (userstr.isNotEmpty())
            Api.gson.fromJson(userstr, User::class.java) else null
        isLoggedin = token.isNotEmpty()

        // Add our drawer
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        tvAbout.text = Html.fromHtml("""
            <p>© ${Calendar.getInstance().get(Calendar.YEAR)} — <a href="https://vdbroek.dev">Pepijn</a> | v${Api.version} (${Api.versionCode})<br/>
            Made possible with <a href="https://nekos.moe">nekos.moe</a><br/></p>
        """.trimIndent(), Html.FROM_HTML_MODE_LEGACY)
        tvAbout.movementMethod = LinkMovementMethod.getInstance()

        nav_view.setNavigationItemSelectedListener(this)
        val loginOut = nav_view.menu.findItem(R.id.login_out)

        // Change text based on whether you're logged in
        val headerView = nav_view.getHeaderView(0)
        if (isLoggedin) {
            loginOut.title = getString(R.string.login_out, "Logout")
            loginOut.icon = getDrawable(R.drawable.menu_logout)
            headerView.headerTitle.text = user?.username ?: "-"
        } else {
            loginOut.title = getString(R.string.login_out, "Login")
            loginOut.icon = getDrawable(R.drawable.menu_login)
            headerView.headerTitle.text = "-"
        }

        // Set base url for api requests
        FuelManager.instance.basePath = Api.baseUrl

        FuelManager.instance.baseHeaders = mapOf("User-Agent" to Api.userAgent)

        val rvItems = findViewById<RecyclerView>(R.id.list)
        rvItems.addItemDecoration(GridItemDecoration(80, 2))

        allNekos = getNekos(false)
        adapter = NekosGridRecyclerAdapter()
        if (allNekos != null) adapter.setNekosList(allNekos!!)
        rvItems.adapter = adapter

        val layoutManager = GridLayoutManager(this, 2)
        rvItems.layoutManager = layoutManager

        // Retain an instance so that you can call `resetState()` for fresh searches
        scrollListener = object : EndlessRecyclerViewScrollListener(layoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                val moreNekos = getNekos(true)
                val curSize = adapter.itemCount
                if (moreNekos != null) {
                    allNekos!!.addAll(moreNekos)
                    view.post { adapter.notifyItemRangeInserted(curSize, allNekos!!.size - 1) }
                }
            }
        }
        // Adds the scroll listener to RecyclerView
        rvItems.addOnScrollListener(scrollListener!!)

        // Initialize fresco for loading fullscreen images
        val config = ImagePipelineConfig.newBuilder(this)
            .setProgressiveJpegConfig(SimpleProgressiveJpegConfig())
            .setResizeAndRotateEnabledForNetwork(true)
            .setDownsampleEnabled(true)
            .build()
        Fresco.initialize(this, config)

        GlobalScope.launch {
            @Suppress("BlockingMethodInNonBlockingContext")
            Thread.sleep(5_000)
            init = false
        }
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
        if (!connected) {
            Snackbar.make(activityMain, "Lost network connection", Snackbar.LENGTH_SHORT).show()
        } else {
            if (!init) Snackbar.make(activityMain, "Network connection restored", Snackbar.LENGTH_SHORT).show()
        }
    }

    // Whenever we get a result from an intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        // If the user canceled the action just return on do nothing
        if (resultCode == RESULT_CANCELED) return
        when (requestCode) {
            997 -> { // Delete temp file after sharing an image
                try {
                    if (file.exists() && resultCode == RESULT_OK) {
                        Thread.sleep(1_000) // Stupid result doesn't actually wait for the intent to finish sending...
                        val isDeleted = file.delete()
                        if (isDeleted.not())
                        Snackbar.make(activityMain, "Could not delete shared file", Snackbar.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(activityMain, e.message ?: "Something went wrong", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    // Whenever we requested the needed permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            999 -> {
                // Check whether the user granted us the needed permissions
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(activityMain, "The app can now save images", Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(activityMain, "The app can not save images", Snackbar.LENGTH_SHORT).show()
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
//                val rand = Math.floor((Math.random() * 8) + 1).toInt()
//                showAd(rand, interstitialAd.isLoaded, user?.id)
                if (!isLoggedin) {
                    Snackbar.make(activityMain, "Login before using this action", Snackbar.LENGTH_LONG).show()
                    return false
                }

                getCurrentUser()
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.sort -> {
                // The sorting options the user can choose from
                val buttons = listOf("New", "Old", "Likes")

                // Set these back to the default to make requestNeko start over again with the new sorting options
                isNew = true
                page = 1

                // Dialog with options of whatever the user wishes to see
                val rvItems = findViewById<RecyclerView>(R.id.list)
                selector(null, buttons) { _, i ->
                    when (i) {
                        0 -> {
                            sort = "newest"
                            val nekos = getNekos(false)
                            val curSize = adapter.itemCount
                            if (nekos != null) {
                                allNekos!!.removeAll(allNekos!!)
                                allNekos!!.addAll(nekos)
                                rvItems.post { adapter.notifyItemRangeInserted(curSize, allNekos!!.size - 1) }
                                adapter.setNekosList(allNekos!!)
                            }
                            scrollListener?.resetState()
                        }
                        1 -> {
                            sort = "oldest"
                            val nekos = getNekos(false)
                            val curSize = adapter.itemCount
                            if (nekos != null) {
                                allNekos!!.removeAll(allNekos!!)
                                allNekos!!.addAll(nekos)
                                rvItems.post { adapter.notifyItemRangeInserted(curSize, allNekos!!.size - 1) }
                                adapter.setNekosList(allNekos!!)
                            }
                            scrollListener?.resetState()
                        }
                        2 -> {
                            sort = "likes"
                            val nekos = getNekos(false)
                            val curSize = adapter.itemCount
                            if (nekos != null) {
                                allNekos!!.removeAll(allNekos!!)
                                allNekos!!.addAll(nekos)
                                rvItems.post { adapter.notifyItemRangeInserted(curSize, allNekos!!.size - 1) }
                                adapter.setNekosList(allNekos!!)
                            }
                            scrollListener?.resetState()
                        }
                        else -> return@selector
                    }
                }
            }
            R.id.upload -> Snackbar.make(activityMain, "Not yet implemented", Snackbar.LENGTH_SHORT).show()
        }

        // Close drawer again when an item is clicked
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    // Update the UI depending on whether you're logged in
    private fun updateUI(success: Boolean) {
        val loginOut = nav_view.menu.findItem(R.id.login_out)
        val headerView = nav_view.getHeaderView(0)
        if (success) {
            isLoggedin = true
            headerView.headerTitle.text = user!!.username
            loginOut.title = getString(R.string.login_out, "Logout")
            loginOut.icon = getDrawable(R.drawable.menu_logout)
        } else {
            isLoggedin = false
            token = ""
            user = null
            sharedPreferences.edit()
                .remove("token")
                .remove("user")
                .apply()
            headerView.headerTitle.text = "-"
            loginOut.title = getString(R.string.login_out, "Login")
            loginOut.icon = getDrawable(R.drawable.menu_login)
            Snackbar.make(activityMain, "You're now logged out", Snackbar.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("InflateParams")
    private fun login() {
        if (!connected || !isConnected(this)) {
            Snackbar.make(activityMain, "No network connection", Snackbar.LENGTH_SHORT).show()
            return
        }

        val loginFactory = LayoutInflater.from(this)
        val loginView = loginFactory.inflate(R.layout.dialog_login, null)

        // Create login dialog
        val loginDialog = AlertDialog.Builder(this)
            .setView(loginView)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .setNeutralButton("Register") { dialog, _ ->
                // Dismiss old dialog
                dialog.dismiss()

                val registerFactory = LayoutInflater.from(this)
                val registerView = registerFactory.inflate(R.layout.dialog_register, null)

                // Create register dialog
                val registerDialog = AlertDialog.Builder(this)
                    .setView(registerView)
                    .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                    .setPositiveButton("Confirm") { _, _ ->
                        // Input checks
                        if (registerView.userInput.text.isNullOrEmpty() || registerView.passInput.text.isNullOrEmpty() || registerView.emailInput.text.isNullOrEmpty()) {
                            Snackbar.make(activityMain, "Please complete all fields", Snackbar.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                        if (registerView.userInput.text.contains("@")) {
                            Snackbar.make(activityMain, "Usernames cannot contain an @ symbol", Snackbar.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                        if (registerView.passInput.text.length < 8) {
                            Snackbar.make(activityMain, "Password needs to be atleast 8 characters long", Snackbar.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                        if (registerView.passInput.text.toString() != registerView.cPasswordInput.text.toString()) {
                            Snackbar.make(activityMain, "Passwords do not match", Snackbar.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                        if (registerView.ageCheckBox.isChecked.not()) {
                            Snackbar.make(activityMain, "You must be at least 13 years old to make an account", Snackbar.LENGTH_LONG).show()
                            return@setPositiveButton
                        }

                        val reqbody = """
                        {
                            "email": "${registerView.emailInput.text}",
                            "username": "${registerView.userInput.text}",
                            "password": "${registerView.passInput.text}"
                        }
                        """.trimMargin()
                        // Register new user
                        Fuel.post("/register")
                            .header(mapOf("Content-Type" to "application/json"))
                            .body(reqbody)
                            .responseString { _, response, result ->
                                when (result) {
                                    is Result.Failure -> {
                                        val (_, fuelError) = result
                                        if (fuelError != null) {
                                            val nekoException = Api.gson.fromJson(fuelError.response.responseMessage, NekoException::class.java)
                                            val msg = nekoException.message ?: fuelError.message ?: "Something went wrong"
                                            Snackbar.make(activityMain, msg, Snackbar.LENGTH_LONG).show()
                                        }
                                    }
                                    is Result.Success -> {
                                        if (response.statusCode == 201) {
                                            Snackbar.make(activityMain, "A confirmation mail has been send to your email", Snackbar.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                    }.create()
                registerDialog.show()
            }
            .setPositiveButton("Login") { _, _ ->
                if (loginView.usernameInput.text.isNullOrEmpty() || loginView.passwordInput.text.isNullOrEmpty()) {
                    Snackbar.make(activityMain, "Please complete all fields", Snackbar.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                GlobalScope.launch(Dispatchers.IO) {
                    val reqbody = "{\"username\": \"${loginView.usernameInput.text}\", \"password\": \"${loginView.passwordInput.text}\"}"
                    Fuel.post("/auth")
                        .header(mapOf("Content-Type" to "application/json"))
                        .body(reqbody)
                        .responseString { _, _, result ->
                            val (data, error) = result
                            when (result) {
                                is Result.Failure -> {
                                    if (error != null) {
                                        updateUI(false)
                                        val nekoException = Api.gson.fromJson(error.response.responseMessage, NekoException::class.java)
                                        val msg = nekoException.message ?: error.message ?: "Something went wrong"
                                        Snackbar.make(activityMain, msg, Snackbar.LENGTH_LONG).show()
                                    }
                                }
                                is Result.Success -> {
                                    if (data != null) {
                                        // Try getting the token
                                        val response = Api.gson.fromJson(data, LoginResponse::class.java)

                                        if (response != null) {
                                            // Save token
                                            sharedPreferences.edit().putString("token", response.token).apply()
                                            token = response.token
                                            // Get the logged in user
                                            getCurrentUser()
                                            Snackbar.make(activityMain, "Success logging in", Snackbar.LENGTH_SHORT).show()
                                        } else {
                                            Snackbar.make(activityMain, "Could not login", Snackbar.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Snackbar.make(activityMain, "Could not login", Snackbar.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                }
            }.create()
        loginDialog.show()
    }

    // Get the currently logged in user
    private fun getCurrentUser() {
        if (connected || isConnected(this)) {
            GlobalScope.launch(Dispatchers.IO) {
                "user/@me".httpGet()
                    .header(mapOf("Authorization" to token))
                    .responseString { _, _, result ->
                        val (resp, error) = result
                        when (result) {
                            is Result.Failure -> {
                                if (error != null) {
                                    val nekoException = Api.gson.fromJson(error.response.responseMessage, NekoException::class.java)
                                    val msg = nekoException.message ?: error.message ?: "Something went wrong"
                                    Snackbar.make(activityMain, msg, Snackbar.LENGTH_LONG).show()
                                }
                            }
                            is Result.Success -> {
                                if (resp != null) {
                                    try {
                                        // Deserialize and save the logged in user for later
                                        user = Api.gson.fromJson(resp, UserResponse::class.java).user
                                        sharedPreferences.edit().putString("user", Api.gson.toJson(user)).apply()
                                        GlobalScope.launch(Dispatchers.Main) { updateUI(true) } // Update UI to show that we're logged in
                                    } catch (e: JSONException) {
                                        Snackbar.make(activityMain, e.message ?: "A JSON exception occured", Snackbar.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun getNekos(next: Boolean): MutableList<Neko>? = runBlocking {
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
        val oldSkip = toSkip
        toSkip = if (isNew || page <= 1) {
            0
        } else {
            if (next) toSkip + 50 else toSkip - 50
        }

        // If the page is less or same as 1 and is not new and the old page is not 2 we do nothing
        // This is to prevent making useless requests
        if (page <= 1 && !isNew && oldPage != 2) return@runBlocking null
        val (response, exception) = Api.requestNekosAsync(toSkip, sort).await()
        if (isNew) isNew = false
        return@runBlocking when {
            response != null -> response.images
            exception != null -> {
                Snackbar.make(activityMain, exception.message ?: "ERROR", Snackbar.LENGTH_LONG).show()
                toSkip = oldSkip
                page = oldPage
                null
            }
            else -> null
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
            val view = factory.inflate(R.layout.dialog_alert, null)
            view.alertMessage.text = getString(R.string.network_usage_alert)

            val alertDialog = AlertDialog.Builder(this)
                .setView(view)
                .setNegativeButton("Close", null)
                .setPositiveButton("Switch") { _, _ ->
                    val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wifi.isWifiEnabled = true
                    Snackbar.make(activityMain, "Enabled wifi, have fun browsing cute nekos", Snackbar.LENGTH_SHORT).show()
                }.create()

            alertDialog.show()
            return true
        }
        return false
    }
}