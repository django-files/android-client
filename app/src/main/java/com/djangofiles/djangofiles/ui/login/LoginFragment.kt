package com.djangofiles.djangofiles.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.databinding.FragmentLoginBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    //private val viewModel: LoginViewModel by viewModels()
    private val viewModel: LoginViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Login[onCreate]", "savedInstanceState: ${savedInstanceState?.size()}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("Login[onCreateView]", "savedInstanceState: ${savedInstanceState?.size()}")
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onStart() {
        super.onStart()
        Log.d("Login[onStart]", "onStart - Hide UI")
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.GONE
    }

    override fun onStop() {
        Log.d("Login[onStop]", "onStop - Show UI")
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).visibility =
            View.VISIBLE
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Login[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        val ctx = requireContext()

        //val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        //val formattedVersion = getString(
        //    R.string.version_code_string,
        //    packageInfo.versionName,
        //    packageInfo.versionCode.toString()
        //)
        //Log.d("showAppInfoDialog", "formattedVersion: $formattedVersion")
        //binding.versionName.text = formattedVersion

        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        binding.versionName.text = packageInfo.versionName

        val authUrl = arguments?.getString("authUrl")
        Log.d("Login[onViewCreated]", "authUrl: $authUrl")

        binding.hostnameText.requestFocus()
        binding.hostnameText.setSelection(binding.hostnameText.text.length)

        val loginFunction = View.OnClickListener {
            Log.d("loginFunction", "it: ${it.id}")
            val inputHost = binding.hostnameText.text.toString().trim()
            Log.d("loginFunction", "inputHost: $inputHost")
            val host = parseHost(inputHost)
            if (inputHost != host) {
                binding.hostnameText.setText(host)
                binding.hostnameText.setSelection(binding.hostnameText.text.length)
            }
            Log.d("loginFunction", "host: $host")
            // TODO: Look into the usage of (host.toHttpUrlOrNull() == null) here.
            //  NOTE: This seems to be less restrictive so should work and can be improved...
            //if (!isURL(host)) {
            if (host.toHttpUrlOrNull() == null) {
                binding.hostnameText.error = "Invalid Hostname"
                return@OnClickListener
            }

            //val sharedPreferences =
            //    requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
            //val savedUrl = sharedPreferences.getString("saved_url", null)
            //Log.d("getSharedPreferences", "savedUrl: $savedUrl")
            //sharedPreferences?.edit { putString("saved_url", host) }

            Log.d("loginFunction", "Processing URL: $host")
            val api = ServerApi(ctx, host)
            lifecycleScope.launch {
                try {
                    // TODO: When a session expires the server will be a duplicate...
                    //val dao: ServerDao = ServerDatabase.getInstance(requireContext()).serverDao()
                    //val server = withContext(Dispatchers.IO) { dao.getByUrl(host) }
                    //Log.d("loginFunction", "server: $server")
                    //Log.d("loginFunction", "authUrl: $authUrl")
                    //if (server != null && authUrl == null) {
                    //    Log.i("loginFunction", "Duplicate Hostname")
                    //    val msg = "Duplicate Hostname"
                    //    withContext(Dispatchers.Main) {
                    //        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    //    }
                    //    binding.hostnameText.error = msg
                    //    return@launch
                    //}

                    // TODO: Implement version request and response, again...
                    //val versionResponse = api.version(versionName.toString())
                    //Log.d("showSettingsDialog", "versionResponse: $versionResponse")

                    // TODO: This was the old code for the version endpoint...
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

                    val methodsResponse = api.methods()
                    Log.d("loginFunction", "methodsResponse: $methodsResponse")
                    val methodsData = methodsResponse.body()
                    if (methodsData == null) {
                        throw Exception("methodsData is null")
                    }
                    viewModel.hostname.value = host
                    viewModel.siteName.value = methodsData.siteName
                    viewModel.authMethods.value = methodsData.authMethods
                    findNavController().navigate(R.id.nav_item_login_action_next)

                } catch (e: Exception) {
                    Log.d("loginFunction", "EXCEPTION")
                    e.printStackTrace()
                    val msg = e.message ?: "Unknown Error Validating Server."
                    Log.i("loginFunction", "msg: $msg")
                    binding.hostnameText.error = "Validation Error"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
            Log.d("loginFunction", "DONE")
        }

        binding.continueBtn.setOnClickListener(loginFunction)
        binding.goBackBtn.setOnClickListener {
            //findNavController().navigateUp()
            if (!findNavController().popBackStack()) {
                requireActivity().finishAffinity()
            }
        }

        binding.websiteLink.paint?.isUnderlineText = true
        binding.websiteLink.setOnClickListener { v ->
            startActivity(Intent(Intent.ACTION_VIEW, v.tag.toString().toUri()))
        }

        if (authUrl != null) {
            // TODO: If the user presses back they can not go forward with the same url...
            Log.d("authUrl", "MANUALLY TRIGGERING CLICK ON authUrl: $authUrl")
            arguments?.remove("authUrl")
            binding.hostnameText.setText(authUrl)
            binding.continueBtn.performClick()
        }
    }

    private fun parseHost(urlString: String): String {
        try {
            var url = urlString.trim()
            if (url.isEmpty()) {
                return ""
            }
            if (!url.lowercase().startsWith("http")) {
                url = "https://$url"
            }
            if (url.toHttpUrlOrNull() == null) {
                return url
            }
            val uri = url.toUri()
            Log.d("parseHost", "uri: $uri")
            Log.d("parseHost", "uri.scheme: ${uri.scheme}")
            if (uri.scheme.isNullOrEmpty()) {
                return "https://"
            }
            Log.d("parseHost", "uri.host: ${uri.host}")
            if (uri.host.isNullOrEmpty()) {
                return "${uri.scheme}://"
            }
            Log.d("parseHost", "uri.path: ${uri.path}")
            val result = "${uri.scheme}://${uri.host}${uri.path}"
            Log.i("parseHost", "result: $result")
            return if (result.endsWith("/")) {
                result.dropLast(1)
            } else {
                result
            }
        } catch (e: Throwable) {
            Log.d("parseHost", "Exception: $e")
            return ""
        }
    }

    //private fun parseHost(urlString: String): String {
    //    var url = urlString.trim()
    //    if (url.isEmpty()) {
    //        return ""
    //    }
    //    if (!url.lowercase().startsWith("http")) {
    //        url = "https://$url"
    //    }
    //    if (url.endsWith("/")) {
    //        url = url.substring(0, url.length - 1)
    //    }
    //    return url
    //}

    //private fun isURL(url: String): Boolean {
    //    return try {
    //        URL(url)
    //        Log.d("isURL", "TRUE")
    //        true
    //    } catch (_: Exception) {
    //        Log.d("isURL", "FALSE")
    //        false
    //    }
    //}
}
