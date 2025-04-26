package com.djangofiles.djangofiles.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.djangofiles.djangofiles.ServerDao
import com.djangofiles.djangofiles.ServerDatabase
import com.djangofiles.djangofiles.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var webViewState: Bundle = Bundle()
    private var currentUrl: String = ""
    //private var clearHistory = false

    val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("Home[onCreateView]", "savedInstanceState: ${savedInstanceState?.size()}")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        Log.d("Home[onDestroyView]", "webView.destroy()")
        binding.webView.apply {
            loadUrl("about:blank")
            stopLoading()
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Home[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")
        Log.d("Home[onViewCreated]", "webViewState: ${webViewState.size()}")
        // TODO: Not sure when this method is triggered...
        if (savedInstanceState != null) {
            Log.i("Home[onViewCreated]", "SETTING webViewState FROM savedInstanceState")
            webViewState =
                savedInstanceState.getBundle("webViewState") ?: Bundle()  // Ensure non-null
            Log.d("Home[onViewCreated]", "webViewState: ${webViewState.size()}")
        }

        val url = arguments?.getString("url")
        Log.d("Home[onViewCreated]", "arguments: url: $url")
        if (url != null) {
            Log.d("Home[onViewCreated]", "arguments.remove: url")
            arguments?.remove("url")
        }

        val versionName = requireContext()
            .packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName
        Log.d("Home[onViewCreated]", "versionName: $versionName")
        val userAgent =
            "${binding.webView.settings.userAgentString} DjangoFiles Android/${versionName}"
        Log.d("Home[onViewCreated]", "UA: $userAgent")

        //Log.d("Home[onViewCreated]", "BEFORE - currentUrl: $currentUrl")
        val sharedPreferences = context?.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        currentUrl = sharedPreferences?.getString("saved_url", "").toString()
        Log.d("Home[onViewCreated]", "Home[onViewCreated] - savedUrl/currentUrl: $currentUrl")

        binding.webView.apply {
            webViewClient = MyWebViewClient()
            webChromeClient = MyWebChromeClient()
            settings.domStorageEnabled = true
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            //settings.loadWithOverviewMode = true // prevent loading images zoomed in
            //settings.useWideViewPort = true // prevent loading images zoomed in
            settings.userAgentString = userAgent
            addJavascriptInterface(WebAppInterface(context), "Android")

            // TODO: Cleanup URL Handling. Consider removing bundle arguments...
            if (url != null) {
                Log.i("Home[webView.apply]", "ARGUMENT - url: $url")
                loadUrl(url)
            } else if (webViewState.size() > 0) {
                Log.i("Home[webView.apply]", "RESTORE STATE")
                restoreState(webViewState)
                Log.d("Home[webView.apply]", "binding.webView.url: ${binding.webView.url}")
                if (binding.webView.url?.startsWith(currentUrl) != true) {
                    Log.i("Home[webView.apply]", "LOAD CHANGED - currentUrl: $currentUrl")
                    loadUrl(currentUrl)
                }
            } else if (currentUrl.isNotBlank()) {
                Log.i("Home[webView.apply]", "LOAD - currentUrl: $currentUrl")
                loadUrl(currentUrl)
            } else {
                Log.i("Home[webView.apply]", "DOING NOTHING - NO currentUrl")
            }
        }

        //viewModel.urlToLoad.observe(viewLifecycleOwner) { url ->
        //    Log.i("Home[viewModel]", "TO THE MOON BABY: $url")
        //    binding.webView.loadUrl(url)
        //}
        viewModel.urlToLoad.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { url ->
                Log.i("Home[viewModel]", "TO THE MOON BABY: $url")
                binding.webView.loadUrl(url)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("Home[onSave]", "outState: ${outState.size()}")
        super.onSaveInstanceState(outState)
        Log.d("Home[onSave]", "webViewState: ${webViewState.size()}")
        _binding?.webView?.saveState(outState)
        outState.putBundle("webViewState", webViewState)
        Log.d("Home[onSave]", "outState: ${outState.size()}")
    }

    override fun onPause() {
        Log.d("Home[onPause]", "ON PAUSE")
        super.onPause()
        Log.d("Home[onPause]", "webView. onPause() / pauseTimers()")
        binding.webView.onPause()
        binding.webView.pauseTimers()

        Log.d("Home[onPause]", "webViewState: ${webViewState.size()}")
        binding.webView.saveState(webViewState)
        Log.d("Home[onPause]", "webViewState: ${webViewState.size()}")
    }

    override fun onResume() {
        Log.d("Home[onResume]", "ON RESUME")
        super.onResume()
        Log.d("Home[onResume]", "webView. onResume() / resumeTimers()")
        binding.webView.onResume()
        binding.webView.resumeTimers()
    }

    inner class MyWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            Log.d("shouldOverrideUrl", "requestUrl: $url")
            Log.d("shouldOverrideUrl", "currentUrl: $currentUrl")

            if (
                currentUrl.isEmpty() ||
                (url.startsWith(currentUrl) &&
                        !url.startsWith("$currentUrl/r/") &&
                        !url.startsWith("$currentUrl/raw/"))
            ) {
                Log.d("shouldOverrideUrl", "APP - App URL")
                return false
            }

//            if (
//                url.startsWith("https://discord.com/oauth2") ||
//                url.startsWith("https://github.com/sessions/two-factor/") ||
//                url.startsWith("https://github.com/login") ||
//                url.startsWith("https://accounts.google.com/v3/signin") ||
//                url.startsWith("https://accounts.google.com/o/oauth2/v2/auth")
//            ) {
//                Log.d("shouldOverrideUrl", "APP - OAuth URL")
//                return false
//            }

            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            view.context.startActivity(intent)
            Log.d("shouldOverrideUrl", "BROWSER - Unmatched URL")
            return true
        }

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            Log.d("doUpdateVisitedHistory", "url: $url")
            //if (url.endsWith("/auth/login") == true) {
            //    Log.d("doUpdateVisitedHistory", "LOGOUT: $url")
            //
            //    val sharedPreferences =
            //        view.context.getSharedPreferences("AppPreferences", MODE_PRIVATE)
            //    Log.d("doUpdateVisitedHistory", "REMOVE: authToken")
            //    //sharedPreferences.edit { putString("auth_token", "") }
            //    sharedPreferences.edit { remove("auth_token") }
            //
            //    Log.d("doUpdateVisitedHistory", "view.loadUrl: about:blank")
            //    view.loadUrl("about:blank")
            //    //view.destroy()
            //    findNavController().navigate(
            //        R.id.nav_item_login, null, NavOptions.Builder()
            //            .setPopUpTo(R.id.nav_item_home, true)
            //            .build()
            //    )
            //}
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            Log.d("onPageFinished", "url: $url")
            viewModel.webViewUrl.value = url
            //if (clearHistory == true) {
            //    Log.i("onPageFinished", "CLEARING - binding.webView.clearHistory()")
            //    clearHistory = false
            //    binding.webView.clearHistory()
            //}
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceError
        ) {
            Log.d("onReceivedError", "ERROR: " + errorResponse.errorCode)
        }

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            Log.d("onReceivedHttpError", "ERROR: " + errorResponse.statusCode)
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

    // TODO: Consider passing sharedPreferences since context is only used for that...
    @Suppress("unused")
    inner class WebAppInterface(private var context: Context) {

        @JavascriptInterface
        fun showToast(toast: String?) {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun receiveAuthToken(authToken: String) {
            Log.d("receiveAuthToken", "Received auth token: $authToken")

            val preferences = context.getSharedPreferences("AppPreferences", MODE_PRIVATE)
            val currentToken = preferences.getString("auth_token", null) ?: ""
            Log.d("receiveAuthToken", "currentToken: $currentToken")
            val currentUrl = preferences.getString("saved_url", null) ?: ""
            Log.d("receiveAuthToken", "currentUrl: $currentUrl")

            if (currentToken != authToken) {
                val dao: ServerDao = ServerDatabase.getInstance(context).serverDao()
                dao.setToken(currentUrl, authToken)
                Log.d("receiveAuthToken", "dao.setToken: $authToken")

                preferences.edit { putString("auth_token", authToken) }
                val cookieManager = CookieManager.getInstance()
                cookieManager.flush()
                Log.d("receiveAuthToken", "Cookies Flushed (saved to disk).")
            } else {
                Log.d("receiveAuthToken", "Auth Token Not Changes.")
            }
        }
    }
}
