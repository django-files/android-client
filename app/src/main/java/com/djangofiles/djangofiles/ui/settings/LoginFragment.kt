package com.djangofiles.djangofiles.ui.settings

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.ServerDao
import com.djangofiles.djangofiles.ServerDatabase
import com.djangofiles.djangofiles.databinding.FragmentLoginBinding
import com.djangofiles.djangofiles.isURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    //private val viewModel: SettingsViewModel by viewModels()
    private val viewModel: SettingsViewModel by activityViewModels()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Login[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")

        val packageInfo =
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        val versionName = packageInfo.versionName
        Log.d("Login[onViewCreated]", "versionName: $versionName")

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
            if (!isURL(host)) {
                binding.hostnameText.error = "Invalid Hostname"
                return@OnClickListener
            }

            //val sharedPreferences =
            //    requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
            //val savedUrl = sharedPreferences.getString("saved_url", null)
            //Log.d("getSharedPreferences", "savedUrl: $savedUrl")
            //sharedPreferences?.edit { putString("saved_url", host) }

            Log.d("loginFunction", "Processing URL: $host")
            val api = ServerApi(requireContext(), host)
            lifecycleScope.launch {
                try {
                    val dao: ServerDao = ServerDatabase.getInstance(requireContext()).serverDao()
                    val server = withContext(Dispatchers.IO) { dao.getByUrl(host) }
                    Log.d("loginFunction", "server: $server")
                    Log.d("loginFunction", "authUrl: $authUrl")
                    if (server != null && authUrl == null) {
                        Log.i("loginFunction", "Duplicate Hostname")
                        val msg = "Duplicate Hostname"
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        }
                        binding.hostnameText.error = msg
                        return@launch
                    }

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

                    viewModel.hostname.value = host
                    viewModel.siteName.value = methodsResponse.siteName
                    viewModel.authMethods.value = methodsResponse.authMethods
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
        binding.websiteLink.setOnClickListener {
            val url = binding.websiteLink.text.toString()
            Log.d("websiteLink", "url: $url")
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
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
        var url = urlString.trim()
        if (url.isEmpty()) {
            return ""
        }
        if (!url.lowercase().startsWith("http")) {
            url = "https://$url"
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }
        return url
    }
}
