package com.djangofiles.djangofiles.ui.settings

import android.content.Context.MODE_PRIVATE
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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.Server
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
        Log.d("Main[onCreate]", "versionName: $versionName")

        val link: TextView = binding.githubLink
        link.text = Html.fromHtml(getString(R.string.github_link), Html.FROM_HTML_MODE_LEGACY)
        link.movementMethod = LinkMovementMethod.getInstance()

        // TODO: DUPLICATION: SetupFragment

        //binding.loginHostname.setText("https://")
        binding.loginHostname.requestFocus()

        val loginFunction = View.OnClickListener {
            Log.d("OnClickListener", "it: ${it.id}")
            if (it.id == R.id.add_server_login) {
                Log.d("OnClickListener", "LOGIN BUTTON")
            }
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
            //if (host.isEmpty() || host == "https://") {
            //    binding.loginHostname.error = "Required"
            //    valid = false
            //}
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
            if (!valid) return@OnClickListener

            val sharedPreferences =
                requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
            //sharedPreferences?.edit { putString("saved_url", host) }
            //Log.d("getSharedPreferences", "saved_url: $host")
            //findNavController().navigate(R.id.nav_item_home, null, NavOptions.Builder()
            //    .setPopUpTo(R.id.nav_item_setup, true)
            //    .build())

            Log.d("setOnClickListener", "lifecycleScope.launch")

            Log.d("showSettingsDialog", "Processing URL: $host")
            val api = ServerApi(requireContext(), host)
            lifecycleScope.launch {
                try {
                    Log.d("showSettingsDialog", "versionName: $versionName")
                    val response = api.version(versionName.toString())
                    Log.d("showSettingsDialog", "response: $response")
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Log.d("showSettingsDialog", "SUCCESS")
                            // Save Server
                            val dao: ServerDao =
                                ServerDatabase.getInstance(requireContext())
                                    .serverDao()
                            Log.d("showSettingsDialog", "dao.add Server url = $host")
                            withContext(Dispatchers.IO) {
                                dao.add(Server(url = host))
                            }
                            // Activate Server
                            sharedPreferences.edit { putString("saved_url", host) }
                            // Show WebView to Login
                            if (it.id == R.id.add_server_login) {
                                Log.d("showSettingsDialog", "Login - navigate: nav_item_home")
                                findNavController().navigate(
                                    R.id.nav_item_home, null, NavOptions.Builder()
                                        .setPopUpTo(R.id.nav_item_login, true)
                                        .build()
                                )
                            } else {
                                Log.d("showSettingsDialog", "Return - navigateUp")
                                findNavController().navigateUp()
                            }
                        } else {
                            Log.d("showSettingsDialog", "FAILURE")
                            //input.error = "Invalid URL"
                            binding.loginHostname.error = "Validation Error"
                            Toast.makeText(context, "Invalid URL!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.d("showSettingsDialog", "EXCEPTION")
                    e.printStackTrace()
                    val msg = e.message ?: "Unknown Error Validating Server."
                    Log.i("processUpload", "msg: $msg")
                    binding.loginHostname.error = "Validation Error"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
            Log.d("setOnClickListener", "DONE")
        }

        binding.addServerLogin.setOnClickListener(loginFunction)
        binding.addServerReturn.setOnClickListener(loginFunction)
        binding.goBack.setOnClickListener {
            findNavController().navigateUp()
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
