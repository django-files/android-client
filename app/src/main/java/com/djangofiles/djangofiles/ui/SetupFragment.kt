package com.djangofiles.djangofiles.ui

import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.djangofiles.djangofiles.MainActivity
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.api.ServerApi
import com.djangofiles.djangofiles.databinding.FragmentSetupBinding
import com.djangofiles.djangofiles.isURL
import com.djangofiles.djangofiles.settings.Server
import com.djangofiles.djangofiles.settings.ServerDao
import com.djangofiles.djangofiles.settings.ServerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupFragment : Fragment() {

    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Log.d("SetupFragment", "onCreateView: $savedInstanceState")
        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        Log.d("SetupFragment", "onDestroyView")
        super.onDestroyView()
        _binding = null
        // Unlock Navigation Drawer
        (requireActivity() as MainActivity).setDrawerLockMode(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("onViewCreated", "savedInstanceState: $savedInstanceState")

        // Lock Navigation Drawer
        (requireActivity() as MainActivity).setDrawerLockMode(false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.root.setOnApplyWindowInsetsListener { _, insets ->
                val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
                binding.root.setPadding(0, 0, 0, imeInsets.bottom)
                insets
            }
        }

        val packageInfo =
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        val versionName = packageInfo.versionName
        Log.d("Main[onCreate]", "versionName: $versionName")

        val link: TextView = binding.githubLink
        link.text = Html.fromHtml(getString(R.string.github_link), Html.FROM_HTML_MODE_LEGACY)
        link.movementMethod = LinkMovementMethod.getInstance()

        //binding.loginHostname.setText("https://")
        binding.loginHostname.requestFocus()

        binding.loginButton.setOnClickListener {
            val inputHost = binding.loginHostname.text.toString().trim()
            Log.d("setOnClickListener", "inputHost: $inputHost")
            val host = parseHost(inputHost)
            if (inputHost != host) {
                binding.loginHostname.setText(host)
            }
            Log.d("setOnClickListener", "host: $host")
            //val user = binding.loginUsername.text.toString().trim()
            //Log.d("setOnClickListener", "User: $user")
            //val pass = binding.loginPassword.text.toString().trim()
            //Log.d("setOnClickListener", "Pass: $pass")

            var valid = true
            if (host.isEmpty() || host == "https://") {
                binding.loginHostname.error = "Required"
                valid = false
            }
            if (!isURL(host)) {
                binding.loginHostname.error = "Invalid Hostname"
                valid = false
            }
            //if (user.isEmpty()) {
            //    binding.loginUsername.error = "Required"
            //    valid = false
            //}
            //if (pass.isEmpty()) {
            //    binding.loginPassword.error = "Required"
            //    valid = false
            //}
            if (!valid) return@setOnClickListener

            val sharedPreferences =
                requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
            //sharedPreferences?.edit { putString("saved_url", host) }
            //Log.d("getSharedPreferences", "saved_url: $host")
            //findNavController().navigate(R.id.nav_item_home, null, NavOptions.Builder()
            //    .setPopUpTo(R.id.nav_item_setup, true)
            //    .build())

            Log.d("setOnClickListener", "lifecycleScope.launch")

            Log.d("showSettingsDialog", "Processing URL: $host")
            // TODO: This is copied from showSettingsDialog and needs cleanup...
            val api = ServerApi(requireContext(), host)
            lifecycleScope.launch {
                try {
                    Log.d("showSettingsDialog", "versionName: $versionName")
                    val response = api.version(versionName.toString())
                    Log.d("showSettingsDialog", "response: $response")
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Log.d("showSettingsDialog", "SUCCESS")
                            val dao: ServerDao =
                                ServerDatabase.getInstance(requireContext())
                                    .serverDao()
                            Log.d("showSettingsDialog", "dao.add Server url = $host")
                            withContext(Dispatchers.IO) {
                                dao.add(Server(url = host))
                            }
                            sharedPreferences.edit { putString("saved_url", host) }
                            findNavController().navigate(
                                R.id.nav_item_home, null, NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_item_setup, true)
                                    .build()
                            )
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
                            //input.error = "Invalid URL"
                            binding.loginHostname.error = "Invalid URL"
                            Toast.makeText(context, "Invalid URL!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    val msg = e.message ?: "Unknown Error!"
                    Log.i("processUpload", "msg: $msg")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            }

            //lifecycleScope.launch {
            //    val api = ZiplineApi(requireContext())
            //    val token = api.login(host, user, pass)
            //    Log.d("lifecycleScope.launch", "token: $token")
            //    if (token.isNullOrEmpty()) {
            //        Log.d("lifecycleScope.launch", "LOGIN FAILED")
            //        Toast.makeText(context, "Login Failed!", Toast.LENGTH_SHORT).show()
            //    } else {
            //        Log.d("lifecycleScope.launch", "LOGIN SUCCESS")
            //        val sharedPreferences =
            //            context?.getSharedPreferences("AppPreferences", MODE_PRIVATE)
            //        sharedPreferences?.edit { putString("saved_url", host) }
            //        Log.d("getSharedPreferences", "saved_url: $host")
            //        sharedPreferences?.edit { putString("auth_token", token) }
            //        Log.d("getSharedPreferences", "auth_token: $token")
            //        findNavController().navigate(R.id.nav_item_home, null, NavOptions.Builder()
            //            .setPopUpTo(R.id.nav_item_setup, true)
            //            .build())
            //    }
            //}
            Log.d("setOnClickListener", "DONE")
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
        if (url.length < 9) {
            return "https://"
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }
        //if (!Patterns.WEB_URL.matcher(url).matches()) {
        //    Log.d("parseHost", "Patterns.WEB_URL.matcher Failed")
        //    return ""
        //}
        return url
    }
}
