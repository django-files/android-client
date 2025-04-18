package com.djangofiles.djangofiles

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.util.Patterns
import android.view.Gravity
import android.view.KeyEvent
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.djangofiles.djangofiles.api.ServerApi
import com.djangofiles.djangofiles.databinding.ActivityMainBinding
import com.djangofiles.djangofiles.settings.Server
import com.djangofiles.djangofiles.settings.ServerDao
import com.djangofiles.djangofiles.settings.ServerDatabase
import com.djangofiles.djangofiles.settings.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    companion object {
        const val PREFS_NAME = "AppPreferences"
        const val URL_KEY = "saved_url"
        const val TOKEN_KEY = "auth_token"
    }

    private var userAgent: String = "DjangoFiles Android"
    private var currentUrl: String? = null
    private var versionName: String? = null
    private var clearHistory = false

    private lateinit var binding: ActivityMainBinding

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>>

    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("onCreate", "savedInstanceState: ${savedInstanceState?.size()}")
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.setStatusBarBackgroundColor(Color.TRANSPARENT)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        binding.webView.apply {
            webViewClient = MyWebViewClient()
            webChromeClient = MyWebChromeClient()
            settings.domStorageEnabled = true
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true // not sure if this is needed
            settings.allowContentAccess = true // not sure if this is needed
            settings.mediaPlaybackRequiresUserGesture = true // not sure if this is needed
            //settings.loadWithOverviewMode = true // prevent loading images zoomed in
            //settings.useWideViewPort = true // prevent loading images zoomed in
            addJavascriptInterface(WebAppInterface(this@MainActivity), "Android")
        }

        val packageInfo = packageManager.getPackageInfo(this.packageName, 0)
        versionName = packageInfo.versionName
        Log.d("onCreate", "versionName: $versionName")
        userAgent = "${binding.webView.settings.userAgentString} DjangoFiles Android/$versionName"
        Log.d("onCreate", "UA: $userAgent")
        binding.webView.settings.userAgentString = userAgent

        //ViewCompat.setOnApplyWindowInsetsListener(
        //    binding.main
        //) { v: View, insets: WindowInsetsCompat ->
        //    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        //    Log.d("systemBars", "systemBars: $systemBars")
        //    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        //    insets
        //}

        val headerView = binding.navigationView.getHeaderView(0)
        val versionTextView = headerView.findViewById<TextView>(R.id.header_version)
        versionTextView.text = "v${versionName}"

        filePickerLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                Log.d("filePickerLauncher", "uri: $uri")
                if (uri != null) {
                    val mimeType = contentResolver.getType(uri)
                    Log.d("filePickerLauncher", "mimeType: $mimeType")
                    processSharedFile(uri)
                } else {
                    Log.w("filePickerLauncher", "No File Selected!")
                    Toast.makeText(this, "No File Selected!", Toast.LENGTH_SHORT).show()
                }
            }

        val itemPathMap = mapOf(
            R.id.nav_item_home to "",
            R.id.nav_item_files to "files/",
            R.id.nav_item_gallery to "gallery/",
            R.id.nav_item_albums to "albums/",
            R.id.nav_item_shorts to "shorts/",
            R.id.nav_item_settings_user to "settings/user/",
            R.id.nav_item_settings_site to "settings/site/",
            R.id.nav_item_server_list to "server_list",
        )

        // Handle Navigation Item Clicks
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            Log.d("Drawer", "menuItem: $menuItem")
            Log.d("Drawer", "itemId: ${menuItem.itemId}")

            val path = itemPathMap[menuItem.itemId]
            Log.d("Drawer", "path: $path")

            if (menuItem.itemId == R.id.nav_item_upload) {
                Log.d("Drawer", "nav_item_upload")
                filePickerLauncher.launch(arrayOf("*/*"))

            } else if (menuItem.itemId == R.id.nav_item_server_list) {
                Log.d("Drawer", "nav_item_server_list")
                startActivity(Intent(this, SettingsActivity::class.java))

            } else if (path == null) {
                Log.e("Drawer", "Unknown Menu Item!")
                Toast.makeText(this, "Unknown Menu Item!", Toast.LENGTH_LONG).show()

            } else {
                Log.d("Drawer", "currentUrl: $currentUrl")
                val url = "${currentUrl}/${path}"
                Log.d("Drawer", "Click URL: $url")
                Log.d("Drawer", "webView.url: ${binding.webView.url}")
                if (binding.webView.url != url) {
                    Log.d("Drawer", "binding.webView.loadUrl: $url")
                    binding.webView.loadUrl(url)
                }
            }
            binding.drawerLayout.closeDrawers()
            false
        }

        // Handle Intent
        Log.d("onCreate", "getAction: ${intent.action}")
        Log.d("onCreate", "getData: ${intent.data}")
        Log.d("onCreate", "getExtras: ${intent.extras}")

        handleIntent(intent, savedInstanceState)

        //binding.drawerLayout.openDrawer(GravityCompat.START)
        //startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("onDestroy", "binding.webView.destroy()")
        binding.webView.apply {
            loadUrl("about:blank")
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d("onSaveInstanceState", "outState1: ${outState.size()}")
        binding.webView.saveState(outState)
        Log.d("onSaveInstanceState", "outState2: ${outState.size()}")
    }

    //override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    //    super.onRestoreInstanceState(savedInstanceState)
    //    Log.d("onRestoreInstanceState", "binding.webView.url: ${binding.webView.url}")
    //    Log.d("onRestoreInstanceState", "savedInstanceState: ${savedInstanceState.size()}")
    //}

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")
        binding.webView.onPause()
        binding.webView.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume")
        binding.webView.onResume()
        binding.webView.resumeTimers()
        // TODO: Determine how to better handle this...
        val savedUrl = sharedPreferences.getString(URL_KEY, null)
        Log.d("onResume", "savedUrl: $savedUrl")
        Log.d("onResume", "currentUrl: $currentUrl")
        if (savedUrl.isNullOrEmpty()) {
            Log.d("onResume", "No savedUrl - First Run Detected.")
            //startActivity(Intent(this, SettingsActivity::class.java))
        } else if (savedUrl != currentUrl) {
            Log.d("onResume", "binding.webView.loadUrl: $savedUrl")
            currentUrl = savedUrl
            clearHistory = true
            binding.webView.loadUrl(savedUrl)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && binding.webView.canGoBack()) {
            binding.webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("onNewIntent", "intent: $intent")
        handleIntent(intent, null)
    }

    private fun handleIntent(intent: Intent, savedInstanceState: Bundle?) {
        Log.d("handleIntent", "intent.data (uri): ${intent.data}")
        Log.d("handleIntent", "intent.type: ${intent.type}")
        Log.d("handleIntent", "intent.action: ${intent.action}")

        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
        Log.d("handleIntent", "extraText: $extraText")

        //val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString(URL_KEY, null)
        Log.d("handleIntent", "savedUrl: $savedUrl")
        val authToken = sharedPreferences.getString(TOKEN_KEY, null)
        Log.d("handleIntent", "authToken: $authToken")
        val webViewUrl = binding.webView.url
        Log.d("handleIntent", "webViewUrl: $webViewUrl")

        if (Intent.ACTION_MAIN == intent.action) {
            Log.d("handleIntent", "ACTION_MAIN")

            if (savedUrl.isNullOrEmpty()) {
                Log.d("handleIntent", "No Saved URL!")
                showSettingsDialog()
                //startActivity(Intent(this, SettingsActivity::class.java))
            } else {
                if (webViewUrl == null) {
                    if (savedInstanceState != null) {
                        Log.d("handleIntent", "----- restoreState: ${savedInstanceState.size()}")
                        currentUrl = webViewUrl
                        binding.webView.restoreState(savedInstanceState)
                    } else {
                        Log.d("handleIntent", "+++++ loadUrl: $savedUrl")
                        currentUrl = savedUrl
                        binding.webView.loadUrl(savedUrl)
                    }
                } else {
                    Log.i("handleIntent", "SKIPPING binding.webView.loadUrl")
                }

                val fromShortcut = intent.getStringExtra("fromShortcut")
                Log.d("handleIntent", "fromShortcut: $fromShortcut")
                if (fromShortcut == "upload") {
                    Log.d("handleIntent", "filePickerLauncher.launch")
                    filePickerLauncher.launch(arrayOf("*/*"))
                }
            }

        } else if (Intent.ACTION_VIEW == intent.action) {
            Log.d("handleIntent", "ACTION_VIEW")

            if ("djangofiles" == intent.data?.scheme) {
                Log.d("handleIntent", "scheme: ${intent.data?.scheme}")
                val host = intent.data?.host
                Log.d("handleIntent", "host: $host")
                if ("serverlist" == host) {
                    Log.d("handleIntent", "djangofiles://serverlist")
                    //showSettingsDialog()
                    startActivity(Intent(this, SettingsActivity::class.java))
                } else {
                    Toast.makeText(this, "Unknown DeepLink!", Toast.LENGTH_LONG).show()
                    Log.w("handleIntent", "Unknown DeepLink!")
                    finish()
                }
            } else if (intent.data != null) {
                Log.d("handleIntent", "processSharedFile: ${intent.data}")
                processSharedFile(intent.data!!)
            } else {
                Toast.makeText(this, "Unknown DeepLink!", Toast.LENGTH_LONG).show()
                Log.w("handleIntent", "Unknown DeepLink!")
                finish()
            }

        } else if (Intent.ACTION_SEND == intent.action && intent.type != null) {
            Log.d("handleIntent", "ACTION_SEND")

            val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            Log.d("handleIntent", "fileUri: $fileUri")

            if (!extraText.isNullOrEmpty()) {
                Log.d("handleIntent", "SEND TEXT: $extraText")
                //if (extraText.lowercase().startsWith("http")) {
                if (Patterns.WEB_URL.matcher(extraText).matches()) {
                    Log.d("handleIntent", "URL TEXT DETECTED: $extraText")
                    processShort(extraText)
                } else {
                    Log.d("handleIntent", "NON-URL TEXT DETECTED: $extraText")
                    Toast.makeText(this, "Not Yet Implemented!", Toast.LENGTH_LONG).show()
                }
            } else if (fileUri != null) {
                processSharedFile(fileUri)
            } else {
                Toast.makeText(this, "Unknown Content!", Toast.LENGTH_SHORT).show()
                Log.w("handleIntent", "Unknown Content!")
            }

        } else if (Intent.ACTION_SEND_MULTIPLE == intent.action) {
            Log.d("handleIntent", "ACTION_SEND_MULTIPLE")

            //val fileUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            val fileUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            Log.d("handleIntent", "fileUris: $fileUris")
            if (fileUris != null) {
                for (fileUri in fileUris) {
                    Log.d("handleIntent", "fileUri: $fileUri")
                    processSharedFile(fileUri)
                }
            } else {
                Toast.makeText(this, "Empty Content URI!", Toast.LENGTH_LONG).show()
                Log.w("handleIntent", "URI is NULL")
            }
        } else {
            Toast.makeText(this, "Unknown Intent!", Toast.LENGTH_LONG).show()
            Log.w("handleIntent", "All Intent Types Processed. No Match!")
        }
    }

    // TODO: Replace with Fragment or Activity...
    private fun showSettingsDialog() {
        Log.d("MainActivity", "showSettingsDialog")
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(5, 0, 5, 80)

        val input = EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT
        input.maxLines = 1
        input.hint = getString(R.string.settings_input_place)
        layout.addView(input)
        input.requestFocus()

        val text = TextView(this)
        text.text = getString(R.string.settings_requires)
        text.gravity = Gravity.CENTER_HORIZONTAL
        text.setPadding(0, 20, 0, 0)
        layout.addView(text)

        AlertDialog.Builder(this)
            .setCancelable(false)
            .setTitle("${getString(R.string.app_name)} v$versionName")
            .setMessage(getString(R.string.settings_message))
            .setView(layout)
            .setNegativeButton("Exit") { dialog: DialogInterface?, which: Int -> finish() }
            .setPositiveButton("OK", null)
            .show().apply {
                getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    // TODO: DUPLICATION: SettingsFragment
                    var url = input.text.toString().trim()
                    Log.d("showSettingsDialog", "setPositiveButton URL: $url")
                    url = cleanUrl(url)
                    Log.d("showSettingsDialog", "cleanUrl: $url")
                    if (url.isEmpty()) {
                        Log.d("showSettingsDialog", "URL is Empty")
                        input.error = "This field is required."
                    } else {
                        Log.d("showSettingsDialog", "Processing URL: $url")
                        val api = ServerApi(this@MainActivity, url)
                        lifecycleScope.launch {
                            Log.d("showSettingsDialog", "versionName: $versionName")
                            val response = api.version(versionName!!)
                            Log.d("showSettingsDialog", "response: $response")
                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Log.d("showSettingsDialog", "SUCCESS")
                                    val dao: ServerDao =
                                        ServerDatabase.getInstance(this@MainActivity)
                                            .serverDao()
                                    Log.d("showSettingsDialog", "dao.add Server url = $url")
                                    withContext(Dispatchers.IO) {
                                        dao.add(Server(url = url))
                                    }
                                    sharedPreferences.edit { putString(URL_KEY, url) }
                                    currentUrl = url
                                    Log.d("showSettingsDialog", "binding.webView.loadUrl: $url")
                                    binding.webView.loadUrl(url)
                                    dismiss()
                                    // TODO: I did this by creating a bad version endpoint...
                                    //val versionResponse = response.body()
                                    //Log.d("processShort", "versionResponse: $versionResponse")
                                    //if (versionResponse != null && versionResponse.valid) {
                                    //    Log.d("showSettingsDialog", "SUCCESS")
                                    //    sharedPreferences.edit { putString(URL_KEY, url) }
                                    //    currentUrl = url
                                    //    Log.d("showSettingsDialog", "binding.webView.loadUrl: $url")
                                    //    binding.webView.loadUrl(url)
                                    //    dismiss()
                                    //} else {
                                    //    Log.d("showSettingsDialog", "FAILURE")
                                    //    input.error = "Server Version Too Old"
                                    //}
                                } else {
                                    Log.d("showSettingsDialog", "FAILURE")
                                    input.error = "Invalid URL"
                                }
                            }
                        }
                    }
                }
            }
    }

    // TODO: DUPLICATION: processSharedFile
    private fun processShort(url: String) {
        Log.d("processShort", "url: $url")
        //val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString(URL_KEY, null)
        Log.d("processShort", "savedUrl: $savedUrl")
        val authToken = sharedPreferences.getString(TOKEN_KEY, null)
        Log.d("processShort", "authToken: $authToken")
        if (savedUrl == null || authToken == null) {
            // TODO: Show settings dialog here...
            Log.w("processShort", "Missing OR savedUrl/authToken")
            Toast.makeText(this, getString(R.string.tst_no_url), Toast.LENGTH_SHORT).show()
            return
        }
        val api = ServerApi(this, savedUrl)
        Log.d("processShort", "api: $api")
        Toast.makeText(this, "Creating Short URL...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val response = api.shorten(url)
                Log.d("processShort", "response: $response")
                if (response.isSuccessful) {
                    val shortResponse = response.body()
                    Log.d("processShort", "shortResponse: $shortResponse")
                    withContext(Dispatchers.Main) {
                        Log.d("processShort", "loadUrl: ${savedUrl}/shorts/#shorts-table_wrapper")
                        binding.webView.loadUrl("${savedUrl}/shorts/#shorts-table_wrapper")
                        if (shortResponse != null) {
                            copyToClipboard(shortResponse.url)
                            val msg = getString(R.string.tst_url_copied)
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                            val shareUrl = sharedPreferences.getBoolean("share_after_short", true)
                            Log.d("processShort", "shareUrl: $shareUrl")
                            if (shareUrl) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shortResponse.url)
                                }
                                startActivity(Intent.createChooser(shareIntent, null))
                            }
                        } else {
                            Log.w("processShort", "fileResponse is null")
                            val msg = "Unknown Response!"
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val msg = "Error: ${response.code()}: ${response.message()}"
                    Log.w("processSharedFile", "Error: $msg")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = e.message ?: "Unknown Error!"
                Log.i("processShort", "msg: $msg")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // TODO: DUPLICATION: processShort
    private fun processSharedFile(fileUri: Uri) {
        Log.d("processSharedFile", "fileUri: $fileUri")
        //val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString(URL_KEY, null)
        Log.d("processSharedFile", "savedUrl: $savedUrl")
        val authToken = sharedPreferences.getString(TOKEN_KEY, null)
        Log.d("processSharedFile", "authToken: $authToken")
        val fileName = getFileNameFromUri(fileUri)
        Log.d("processSharedFile", "fileName: $fileName")
        if (savedUrl == null || authToken == null || fileName == null) {
            // TODO: Show settings dialog here...
            Log.w("processSharedFile", "Missing OR savedUrl/authToken/fileName")
            Toast.makeText(this, getString(R.string.tst_no_url), Toast.LENGTH_SHORT).show()
            return
        }
        //val contentType = URLConnection.guessContentTypeFromName(fileName)
        //Log.d("processSharedFile", "contentType: $contentType")
        val inputStream = this@MainActivity.contentResolver.openInputStream(fileUri)
        if (inputStream == null) {
            Log.w("processSharedFile", "inputStream is null")
            Toast.makeText(this, getString(R.string.tst_error_uploading), Toast.LENGTH_SHORT).show()
            return
        }
        val api = ServerApi(this, savedUrl)
        Log.d("processSharedFile", "api: $api")
        Toast.makeText(this, getString(R.string.tst_uploading_file), Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val response = api.upload(fileName, inputStream)
                Log.d("processSharedFile", "response: $response")
                if (response.isSuccessful) {
                    val fileResponse = response.body()
                    Log.d("processSharedFile", "fileResponse: $fileResponse")
                    withContext(Dispatchers.Main) {
                        if (fileResponse != null) {
                            copyToClipboard(fileResponse.url)
                            binding.webView.loadUrl(fileResponse.url)
                            val msg = getString(R.string.tst_url_copied)
                            Log.d("processSharedFile", "msg: $msg")
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            Log.w("processSharedFile", "fileResponse is null")
                            val msg = "Unknown Response!"
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val msg = "Error: ${response.code()}: ${response.message()}"
                    Log.w("processSharedFile", "Error: $msg")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = e.message ?: "Unknown Error!"
                Log.i("processShort", "msg: $msg")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun getFileNameFromUri(uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    private fun copyToClipboard(url: String) {
        Log.d("copyToClipboard", "URL: $url")
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("URL", url)
        clipboard.setPrimaryClip(clip)
    }

    inner class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            Log.d("shouldOverrideUrl", "url: $url")
            val savedUrl = sharedPreferences.getString(URL_KEY, null)
            Log.d("shouldOverrideUrlLoading", "savedUrl: $savedUrl")

            if (
                savedUrl.isNullOrEmpty() ||
                (url.startsWith(savedUrl) &&
                        !url.startsWith("$savedUrl/r/") &&
                        !url.startsWith("$savedUrl/raw/"))
            ) {
                Log.d("shouldOverrideUrlLoading", "APP - App URL")
                return false
            }

            if (
                url.startsWith("https://discord.com/oauth2") ||
                url.startsWith("https://github.com/sessions/two-factor/") ||
                url.startsWith("https://github.com/login") ||
                url.startsWith("https://accounts.google.com/v3/signin") ||
                url.startsWith("https://accounts.google.com/o/oauth2/v2/auth")
            ) {
                Log.d("shouldOverrideUrlLoading", "APP - OAuth URL")
                return false
            }

            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            view.context.startActivity(intent)
            Log.d("shouldOverrideUrlLoading", "BROWSER - Unmatched URL")
            return true
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            Log.d("doUpdateVisitedHistory", "url: $url")
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceError,
        ) {
            Log.d("onReceivedError", "errorCode: ${errorResponse.errorCode}")
            Log.d("onReceivedError", "description: ${errorResponse.description}")
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse,
        ) {
            Log.d("onReceivedHttpError", "statusCode: ${errorResponse.statusCode}")
            Log.d("onReceivedHttpError", "reasonPhrase: ${errorResponse.reasonPhrase}")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            Log.d("onPageFinished", "url: $url")
            if (clearHistory == true) {
                Log.d("onPageFinished", "binding.webView.clearHistory()")
                clearHistory = false
                binding.webView.clearHistory()
            }
        }
    }

    inner class MyWebChromeClient : WebChromeClient() {
        private var filePathCallback: ValueCallback<Array<Uri>>? = null

        private val fileChooserLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val clipData = result.data?.clipData
                val dataUri = result.data?.data
                val uris = when {
                    clipData != null -> Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    dataUri != null -> arrayOf(dataUri)
                    else -> null
                }
                Log.d("fileChooserLauncher", "uris: ${uris?.contentToString()}")
                filePathCallback?.onReceiveValue(uris)
                filePathCallback = null
            }

        override fun onShowFileChooser(
            view: WebView,
            callback: ValueCallback<Array<Uri>>,
            params: FileChooserParams
        ): Boolean {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = callback
            return try {
                Log.d("onShowFileChooser", "fileChooserLauncher.launch")
                fileChooserLauncher.launch(params.createIntent())
                true
            } catch (e: Exception) {
                Log.w("onShowFileChooser", "Exception: $e")
                filePathCallback = null
                false
            }
        }
    }
}


fun cleanUrl(urlString: String): String {
    var url = urlString.trim()
    if (url.isEmpty()) {
        Log.i("cleanUrl", "url.isEmpty()")
        return ""
    }
    if (!url.lowercase().startsWith("http")) {
        url = "https://$url"
    }
    if (url.endsWith("/")) {
        url = url.substring(0, url.length - 1)
    }
    Log.d("cleanUrl", "matching: $url")
    if (!Patterns.WEB_URL.matcher(url).matches()) {
        Log.i("cleanUrl", "Patterns.WEB_URL.matcher Failed")
        return ""
    }
    return url
}
