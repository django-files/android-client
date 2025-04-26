package com.djangofiles.djangofiles

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.djangofiles.djangofiles.databinding.ActivityMainBinding
import com.djangofiles.djangofiles.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>>

    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Main[onCreate]", "savedInstanceState: ${savedInstanceState?.size()}")
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // NOTE: This is used over findNavController to use androidx.fragment.app.FragmentContainerView
        navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        NavigationUI.setupWithNavController(binding.navigationView, navController)

        val packageInfo = packageManager.getPackageInfo(this.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d("Main[onCreate]", "versionName: $versionName")

        val headerView = binding.navigationView.getHeaderView(0)
        val versionTextView = headerView.findViewById<TextView>(R.id.header_version)
        versionTextView.text = "v${versionName}"

        binding.drawerLayout.setStatusBarBackgroundColor(Color.TRANSPARENT)

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
                if (handled) {
                    binding.drawerLayout.closeDrawers()
                }
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
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                Log.d("filePickerLauncher", "uri: $uri")
                if (uri != null) {
                    val mimeType = contentResolver.getType(uri)
                    Log.d("filePickerLauncher", "mimeType: $mimeType")
                    showPreview(uri, mimeType)
                } else {
                    Log.w("filePickerLauncher", "No File Selected!")
                    Toast.makeText(this, "No File Selected!", Toast.LENGTH_SHORT).show()
                }
            }

        handleIntent(intent, savedInstanceState)
    }

    // TODO: Update with a ViewModel...
    fun setDrawerLockMode(enabled: Boolean) {
        Log.d("setDrawerLockMode", "enabled: $enabled")
        val lockMode =
            if (enabled) DrawerLayout.LOCK_MODE_UNLOCKED else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        Log.d("setDrawerLockMode", "setDrawerLockMode: $lockMode")
        binding.drawerLayout.setDrawerLockMode(lockMode)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("onNewIntent", "intent: $intent")
        handleIntent(intent, null)
    }

    private fun handleIntent(intent: Intent, savedInstanceState: Bundle?) {
        Log.d("handleIntent", "intent: $intent")
        val data = intent.data
        val type = intent.type
        val action = intent.action
        Log.d("handleIntent", "data: $data")
        Log.d("handleIntent", "type: $type")
        Log.d("handleIntent", "action: $action")

        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
        Log.d("handleIntent", "extraText: $extraText")

        val sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("saved_url", null)
        Log.d("handleIntent", "savedUrl: $savedUrl")
        Log.d("handleIntent", "data?.host: ${data?.host}")
        //val authToken = sharedPreferences.getString("auth_token", null)
        //Log.d("handleIntent", "authToken: $authToken")

        Log.d("handleIntent", "data?.host: ${data?.host}")
        if (data?.host != "oauth" && savedUrl.isNullOrEmpty()) {
            Log.i("handleIntent", "Missing Saved URL or Token! Showing Login...")

            setDrawerLockMode(false)
            navController.navigate(
                R.id.nav_item_login, null, NavOptions.Builder()
                    .setPopUpTo(R.id.nav_item_home, true)
                    .build()
            )

        } else if (Intent.ACTION_MAIN == action) {
            Log.d("handleIntent", "ACTION_MAIN: ${savedInstanceState?.size()}")

            binding.drawerLayout.closeDrawers()

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

            // TODO: Determine if this needs to be in the above if/else
            val fromShortcut = intent.getStringExtra("fromShortcut")
            Log.d("handleIntent", "fromShortcut: $fromShortcut")
            if (fromShortcut == "upload") {
                Log.d("handleIntent", "filePickerLauncher.launch")
                filePickerLauncher.launch(arrayOf("*/*"))
            }

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
                Log.d("handleIntent", "SEND TEXT DETECTED: $extraText")
                //if (extraText.lowercase().startsWith("http")) {
                //if (Patterns.WEB_URL.matcher(extraText).matches()) {
                if (isURL(extraText)) {
                    Log.d("handleIntent", "URL DETECTED: $extraText")
                    binding.drawerLayout.closeDrawers()
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
                    Toast.makeText(this, "Not Yet Implemented!", Toast.LENGTH_LONG).show()
                    Log.w("handleIntent", "NOT IMPLEMENTED")
                }
            } else {
                showPreview(fileUri, type)
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
            for (fileUri in fileUris) {
                Log.d("handleIntent", "MULTI: fileUri: $fileUri")
            }
            Toast.makeText(this, "Not Yet Implemented!", Toast.LENGTH_LONG).show()
            Log.w("handleIntent", "NOT IMPLEMENTED")

        } else if (Intent.ACTION_VIEW == action) {
            Log.d("handleIntent", "ACTION_VIEW")
            Log.d("handleIntent", "data: $data")
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
                } else if ("oauth" == data.host) {
                    // TODO: Can do this in a fragment to show loading screen/errors eventually...
                    Log.d("handleIntent", "FUCKING OAUTH: WE MADE IT!")
                    val token = data.getQueryParameter("token")
                    val sessionKey = data.getQueryParameter("session_key")
                    val error = data.getQueryParameter("error")

                    // TODO: Handle null data and errors
                    Log.d("handleIntent", "token: $token")
                    Log.d("handleIntent", "session_key: $sessionKey")
                    Log.d("handleIntent", "error: $error")

                    // TODO: Determine how to better get hostname
                    val hostname = sharedPreferences.getString("oauth_host", null)
                    Log.d("handleIntent", "hostname: $hostname")

                    sharedPreferences.edit {
                        putString("saved_url", hostname)
                        putString("auth_token", token)
                    }
                    lifecycleScope.launch {
                        val dao: ServerDao =
                            ServerDatabase.getInstance(this@MainActivity).serverDao()
                        withContext(Dispatchers.IO) {
                            dao.add(Server(url = hostname!!, token = token!!, active = true))
                        }
                    }

                    val cookieManager = CookieManager.getInstance()
                    //cookieManager.setAcceptThirdPartyCookies(webView, true)
                    val cookie = "sessionid=$sessionKey; Path=/; HttpOnly; Secure"
                    Log.d("handleIntent", "cookie: $cookie")
                    cookieManager.setCookie(hostname, cookie) { cookieManager.flush() }

                    Log.d("handleIntent", "navigate: nav_item_home - setPopUpTo: nav_item_login")
                    setDrawerLockMode(true)
                    navController.navigate(
                        R.id.nav_item_home, null, NavOptions.Builder()
                            .setPopUpTo(R.id.nav_item_login, true)
                            .build()
                    )

                } else {
                    Toast.makeText(this, "Unknown DeepLink!", Toast.LENGTH_LONG).show()
                    Log.w("handleIntent", "Unknown DeepLink!")
                }
            } else {
                Log.d("handleIntent", "File URI: $data")
                showPreview(data, type)
            }
        } else {
            Toast.makeText(this, "That's a Bug!", Toast.LENGTH_LONG).show()
            Log.e("handleIntent", "BUG: UNKNOWN action: $action")
        }
    }

    private fun showPreview(uri: Uri?, type: String?) {
        Log.d("Main[showPreview]", "$type - $uri")
        binding.drawerLayout.closeDrawers()
        val bundle = Bundle().apply {
            putString("uri", uri.toString())
            putString("type", type)
        }
        // TODO: Determine how to properly navigate on new intent...
        //navController.navigate(R.id.nav_item_preview, bundle)

        navController.popBackStack(R.id.nav_graph, true)
        navController.navigate(
            R.id.nav_item_preview, bundle, NavOptions.Builder()
                .setPopUpTo(R.id.nav_item_home, true)
                .setLaunchSingleTop(true)
                .build()
        )

        //val navOptions = NavOptions.Builder()
        //    .setPopUpTo(R.id.nav_item_home, false)
        //    .setLaunchSingleTop(true)
        //    .build()
        //navController.navigate(R.id.nav_item_preview, bundle, navOptions)
    }
}

fun copyToClipboard(context: Context, url: String) {
    Log.d("copyToClipboard", "url: $url")
    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("URL", url)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied URL to Clipboard.", Toast.LENGTH_SHORT).show()
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
