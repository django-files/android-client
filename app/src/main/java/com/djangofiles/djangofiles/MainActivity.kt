package com.djangofiles.djangofiles

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.webkit.PermissionRequest
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
import com.djangofiles.djangofiles.databinding.ActivityMainBinding
import com.djangofiles.djangofiles.settings.Server
import com.djangofiles.djangofiles.settings.ServerDao
import com.djangofiles.djangofiles.settings.ServerDatabase
import com.djangofiles.djangofiles.settings.SettingsActivity
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection


class MainActivity : AppCompatActivity() {
    companion object {
        const val PREFS_NAME = "AppPreferences"
        const val URL_KEY = "saved_url"
        const val TOKEN_KEY = "auth_token"
    }

    private lateinit var binding: ActivityMainBinding
    private val client = OkHttpClient()

    private var clearHistory = false

    private var userAgent: String = "DjangoFiles Android"
    private var currentUrl: String? = null
    private var versionName: String? = null

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>>

    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("onCreate", "savedInstanceState: ${savedInstanceState?.size()}")
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        binding.webview.apply {
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
        userAgent = "${binding.webview.settings.userAgentString} DjangoFiles Android/$versionName"
        Log.d("onCreate", "UA: $userAgent")
        binding.webview.settings.userAgentString = userAgent

        //ViewCompat.setOnApplyWindowInsetsListener(
        //    binding.main
        //) { v: View, insets: WindowInsetsCompat ->
        //    val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        //    Log.d("systemBars", "systemBars: $systemBars")
        //    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        //    insets
        //}

        val headerView = navigationView.getHeaderView(0)
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
                    Toast.makeText(this, "No File Selected!", Toast.LENGTH_LONG).show()
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
        navigationView.setNavigationItemSelectedListener { menuItem ->
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
                Log.d("Drawer", "webView.url: ${binding.webview.url}")
                if (binding.webview.url != url) {
                    Log.d("Drawer", "binding.webview.loadUrl: $url")
                    binding.webview.loadUrl(url)
                }
            }
            drawerLayout.closeDrawers()
            false
        }

        // Handle Intent
        Log.d("onCreate", "getAction: ${intent.action}")
        Log.d("onCreate", "getData: ${intent.data}")
        Log.d("onCreate", "getExtras: ${intent.extras}")

        handleIntent(intent, savedInstanceState)

        //drawerLayout.openDrawer(GravityCompat.START)
        //startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("onDestroy", "binding.webview.destroy()")
        binding.webview.apply {
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
        binding.webview.saveState(outState)
        Log.d("onSaveInstanceState", "outState2: ${outState.size()}")
    }

    //override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    //    super.onRestoreInstanceState(savedInstanceState)
    //    Log.d("onRestoreInstanceState", "binding.webview.url: ${binding.webview.url}")
    //    Log.d("onRestoreInstanceState", "savedInstanceState: ${savedInstanceState.size()}")
    //}

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")
        binding.webview.onPause()
        binding.webview.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume")
        binding.webview.onResume()
        binding.webview.resumeTimers()
        // TODO: Determine how to better handle this...
        val savedUrl = sharedPreferences.getString(URL_KEY, null)
        Log.d("onResume", "savedUrl: $savedUrl")
        Log.d("onResume", "currentUrl: $currentUrl")
        if (savedUrl.isNullOrEmpty()) {
            Log.d("onResume", "No savedUrl - First Run Detected.")
            //startActivity(Intent(this, SettingsActivity::class.java))
        } else if (savedUrl != currentUrl) {
            Log.d("onResume", "binding.webview.loadUrl: $savedUrl")
            currentUrl = savedUrl
            clearHistory = true
            binding.webview.loadUrl(savedUrl)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && binding.webview.canGoBack()) {
            binding.webview.goBack()
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
        val uri = intent.data
        Log.d("handleIntent", "uri: $uri")

        //String mimeType = getContentResolver().getType(uri);
        val mimeType = intent.type
        Log.d("handleIntent", "mimeType: $mimeType")

        val action = intent.action
        Log.d("handleIntent", "action: $action")

        if (Intent.ACTION_MAIN == action) {
            Log.d("handleIntent", "ACTION_MAIN")
            //val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val savedUrl = sharedPreferences.getString(URL_KEY, null)
            Log.d("handleIntent", "savedUrl: $savedUrl")
            currentUrl = savedUrl
            val authToken = sharedPreferences.getString(TOKEN_KEY, null)
            Log.d("handleIntent", "authToken: $authToken")

            val webViewUrl = binding.webview.url
            Log.d("handleIntent", "webViewUrl: $webViewUrl")

            if (savedUrl.isNullOrEmpty()) {
                showSettingsDialog()
                //startActivity(Intent(this, SettingsActivity::class.java))
            } else {
                if (webViewUrl == null) {
                    Log.d("handleIntent", "binding.webview.url: ${binding.webview.url}")
                    Log.d("handleIntent", "binding.webview.apply")
                    //binding.webview.loadUrl(savedUrl)
                    if (savedInstanceState != null) {
                        Log.d("handleIntent", "----- restoreState: ${savedInstanceState.size()}")
                        binding.webview.restoreState(savedInstanceState)
                    } else {
                        Log.d("handleIntent", "+++++ loadUrl: $savedUrl")
                        binding.webview.loadUrl(savedUrl)
                    }

                } else {
                    Log.d("handleIntent", "SKIPPING  binding.webview.loadUrl")
                }
            }
        } else if (Intent.ACTION_VIEW == action) {
            Log.d("handleIntent", "ACTION_VIEW")
            if (uri != null) {
                val scheme = uri.scheme
                Log.d("handleIntent", "scheme: $scheme")
                val host = uri.host
                Log.d("handleIntent", "host: $host")
                if ("djangofiles" == scheme) {
                    if ("serverlist" == host) {
                        Log.d("handleIntent", "djangofiles://serverlist")
                        //showSettingsDialog()
                        startActivity(Intent(this, SettingsActivity::class.java))
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.tst_error) + ": Unknown DeepLink",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("handleIntent", "Unknown DeepLink!")
                        finish()
                    }
                } else {
                    Log.d("handleIntent", "processSharedFile: $uri")
                    processSharedFile(uri)
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.tst_error) + ": Unknown Intent",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e("handleIntent", "Unknown Intent!")
                finish()
            }
        } else if (Intent.ACTION_SEND == action && mimeType != null) {
            Log.d("handleIntent", "ACTION_SEND")
            if ("text/plain" == mimeType) {
                val sharedText: String? = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (sharedText != null) {
                    Log.d("handleIntent", "Received text/plain: $sharedText")
                    if (sharedText.startsWith("content://")) {
                        val fileUri = sharedText.toUri()
                        Log.d("handleIntent", "Received URI: $fileUri")
                    } else {
                        Log.d("handleIntent", "Received text/plain: $sharedText")
                    }
                }
                //val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                val savedUrl = sharedPreferences.getString(URL_KEY, null)
                Log.d("handleIntent", "binding.webview.loadUrl: ${savedUrl}/paste/")
                binding.webview.loadUrl("${savedUrl}/paste/")
                Toast.makeText(
                    this,
                    this.getString(R.string.tst_not_implemented),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                //val fileUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (fileUri != null) {
                    processSharedFile(fileUri)
                } else {
                    Log.w("handleIntent", "URI is NULL")
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            Log.d("handleIntent", "ACTION_SEND_MULTIPLE")
            //val fileUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            val fileUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            if (fileUris != null) {
                for (fileUri in fileUris) {
                    processSharedFile(fileUri)
                }
            } else {
                Log.w("handleIntent", "URI is NULL")
            }
        } else {
            Toast.makeText(this, "Unknown Intent!", Toast.LENGTH_SHORT).show()
            Log.w("handleIntent", "All Intent Types Processed. No Match!")
        }
    }

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

        runOnUiThread {
            AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle("${getString(R.string.app_name)} v$versionName")
                .setMessage(getString(R.string.settings_message))
                .setView(layout)
                .setNegativeButton("Exit") { dialog: DialogInterface?, which: Int -> finish() }
                .setPositiveButton("OK", null)
                .show().apply {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        var url = input.text.toString().trim { it <= ' ' }
                        Log.d("showSettingsDialog", "setPositiveButton: url: $url")

                        // TODO: Duplicate - SettingsFragment - make this a function
                        if (url.isEmpty()) {
                            Log.d("showSettingsDialog", "URL is Empty")
                            input.error = "This field is required."
                        } else {
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "https://$url"
                            }
                            if (url.endsWith("/")) {
                                url = url.substring(0, url.length - 1)
                            }

                            Log.d("showSettingsDialog", "Processed URL: $url")
                            CoroutineScope(Dispatchers.IO).launch {
                                val response = checkUrl(url)
                                Log.d("showSettingsDialog", "response: $response")
                                withContext(Dispatchers.Main) {
                                    if (response) {
                                        Log.d("showSettingsDialog", "SUCCESS")
                                        sharedPreferences.edit {
                                            putString(
                                                URL_KEY,
                                                url
                                            )
                                        }
                                        currentUrl = url
                                        Log.d("showSettingsDialog", "binding.webview.loadUrl: $url")
                                        binding.webview.loadUrl(url)
                                        dismiss()
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
    }

    // TODO: Duplication - SettingsFragment
    private fun checkUrl(url: String): Boolean {
        Log.d("MainActivity", "checkUrl url: $url")

        val authUrl = "${url}/api/auth/methods/"
        Log.d("MainActivity", "checkUrl authUrl: $authUrl")

        // TODO: Change this to HEAD or use response data...
        val request = Request.Builder().header("User-Agent", "DF").url(authUrl).get().build()
        return try {
            val dao: ServerDao = ServerDatabase.getInstance(this).serverDao()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("MainActivity", "checkUrl Success: Remote OK.")
                dao.add(Server(url = url))
            } else {
                Log.d("MainActivity", "checkUrl Error: Remote code: ${response.code}")
            }
            response.isSuccessful
        } catch (e: Exception) {
            Log.d("MainActivity", "checkUrl Exception: $e")
            false
        }
    }

    private fun processSharedFile(fileUri: Uri) {
        Log.d("processSharedFile", "fileUri: $fileUri")
        //val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString(URL_KEY, null)
        Log.d("processSharedFile", "savedUrl: $savedUrl")
        val authToken = sharedPreferences.getString(TOKEN_KEY, null)
        Log.d("processSharedFile", "authToken: $authToken")
        if (savedUrl == null || authToken == null) {
            // TODO: Show settings dialog here...
            Toast.makeText(this, getString(R.string.tst_no_url), Toast.LENGTH_SHORT).show()
            return
        }

        val file = getInputStreamFromUri(fileUri)
        if (file == null) {
            Toast.makeText(this, "Unable To Process Content!", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = getFileNameFromUri(fileUri)
        Log.d("processSharedFile", "fileName: $fileName")

        val uploadUrl = "$savedUrl/api/upload"
        Log.d("processSharedFile", "uploadUrl: $uploadUrl")
        val contentType = URLConnection.guessContentTypeFromName(fileName)
        Log.d("processSharedFile", "contentType: $contentType")

        Toast.makeText(this, getString(R.string.tst_uploading_file), Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val url = URL(uploadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Authorization", authToken)
                val boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW"
                connection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=$boundary"
                )
                connection.connect()

                val outputStream = DataOutputStream(connection.outputStream)

                // Write the boundary and the necessary headers
                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
                outputStream.writeBytes("Content-Type: $contentType\r\n")
                outputStream.writeBytes("Content-Transfer-Encoding: binary\r\n\r\n")

                // Write the file content
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while ((file.read(buffer).also { bytesRead = it }) != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                file.close()

                // End the multipart request
                outputStream.writeBytes("\r\n--$boundary--\r\n")
                outputStream.flush()
                outputStream.close()

                // Get the response code
                val responseCode = connection.responseCode
                Log.d("processSharedFile", "responseCode: $responseCode")
                val responseMessage = connection.responseMessage
                Log.d("processSharedFile", "responseMessage: $responseMessage")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonURL = parseJsonResponse(connection)
                    runOnUiThread { copyToClipboard(jsonURL!!) }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.tst_error) + ": " + responseMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.tst_error_uploading),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun getInputStreamFromUri(uri: Uri): InputStream? {
        return try {
            contentResolver.openInputStream(uri)
        } catch (e: IOException) {
            Log.d("getInputStreamFromUri", "Error: $e")
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
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

    private fun parseJsonResponse(connection: HttpURLConnection): String? {
        try {
            Log.d("parseJsonResponse", "Begin.")
            val `in` = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var inputLine: String?
            while ((`in`.readLine().also { inputLine = it }) != null) {
                response.append(inputLine)
            }
            `in`.close()

            Log.d("parseJsonResponse", "response: $response")
            val jsonResponse = JSONObject(response.toString())
            Log.d("parseJsonResponse", "JSONObject: $jsonResponse")

            val name = jsonResponse.getString("name")
            val raw = jsonResponse.getString("raw")
            val url = jsonResponse.getString("url")

            Log.d("parseJsonResponse", "Name: $name")
            Log.d("parseJsonResponse", "raw: $raw")
            Log.d("parseJsonResponse", "url: $url")

            return url
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun copyToClipboard(url: String) {
        Log.d("copyToClipboard", "binding.webview.loadUrl: $url")
        binding.webview.loadUrl(url)
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("URL", url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.tst_url_copied), Toast.LENGTH_SHORT).show()
    }

    inner class MyWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            Log.d("shouldOverrideUrlLoading", "url: $url")

            val savedUrl = sharedPreferences.getString(URL_KEY, null)
            Log.d("shouldOverrideUrlLoading", "savedUrl: $savedUrl")

            // Null URL
            if (savedUrl == null) {
                Log.d("shouldOverrideUrlLoading", "APP - null saved url")
                return false
            }

            // Saved URL
            if (
                url.startsWith(savedUrl) &&
                !url.startsWith("$savedUrl/r/") &&
                !url.startsWith("$savedUrl/raw/")
            ) {
                Log.d("shouldOverrideUrlLoading", "APP - saved url match")
                return false
            }

            // OAuth URL
            if (
                url.startsWith("https://discord.com/oauth2") ||
                url.startsWith("https://github.com/sessions/two-factor/") ||
                url.startsWith("https://github.com/login") ||
                url.startsWith("https://accounts.google.com/v3/signin") ||
                url.startsWith("https://accounts.google.com/o/oauth2/v2/auth")
            ) {
                Log.d("shouldOverrideUrlLoading", "APP - oauth url match")
                return false
            }

            // Other URL
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            view.context.startActivity(intent)
            Log.d("shouldOverrideUrlLoading", "BROWSER - unmatched url")
            return true
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
                Log.d("onPageFinished", "binding.webview.clearHistory()")
                clearHistory = false
                binding.webview.clearHistory()
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

        override fun onPermissionRequest(request: PermissionRequest) {
            Log.d("onPermissionRequest", "request: $request")
            //runOnUiThread {
            //    val resources = request.resources
            //    Log.d("onPermissionRequest", "resources: $resources")
            //    request.grant(resources)
            //}
        }
    }
}
