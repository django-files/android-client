package com.djangofiles.djangofiles

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.djangofiles.djangofiles.databinding.ActivityMainBinding
import com.djangofiles.djangofiles.db.Server
import com.djangofiles.djangofiles.db.ServerDao
import com.djangofiles.djangofiles.db.ServerDatabase
import com.djangofiles.djangofiles.ui.home.HomeViewModel
import com.djangofiles.djangofiles.widget.WidgetProvider
import com.djangofiles.djangofiles.work.DAILY_WORKER_CONSTRAINTS
import com.djangofiles.djangofiles.work.DailyWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>>

    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    //private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
    //    Log.d("SharedPreferences", "OnSharedPreferenceChangeListener: $key")
    //    //val value = prefs.getString(key, "")
    //    if (key == "saved_url") {
    //        val value = prefs.getString(key, "")
    //        Log.i("SharedPreferences", "value: $value")
    //
    //        Log.i("SharedPreferences", "Updating Widget")
    //        val appWidgetManager = AppWidgetManager.getInstance(this)
    //        val widgetComponent = ComponentName(this, WidgetProvider::class.java)
    //        val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
    //        val intent = Intent(this, WidgetProvider::class.java).apply {
    //            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    //            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
    //        }
    //        this.sendBroadcast(intent)
    //    }
    //}

    override fun onDestroy() {
        Log.d("Main[onDestroy]", "ON DESTROY")
        // TODO: Determine if this is necessary...
        //preferences.unregisterOnSharedPreferenceChangeListener(listener)
        super.onDestroy()
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Main[onCreate]", "savedInstanceState: ${savedInstanceState?.size()}")
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NavHostFragment
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        // Start Destination
        if (savedInstanceState == null) {
            val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
            val startPreference = preferences.getString("start_destination", null)
            Log.d("Main[onCreate]", "startPreference: $startPreference")
            val startDestination =
                if (startPreference == "files") R.id.nav_item_files else R.id.nav_item_home
            navGraph.setStartDestination(startDestination)
            navController.graph = navGraph
        }

        // Bottom Navigation
        val bottomNav = binding.appBarMain.contentMain.bottomNav
        bottomNav.setupWithNavController(navController)

        // Navigation Drawer
        binding.navView.setupWithNavController(navController)

        // Destinations w/ a Parent Item
        val destinationToBottomNavItem = mapOf(
            R.id.nav_item_file_preview to R.id.nav_item_files,
            R.id.nav_item_settings_widget to R.id.nav_item_settings
        )
        // Destination w/ No Parent
        val hiddenDestinations = setOf(
            R.id.nav_item_upload,
            R.id.nav_item_upload_multi,
            R.id.nav_item_short,
            R.id.nav_item_text
        )
        // Implement Navigation Hacks Because.......Android?
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("addOnDestinationChangedListener", "destination: ${destination.label}")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            val destinationId = destination.id
            if (destinationId in hiddenDestinations) {
                Log.d("addOnDestinationChangedListener", "Set bottomNav to Hidden Item")
                bottomNav.menu.findItem(R.id.nav_wtf).isChecked = true
                return@addOnDestinationChangedListener
            }
            val matchedItem = destinationToBottomNavItem[destinationId]
            if (matchedItem != null) {
                Log.d("addOnDestinationChangedListener", "matched nav item: $matchedItem")
                bottomNav.menu.findItem(matchedItem).isChecked = true
                val menu = binding.navView.menu
                for (i in 0 until menu.size) {
                    val item = menu[i]
                    item.isChecked = item.itemId == matchedItem
                }
            }
        }

        // Set Default Preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_widget, false)
        val uniqueID = preferences.getString("unique_id", null)
        Log.d("Main[onCreate]", "uniqueID: $uniqueID")
        if (uniqueID.isNullOrEmpty()) {
            val uuid = UUID.randomUUID().toString()
            Log.i("Main[onCreate]", "SETTING NEW UUID: $uuid")
            preferences.edit { putString("unique_id", uuid) }
        }

        //// Initialize Shared Preferences Listener
        //Log.d(LOG_TAG, "Initialize Shared Preferences Listener")
        //preferences.registerOnSharedPreferenceChangeListener(listener)

        // Update Status Bar
        window.statusBarColor = Color.TRANSPARENT
        //WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // Set Global Left/Right System Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarMain.contentMain.contentMainLayout) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            Log.i("Main[ViewCompat]", "bars: $bars")
            v.updatePadding(left = bars.left, right = bars.right)
            insets
        }

        //binding.drawerLayout.setStatusBarBackgroundColor(Color.TRANSPARENT)
        //WindowCompat.setDecorFitsSystemWindows(window, false)
        //window.decorView.setOnApplyWindowInsetsListener { view, insets -> insets }

        // Update Navigation Bar
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false)
        }

        // Update Header Padding
        val headerView = binding.navView.getHeaderView(0)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            Log.d("ViewCompat", "binding.root: top: ${bars.top}")
            if (bars.top > 0) {
                headerView.updatePadding(top = bars.top)
            }
            insets
        }

        // Update Header Text
        val packageInfo = packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d("Main[onCreate]", "versionName: $versionName")
        val versionTextView = headerView.findViewById<TextView>(R.id.header_version)
        versionTextView.text = "v${versionName}"

        // TODO: Improve initialization of the WorkRequest
        val workInterval = preferences.getString("work_interval", null) ?: "0"
        Log.i("Main[onCreate]", "workInterval: $workInterval")
        if (workInterval != "0") {
            val workRequest =
                PeriodicWorkRequestBuilder<DailyWorker>(workInterval.toLong(), TimeUnit.MINUTES)
                    .setConstraints(DAILY_WORKER_CONSTRAINTS)
                    .build()
            Log.i("Main[onCreate]", "workRequest: $workRequest")
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } else {
            // TODO: Confirm this is necessary...
            Log.i("Main[onCreate]", "Ensuring Work is Disabled")
            WorkManager.getInstance(this).cancelUniqueWork("app_worker")
        }

        // Handle Custom Navigation Items
        val itemPathMap = mapOf(
            R.id.nav_site_home to "",
            R.id.nav_site_files to "files/",
            R.id.nav_site_gallery to "gallery/",
            R.id.nav_site_albums to "albums/",
            R.id.nav_site_shorts to "shorts/",
            R.id.nav_site_settings_user to "settings/user/",
            R.id.nav_site_settings_site to "settings/site/",
        )
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            val path = itemPathMap[menuItem.itemId]
            Log.d("setNavigationItemSelectedListener", "path: $path")
            if (path != null) {
                Log.d("Drawer", "path: $path")
                val savedUrl = preferences.getString("saved_url", null)
                Log.d("Drawer", "savedUrl: $savedUrl")
                val url = "${savedUrl}/${path}"
                Log.d("Drawer", "Click URL: $url")
                val viewModel: HomeViewModel by viewModels()
                Log.d("Drawer", "viewModel: $viewModel")
                val webViewUrl = viewModel.webViewUrl.value
                Log.d("Drawer", "webViewUrl: $webViewUrl")
                if (webViewUrl != url) {
                    Log.i("Drawer", "WEB VIEW - viewModel.navigateTo: $url")
                    viewModel.navigateTo(url)
                }
                if (navController.currentDestination?.id != R.id.nav_item_home) {
                    Log.d("Drawer", "NAVIGATE: nav_item_home")
                    // NOTE: This is the correct navigation call...
                    val homeMenuItem = binding.navView.menu.findItem(R.id.nav_item_home)
                    NavigationUI.onNavDestinationSelected(homeMenuItem, navController)
                }
                binding.drawerLayout.closeDrawers()
                true
            } else if (menuItem.itemId == R.id.nav_item_upload) {
                Log.d("Drawer", "nav_item_upload")
                filePickerLauncher.launch(arrayOf("*/*"))
                binding.drawerLayout.closeDrawers()
                true
            } else {
                val handled = NavigationUI.onNavDestinationSelected(menuItem, navController)
                Log.d("Drawer", "ELSE - handled: $handled")
                //if (handled) {
                binding.drawerLayout.closeDrawers()
                //}
                handled
            }
        }

        // File Picker for UPLOAD_FILE Intent and Shortcut
        filePickerLauncher =
            registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                Log.d("filePickerLauncher", "uris: $uris")
                if (uris.size > 1) {
                    Log.i("filePickerLauncher", "MULTI!")
                    showMultiPreview(uris as ArrayList<Uri>)
                } else if (uris.size == 1) {
                    Log.i("filePickerLauncher", "SINGLE!")
                    showPreview(uris[0])
                } else {
                    Log.w("filePickerLauncher", "No Files Selected!")
                    //Toast.makeText(this, "No Files Selected!", Toast.LENGTH_SHORT).show()
                }
            }

        MediaCache.initialize(this)

        // Only Handel Intent Once Here after App Start
        if (savedInstanceState?.getBoolean("intentHandled") != true) {
            onNewIntent(intent)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("intentHandled", true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("onNewIntent", "intent: $intent")
        val data = intent.data
        val action = intent.action
        Log.d("onNewIntent", "${action}: $data")

        val savedUrl = preferences.getString("saved_url", null)
        val authToken = preferences.getString("auth_token", null)
        Log.d("onNewIntent", "savedUrl: $savedUrl")
        Log.d("onNewIntent", "authToken: $authToken")

        Log.i("onNewIntent", "drawerLayout.closeDrawers")
        binding.drawerLayout.closeDrawers()

        // Check Auth First
        if (savedUrl.isNullOrEmpty()) {
            Log.i("onNewIntent", "LOCK DRAWER: savedUrl.isNullOrEmpty")
            setDrawerLockMode(false)
        }
        Log.d("onNewIntent", "data?.host: ${data?.host}")
        val noAuthHosts = setOf("oauth", "authorize")
        Log.d("onNewIntent", "noAuthHosts: $noAuthHosts")
        if (data?.host !in noAuthHosts && savedUrl.isNullOrEmpty()) {
            Log.i("onNewIntent", "Missing Saved URL or Token! Showing Login...")
            //setDrawerLockMode(false)
            navController.navigate(
                R.id.nav_item_login, null, NavOptions.Builder()
                    .setPopUpTo(navController.graph.id, true)
                    .build()
            )
            return
        }

        // Reject Calendar URI due to permissions
        val isCalendarUri = data != null &&
                data.authority?.contains("calendar") == true &&
                listOf("/events", "/calendars", "/time").any { data.path?.contains(it) == true }
        Log.d("onNewIntent", "isCalendarUri: $isCalendarUri")
        if (isCalendarUri) {
            Log.i("onNewIntent", "Calendar Links Not Supported!")
            Toast.makeText(this, "Calendar Links Not Supported!", Toast.LENGTH_LONG).show()
            return
        }

        if (action == Intent.ACTION_MAIN) {
            Log.d("onNewIntent", "ACTION_MAIN")

            // TODO: Cleanup the logic for handling MAIN intent...
            val currentDestinationId = navController.currentDestination?.id
            Log.d("onNewIntent", "currentDestinationId: $currentDestinationId")
            val fromShortcut = intent.getStringExtra("fromShortcut")
            Log.d("onNewIntent", "fromShortcut: $fromShortcut")

            when (currentDestinationId) {
                R.id.nav_item_upload, R.id.nav_item_upload_multi, R.id.nav_item_short, R.id.nav_item_text -> {
                    Log.i("onNewIntent", "Navigating away from preview page...")
                    navController.navigate(
                        navController.graph.startDestinationId, null, NavOptions.Builder()
                            .setPopUpTo(navController.graph.id, true)
                            .build()
                    )
                }
            }

            // TODO: Determine if this needs to be in the above if/else
            if (fromShortcut == "upload") {
                Log.d("onNewIntent", "filePickerLauncher.launch")
                filePickerLauncher.launch(arrayOf("*/*"))
            }

        } else if (action == "UPLOAD_FILE") {
            Log.d("onNewIntent", "UPLOAD_FILE")

            filePickerLauncher.launch(arrayOf("*/*"))

        } else if (action == "FILE_LIST") {
            Log.d("onNewIntent", "FILE_LIST")

            if (navController.currentDestination?.id != R.id.nav_item_files) {
                navController.navigate(R.id.nav_item_files)
            }

        } else if (action == Intent.ACTION_SEND) {
            Log.d("onNewIntent", "ACTION_SEND")

            val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            Log.d("onNewIntent", "File URI: $fileUri")

            val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
            Log.d("onNewIntent", "extraText: ${extraText?.take(100)}")

            if (fileUri == null && !extraText.isNullOrEmpty()) {
                Log.d("onNewIntent", "SEND TEXT DETECTED")
                //if (extraText.lowercase().startsWith("http")) {
                //if (Patterns.WEB_URL.matcher(extraText).matches()) {
                if (isTextUrl(extraText)) {
                    Log.i("onNewIntent", "URL DETECTED: $extraText")
                    val bundle = Bundle().apply { putString("url", extraText) }
                    navController.navigate(
                        R.id.nav_item_short, bundle, NavOptions.Builder()
                            .setPopUpTo(navController.graph.id, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                } else {
                    Log.i("onNewIntent", "PLAIN TEXT DETECTED")
                    val bundle = Bundle().apply { putString("text", extraText) }
                    navController.navigate(
                        R.id.nav_item_text, bundle, NavOptions.Builder()
                            .setPopUpTo(navController.graph.id, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                }
            } else {
                showPreview(fileUri)
            }

        } else if (action == Intent.ACTION_SEND_MULTIPLE) {
            Log.d("onNewIntent", "ACTION_SEND_MULTIPLE")

            val fileUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            Log.d("onNewIntent", "fileUris: $fileUris")
            if (fileUris == null) {
                Toast.makeText(this, "Error Parsing URI!", Toast.LENGTH_LONG).show()
                Log.w("onNewIntent", "fileUris is null")
                return
            }
            showMultiPreview(fileUris)

        } else if (action == Intent.ACTION_VIEW) {
            Log.d("onNewIntent", "ACTION_VIEW")

            if (data == null) {
                Toast.makeText(this, "That's a Bug!", Toast.LENGTH_LONG).show()
                Log.e("onNewIntent", "BUG: UNKNOWN action: $action")
                return
            }
            if ("djangofiles" == data.scheme) {
                Log.d("onNewIntent", "scheme: ${data.scheme}")
                Log.d("onNewIntent", "host: ${data.host}")
                if ("serverlist" == data.host) {
                    Log.d("onNewIntent", "djangofiles://serverlist")
                    if (navController.currentDestination?.id != R.id.nav_item_settings) {
                        navController.navigate(R.id.nav_item_settings)
                    }
                } else if ("logout" == data.host) {
                    processLogout()
                } else if ("oauth" == data.host) {
                    processOauth(data)
                } else if ("authorize" == data.host) {
                    Log.w("onNewIntent", "AUTHORIZE QR CODE - DO IT MAN!")
                    val url = data.getQueryParameter("url")
                    val signature = data.getQueryParameter("signature")
                    Log.d("onNewIntent", "url: $url")
                    Log.d("onNewIntent", "signature: $signature")

                    val bundle = Bundle().apply {
                        putString("url", url)
                        putString("signature", signature)
                    }
                    navController.navigate(
                        R.id.nav_item_authorize, bundle, NavOptions.Builder()
                            .setPopUpTo(navController.graph.id, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )

                } else {
                    Toast.makeText(this, "Unknown DeepLink!", Toast.LENGTH_LONG).show()
                    Log.w("onNewIntent", "Unknown DeepLink!")
                }
            } else {
                Log.d("onNewIntent", "File URI: $data")
                showPreview(data)
            }
        } else {
            Toast.makeText(this, "That's a Bug!", Toast.LENGTH_LONG).show()
            Log.e("onNewIntent", "BUG: UNKNOWN action: $action")
        }
    }

    override fun onStop() {
        Log.d("Main[onStop]", "MainActivity - onStop")
        // Update Widget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, WidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        WidgetProvider().onUpdate(this, appWidgetManager, ids)
        super.onStop()
    }

    //private fun navigateIntent(destination: Int){
    //    val args = Bundle().apply { putParcelable("intent", intent) }
    //    Log.d("Main[onCreate]", "args: $args")
    //    navController.popBackStack(R.id.nav_graph, true)
    //    navController.navigate(
    //        destination, args, NavOptions.Builder()
    //            .setPopUpTo(R.id.nav_item_home, true)
    //            .setLaunchSingleTop(true)
    //            .build()
    //    )
    //}

    private fun showMultiPreview(fileUris: ArrayList<Uri>) {
        Log.d("Main[showMultiPreview]", "fileUris: $fileUris")
        //fileUris.sort()
        val bundle = Bundle().apply { putParcelableArrayList("fileUris", fileUris) }
        navController.navigate(
            R.id.nav_item_upload_multi, bundle, NavOptions.Builder()
                .setPopUpTo(navController.graph.id, true)
                .setLaunchSingleTop(true)
                .build()
        )
    }

    private fun showPreview(uri: Uri?) {
        Log.d("Main[showPreview]", "uri: $uri")
        val bundle = Bundle().apply { putString("uri", uri.toString()) }
        navController.navigate(
            R.id.nav_item_upload, bundle, NavOptions.Builder()
                .setPopUpTo(navController.graph.id, true)
                .setLaunchSingleTop(true)
                .build()
        )
    }

    private fun processOauth(data: Uri) {
        // TODO: Can do this in a fragment to show loading screen/errors eventually...
        Log.d("processOauth", "processOauth: data: $data")
        val token = data.getQueryParameter("token")
        val sessionKey = data.getQueryParameter("session_key")
        val error = data.getQueryParameter("error")

        // TODO: Handle null data and errors
        Log.d("processOauth", "token: $token")
        Log.d("processOauth", "session_key: $sessionKey")
        Log.d("processOauth", "error: $error")

        // TODO: Determine how to better get oauthUrl
        val oauthUrl = preferences.getString("oauth_host", null)
        Log.d("processOauth", "oauthUrl: $oauthUrl")
        if (oauthUrl == null) {
            // TODO: Handle this error...
            Log.e("processOauth", "oauthUrl is null")
            return
        }

        preferences.edit {
            putString("saved_url", oauthUrl)
            putString("auth_token", token)
        }
        lifecycleScope.launch {
            val dao: ServerDao =
                ServerDatabase.getInstance(this@MainActivity).serverDao()
            try {
                withContext(Dispatchers.IO) {
                    dao.addOrUpdate(Server(url = oauthUrl, token = token!!, active = true))
                }
            } catch (e: Exception) {
                // TODO: This needs to be handled...
                Log.e("processOauth", "Exception: $e")
            }
        }

        val cookieManager = CookieManager.getInstance()
        //cookieManager.setAcceptThirdPartyCookies(webView, true)
        val cookie = "sessionid=$sessionKey; Path=/; HttpOnly; Secure"
        Log.d("processOauth", "cookie: $cookie")

        val uri = oauthUrl.toUri()
        val origin = "${uri.scheme}://${uri.authority}"
        Log.d("processOauth", "origin: $origin")
        cookieManager.setCookie(origin, cookie) { cookieManager.flush() }

        Log.d("processOauth", "navigate: startDestinationId")
        setDrawerLockMode(true)
        navController.navigate(
            navController.graph.startDestinationId, null, NavOptions.Builder()
                .setPopUpTo(navController.graph.id, true)
                .build()
        )
    }

    private fun processLogout() {
        val savedUrl = preferences.getString("saved_url", null)
        Log.d("processLogout", "savedUrl: $savedUrl")
        preferences.edit {
            remove("saved_url")
            remove("auth_token")
        }
        val dao: ServerDao = ServerDatabase.getInstance(this).serverDao()
        lifecycleScope.launch {
            if (savedUrl != null) {
                Log.d("processLogout", "dao.delete: $savedUrl")
                val server = Server(url = savedUrl)
                Log.d("processLogout", "server: $server")
                withContext(Dispatchers.IO) { dao.delete(server) }
            }
            val servers = withContext(Dispatchers.IO) { dao.getAll() }
            Log.d("processLogout", "servers: $servers")
            if (servers.isEmpty()) {
                Log.i("processLogout", "LOCK DRAWER: NO MORE SERVERS")
                setDrawerLockMode(false)
                navController.navigate(
                    R.id.nav_item_login, null, NavOptions.Builder()
                        .setPopUpTo(navController.graph.id, true)
                        .build()
                )
            } else {
                Log.d("processLogout", "MORE SERVERS - ACTIVATE ONE")
                //servers.firstOrNull()?.let { dao.activate(it.url) }
                val server = servers.first()
                Log.d("processLogout", "server: $server")
                withContext(Dispatchers.IO) { dao.activate(server.url) }

                preferences.edit().apply {
                    putString("saved_url", server.url)
                    putString("auth_token", server.token)
                    apply()
                }

                Log.d("processLogout", "navigate: nav_item_login")
                // TODO: Determine proper navigate call here...
                //navController.navigate(R.id.nav_item_settings)
                navController.navigate(
                    R.id.nav_item_login, null, NavOptions.Builder()
                        .setPopUpTo(navController.graph.startDestinationId, true)
                        .build()
                )
            }
        }
    }

    private fun isTextUrl(input: String): Boolean {
        val url = input.toHttpUrlOrNull() ?: return false
        if (input != url.toString()) return false
        if (url.scheme !in listOf("http", "https")) return false
        if (url.host.isBlank()) return false
        if (url.toString().length > 2048) return false
        return true
    }

    fun setDrawerLockMode(enabled: Boolean) {
        Log.d("setDrawerLockMode", "enabled: $enabled")
        val lockMode =
            if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        Log.d("setDrawerLockMode", "setDrawerLockMode: $lockMode")
        binding.drawerLayout.setDrawerLockMode(lockMode)
    }

    fun launchFilePicker() {
        filePickerLauncher.launch(arrayOf("*/*"))
    }
}


@UnstableApi
object MediaCache {
    lateinit var simpleCache: SimpleCache
    lateinit var cacheDataSourceFactory: CacheDataSource.Factory

    // TODO: Make Cache Size User Configurable: 350 MB
    fun initialize(context: Context) {
        if (!::simpleCache.isInitialized) {
            simpleCache = SimpleCache(
                File(context.cacheDir, "exoCache"),
                LeastRecentlyUsedCacheEvictor(350 * 1024 * 1024),
                StandaloneDatabaseProvider(context)
            )
            cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }
    }
}


fun copyToClipboard(context: Context, text: String, msg: String? = null) {
    //Log.d("copyToClipboard", "text: $text")
    var message = msg
    if (msg == null) {
        message = context.getString(R.string.tst_copied_clipboard)
    }
    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Text", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
