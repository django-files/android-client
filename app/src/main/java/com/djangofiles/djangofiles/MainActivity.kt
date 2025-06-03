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
import java.io.File
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityMainBinding

    private lateinit var navController: NavController
    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>>

    private val preferences by lazy { getSharedPreferences("AppPreferences", MODE_PRIVATE) }

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
        super.onDestroy()
        Log.d("Main[onDestroy]", "ON DESTROY")
        // TODO: Determine if this is necessary...
        //preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Main[onCreate]", "savedInstanceState: ${savedInstanceState?.size()}")
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Note: This is used over findNavController to use androidx.fragment.app.FragmentContainerView
        navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        NavigationUI.setupWithNavController(binding.navigationView, navController)

        // Set Default Preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        PreferenceManager.setDefaultValues(this, R.xml.preferences_widget, false)

        //Log.d("Main[onCreate]", "registerOnSharedPreferenceChangeListener")
        //preferences.registerOnSharedPreferenceChangeListener(listener)
        //val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        val uniqueID = preferences.getString("unique_id", null)
        Log.d("Main[onCreate]", "uniqueID: $uniqueID")
        if (uniqueID.isNullOrEmpty()) {
            val uuid = UUID.randomUUID().toString()
            Log.i("Main[onCreate]", "SETTING NEW UUID: $uuid")
            preferences.edit {
                putString("unique_id", uuid)
            }
        }

        // TODO: Improve initialization of the WorkRequest
        //  Currently no work is added on first start because this is null
        //  Work will not get added until the 2nd start after a user adds a server
        val workInterval = preferences.getString("work_interval", null)
        Log.i("Main[onCreate]", "workInterval: $workInterval")
        if (!workInterval.isNullOrEmpty() && workInterval != "0") {
            Log.i("Main[onCreate]", "ENSURING SCHEDULED WORK")
            val workRequest =
                PeriodicWorkRequestBuilder<DailyWorker>(workInterval.toLong(), TimeUnit.MINUTES)
                    .setConstraints(DAILY_WORKER_CONSTRAINTS)
                    .build()
            Log.d("Main[onCreate]", "workRequest: $workRequest")
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } else {
            Log.i("Main[onCreate]", "NOT SCHEDULING WORK")
        }

        //lifecycleScope.launch {
        //    val api = DiscordApi(applicationContext)
        //    val response = api.sendMessage("APP STARTUP")
        //    Log.i("Main[onCreate]", "response: $response")
        //}

        // Note: This does not work as expected and has many bugs
        //val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        //navController = navHostFragment.navController
        //val inflater = navController.navInflater
        //val originalGraph = inflater.inflate(R.navigation.nav_graph)
        //Log.d("Main[onCreate]", "intent?.action: ${intent?.action}")
        //val startDest = when (intent?.action) {
        //    Intent.ACTION_VIEW -> R.id.nav_item_upload
        //    Intent.ACTION_SEND -> R.id.nav_item_upload
        //    Intent.ACTION_SEND_MULTIPLE -> R.id.nav_item_upload_multi
        //    else -> R.id.nav_item_home
        //}
        //Log.d("Main[onCreate]", "startDest: $startDest")
        //val navState = NavGraphNavigator(navController.navigatorProvider)
        //val newGraph = NavGraph(navState).apply {
        //    id = originalGraph.id
        //    addAll(originalGraph)
        //    setStartDestination(startDest)
        //}
        //val args = if (startDest != R.id.nav_item_home) {
        //    Bundle().apply {
        //        putParcelable("EXTRA_INTENT", intent)
        //    }
        //} else null
        //Log.d("Main[onCreate]", "args: $args")
        //navController.setGraph(newGraph, args)
        //Log.d("Main[onCreate]", "DONE - navController")

        val packageInfo = packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName

        val headerView = binding.navigationView.getHeaderView(0)
        val versionTextView = headerView.findViewById<TextView>(R.id.header_version)
        versionTextView.text = "v${versionName}"

        binding.drawerLayout.setStatusBarBackgroundColor(Color.TRANSPARENT)

        MediaCache.initialize(this)

        val itemPathMap = mapOf(
            R.id.nav_site_home to "",
            R.id.nav_site_files to "files/",
            R.id.nav_site_gallery to "gallery/",
            R.id.nav_site_albums to "albums/",
            R.id.nav_site_shorts to "shorts/",
            R.id.nav_site_settings_user to "settings/user/",
            R.id.nav_site_settings_site to "settings/site/",
        )

        // Handle Custom Navigation Items
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            val path = itemPathMap[menuItem.itemId]
            if (path != null) {
                Log.d("Drawer", "path: $path")
                val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
                val savedUrl = sharedPreferences.getString("saved_url", null)
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
                    navController.navigate(R.id.nav_item_home)
                }
                binding.drawerLayout.closeDrawers()
                true
            }
            if (menuItem.itemId == R.id.nav_item_upload) {
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

        // TODO: Better handle navigation when preview/login fragments are open
        navController.addOnDestinationChangedListener { _, destination, _ ->
            Log.d("DestinationChanged", "destination: $destination")
            when (destination.id) {
                R.id.nav_item_file_preview -> binding.drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_item_login -> binding.drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

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
                    Toast.makeText(this, "No Files Selected!", Toast.LENGTH_SHORT).show()
                }
            }

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
        val type = intent.type
        val action = intent.action
        Log.d("handleIntent", "data: $data")
        Log.d("handleIntent", "type: $type")
        Log.d("handleIntent", "action: $action")

        val isCalendarUri = data != null &&
                data.authority?.contains("calendar") == true &&
                listOf("/events", "/calendars", "/time").any { data.path?.contains(it) == true }
        Log.d("handleIntent", "isCalendarUri: $isCalendarUri")
        if (isCalendarUri) {
            Log.i("handleIntent", "Calendar Links Not Supported!")
            Toast.makeText(this, "Calendar Links Not Supported!", Toast.LENGTH_LONG).show()
            return
        }

        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
        Log.d("handleIntent", "extraText: $extraText")

        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("saved_url", null)
        Log.d("handleIntent", "savedUrl: $savedUrl")
        //val authToken = sharedPreferences.getString("auth_token", null)
        //Log.d("handleIntent", "authToken: $authToken")

        if (savedUrl.isNullOrEmpty()) {
            Log.i("handleIntent", "LOCK DRAWER: savedUrl.isNullOrEmpty")
            setDrawerLockMode(false)
        }
        Log.i("handleIntent", "drawerLayout.closeDrawers")
        binding.drawerLayout.closeDrawers()

        Log.d("handleIntent", "data?.host: ${data?.host}")
        val noAuthHosts = setOf("oauth", "authorize")
        if (data?.host !in noAuthHosts && savedUrl.isNullOrEmpty()) {
            Log.i("handleIntent", "Missing Saved URL or Token! Showing Login...")

            //setDrawerLockMode(false)
            navController.navigate(
                R.id.nav_item_login, null, NavOptions.Builder()
                    .setPopUpTo(R.id.nav_item_home, true)
                    .build()
            )

        } else if (Intent.ACTION_MAIN == action) {
            Log.d("handleIntent", "ACTION_MAIN")

            //binding.drawerLayout.closeDrawers()

            // TODO: Cleanup the logic for handling MAIN intent...
            //val currentDestinationId = navController.currentDestination?.id
            //Log.d("handleIntent", "currentDestinationId: $currentDestinationId")
            //val launcherAction = sharedPreferences.getString("launcher_action", null)
            //Log.d("handleIntent", "launcherAction: $launcherAction")
            //Log.d("handleIntent", "nav_item_preview: ${R.id.nav_item_preview}")
            //Log.d("handleIntent", "nav_item_short: ${R.id.nav_item_short}")
            //if (currentDestinationId == R.id.nav_item_preview || currentDestinationId == R.id.nav_item_short) {
            //    Log.i("handleIntent", "ON PREVIEW/SHORT - Navigating to HomeFragment w/ setPopUpTo")
            //    // TODO: Determine the correct navigation call here...
            //    //navController.navigate(R.id.nav_item_home)
            //    navController.navigate(
            //        R.id.nav_item_home, null, NavOptions.Builder()
            //            .setPopUpTo(navController.graph.id, true)
            //            .build()
            //    )
            //} else if (currentDestinationId != R.id.nav_item_home && launcherAction != "previous") {
            //    Log.i("handleIntent", "HOME SETTING SET - Navigating to HomeFragment")
            //    navController.navigate(R.id.nav_item_home)
            //}

            val fromShortcut = intent.getStringExtra("fromShortcut")
            Log.d("handleIntent", "fromShortcut: $fromShortcut")
            if (fromShortcut == "upload") {
                Log.d("handleIntent", "filePickerLauncher.launch")
                filePickerLauncher.launch(arrayOf("*/*"))
            }

        } else if ("UPLOAD_FILE" == action) {
            Log.d("handleIntent", "UPLOAD_FILE")

            filePickerLauncher.launch(arrayOf("*/*"))

        } else if ("FILE_LIST" == action) {
            Log.d("handleIntent", "FILE_LIST")

            navController.navigate(R.id.nav_item_files)

        } else if (Intent.ACTION_SEND == action) {
            Log.d("handleIntent", "ACTION_SEND")

            val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            Log.d("handleIntent", "File URI: $fileUri")

            if (fileUri == null && !extraText.isNullOrEmpty()) {
                Log.d("handleIntent", "SEND TEXT DETECTED")
                //if (extraText.lowercase().startsWith("http")) {
                //if (Patterns.WEB_URL.matcher(extraText).matches()) {
                if (isURL(extraText)) {
                    Log.d("handleIntent", "URL DETECTED: $extraText")
                    val bundle = Bundle().apply {
                        putString("url", extraText)
                    }
                    // TODO: Determine how to properly navigate on new intent...
                    //navController.navigate(R.id.nav_item_short, bundle)
                    navController.popBackStack(R.id.nav_graph, true)
                    navController.navigate(
                        R.id.nav_item_short, bundle, NavOptions.Builder()
                            .setPopUpTo(R.id.nav_item_home, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                } else {
                    Log.i("handleIntent", "PLAIN TEXT DETECTED")
                    val bundle = Bundle().apply {
                        putString("text", extraText)
                    }
                    // TODO: Determine how to properly navigate on new intent...
                    navController.popBackStack(R.id.nav_graph, true)
                    navController.navigate(
                        R.id.nav_item_text, bundle, NavOptions.Builder()
                            .setPopUpTo(R.id.nav_item_home, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )
                }
            } else {
                showPreview(fileUri)
            }

        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            Log.d("handleIntent", "ACTION_SEND_MULTIPLE")

            val fileUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            Log.d("handleIntent", "fileUris: $fileUris")
            if (fileUris == null) {
                Toast.makeText(this, "Error Parsing URI!", Toast.LENGTH_LONG).show()
                Log.w("handleIntent", "fileUris is null")
                return
            }
            showMultiPreview(fileUris)

        } else if (Intent.ACTION_VIEW == action) {
            Log.d("handleIntent", "ACTION_VIEW")

            if (data == null) {
                Toast.makeText(this, "That's a Bug!", Toast.LENGTH_LONG).show()
                Log.e("handleIntent", "BUG: UNKNOWN action: $action")
                return
            }
            if ("djangofiles" == data.scheme) {
                Log.d("handleIntent", "scheme: ${data.scheme}")
                Log.d("handleIntent", "host: ${data.host}")
                if ("serverlist" == data.host) {
                    Log.d("handleIntent", "djangofiles://serverlist")
                    navController.navigate(R.id.nav_item_settings)
                } else if ("logout" == data.host) {
                    processLogout()
                } else if ("oauth" == data.host) {
                    processOauth(data)
                } else if ("authorize" == data.host) {
                    Log.w("handleIntent", "AUTHORIZE QR CODE - DO IT MAN!")
                    val url = data.getQueryParameter("url")
                    val signature = data.getQueryParameter("signature")
                    Log.d("handleIntent", "url: $url")
                    Log.d("handleIntent", "signature: $signature")

                    val bundle = Bundle().apply {
                        putString("url", url)
                        putString("signature", signature)
                    }
                    //navController.navigate(R.id.nav_item_authorize, bundle)
                    navController.popBackStack(R.id.nav_graph, true)
                    navController.navigate(
                        R.id.nav_item_authorize, bundle, NavOptions.Builder()
                            .setPopUpTo(R.id.nav_item_home, true)
                            .setLaunchSingleTop(true)
                            .build()
                    )

                } else {
                    Toast.makeText(this, "Unknown DeepLink!", Toast.LENGTH_LONG).show()
                    Log.w("handleIntent", "Unknown DeepLink!")
                }
            } else {
                Log.d("handleIntent", "File URI: $data")
                showPreview(data)
            }
        } else {
            Toast.makeText(this, "That's a Bug!", Toast.LENGTH_LONG).show()
            Log.e("handleIntent", "BUG: UNKNOWN action: $action")
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d("Main[onStop]", "MainActivity - onStop")
        this.updateWidget()
    }

    private fun Context.updateWidget() {
        Log.d("updateWidget", "Context.updateWidget")

        //val appWidgetManager = AppWidgetManager.getInstance(this)
        //val componentName = ComponentName(this, WidgetProvider::class.java)
        //val ids = appWidgetManager.getAppWidgetIds(componentName)
        //appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view)
        //WidgetProvider().onUpdate(this, appWidgetManager, ids)

        // TODO: WidgetUpdate: Consolidate to a function...
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, WidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        WidgetProvider().onUpdate(this, appWidgetManager, ids)
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
        //binding.drawerLayout.closeDrawers()
        val bundle = Bundle().apply { putParcelableArrayList("fileUris", fileUris) }
        navController.popBackStack(R.id.nav_graph, true)
        navController.navigate(
            R.id.nav_item_upload_multi, bundle, NavOptions.Builder()
                .setPopUpTo(R.id.nav_item_home, true)
                .setLaunchSingleTop(true)
                .build()
        )
    }

    private fun showPreview(uri: Uri?) {
        Log.d("Main[showPreview]", "uri: $uri")
        //binding.drawerLayout.closeDrawers()
        val bundle = Bundle().apply { putString("uri", uri.toString()) }
        navController.popBackStack(R.id.nav_graph, true)
        navController.navigate(
            R.id.nav_item_upload, bundle, NavOptions.Builder()
                .setPopUpTo(R.id.nav_item_home, true)
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
        val sharedPreferences = this.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val oauthUrl = sharedPreferences.getString("oauth_host", null)
        Log.d("processOauth", "oauthUrl: $oauthUrl")
        if (oauthUrl == null) {
            // TODO: Handle this error...
            Log.e("processOauth", "oauthUrl is null")
            return
        }

        sharedPreferences.edit {
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

        Log.d("processOauth", "navigate: nav_item_home - setPopUpTo: nav_item_login")
        setDrawerLockMode(true)
        navController.navigate(
            R.id.nav_item_home, null, NavOptions.Builder()
                .setPopUpTo(R.id.nav_item_login, true)
                .build()
        )
    }

    private fun processLogout() {
        val sharedPreferences = this.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("saved_url", null)
        Log.d("processLogout", "savedUrl: $savedUrl")
        sharedPreferences.edit {
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
                Log.i("handleIntent", "LOCK DRAWER: NO MORE SERVERS")
                setDrawerLockMode(false)
                navController.navigate(
                    R.id.nav_item_login, null, NavOptions.Builder()
                        .setPopUpTo(R.id.nav_item_home, true)
                        .build()
                )
            } else {
                Log.d("processLogout", "MORE SERVERS - ACTIVATE ONE")
                //servers.firstOrNull()?.let { dao.activate(it.url) }
                val server = servers.first()
                Log.d("processLogout", "server: $server")
                withContext(Dispatchers.IO) { dao.activate(server.url) }

                sharedPreferences.edit().apply {
                    putString("saved_url", server.url)
                    putString("auth_token", server.token)
                    apply()
                }

                Log.d("processLogout", "navigate: nav_item_login")
                // TODO: Determine proper navigate call here...
                navController.navigate(R.id.nav_item_settings)
                //findNavController().navigate(
                //    R.id.nav_item_settings, null, NavOptions.Builder()
                //        .setPopUpTo(R.id.nav_item_home, true)
                //        .build()
                //)
            }
        }
    }

    fun setDrawerLockMode(enabled: Boolean) {
        // TODO: Update with a ViewModel...
        Log.d("setDrawerLockMode", "enabled: $enabled")
        val lockMode =
            if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        Log.d("setDrawerLockMode", "setDrawerLockMode: $lockMode")
        binding.drawerLayout.setDrawerLockMode(lockMode)
    }
}

@UnstableApi
object MediaCache {
    lateinit var simpleCache: SimpleCache
    lateinit var cacheDataSourceFactory: CacheDataSource.Factory

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

fun isURL(url: String): Boolean {
    return try {
        URL(url)
        Log.d("isURL", "TRUE")
        true
    } catch (_: Exception) {
        Log.d("isURL", "FALSE")
        false
    }
}
