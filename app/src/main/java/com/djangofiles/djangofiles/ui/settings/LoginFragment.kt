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
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels


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
        Log.d("Main[onCreate]", "versionName: $versionName")

//        val link: TextView = binding.githubLink
//        link.text = Html.fromHtml(getString(R.string.github_link), Html.FROM_HTML_MODE_LEGACY)
//        link.movementMethod = LinkMovementMethod.getInstance()

        //binding.hostnameText.setText("https://")
        binding.hostnameText.requestFocus()

        val loginFunction = View.OnClickListener {
            Log.d("OnClickListener", "it: ${it.id}")
            //if (it.id == R.id.add_server_login) {
            //    Log.d("OnClickListener", "LOGIN BUTTON")
            //}
            val inputHost = binding.hostnameText.text.toString().trim()
            Log.d("setOnClickListener", "inputHost: $inputHost")
            val host = parseHost(inputHost)
            if (inputHost != host) {
                binding.hostnameText.setText(host)
            }
            Log.d("setOnClickListener", "host: $host")
            if (!isURL(host)) {
                binding.hostnameText.error = "Invalid Hostname"
                return@OnClickListener
            }

            //val sharedPreferences =
            //    requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
            //sharedPreferences?.edit { putString("saved_url", host) }
            //Log.d("getSharedPreferences", "saved_url: $host")
            //findNavController().navigate(R.id.nav_item_home, null, NavOptions.Builder()
            //    .setPopUpTo(R.id.nav_item_login, true)
            //    .build())

            Log.d("showSettingsDialog", "Processing URL: $host")
            val api = ServerApi(requireContext(), host)
            lifecycleScope.launch {
                try {
                    //val versionResponse = api.version(versionName.toString())
                    //Log.d("showSettingsDialog", "versionResponse: $versionResponse")

                    val methodsResponse = api.methods()
                    Log.d("showSettingsDialog", "methodsResponse: $methodsResponse")

                    viewModel.hostname.value = host
                    viewModel.authMethods.value = methodsResponse.authMethods
                    findNavController().navigate(R.id.nav_item_login_action_next)

                } catch (e: Exception) {
                    Log.d("showSettingsDialog", "EXCEPTION")
                    e.printStackTrace()
                    val msg = e.message ?: "Unknown Error Validating Server."
                    Log.i("showSettingsDialog", "msg: $msg")
                    binding.hostnameText.error = "Validation Error"
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
            Log.d("setOnClickListener", "DONE")
        }

        binding.continueBtn.setOnClickListener(loginFunction)
//        binding.addServerReturn.setOnClickListener(loginFunction)
        binding.goBackBtn.setOnClickListener {
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
