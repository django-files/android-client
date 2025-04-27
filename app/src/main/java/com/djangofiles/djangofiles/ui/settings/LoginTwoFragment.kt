package com.djangofiles.djangofiles.ui.settings

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.djangofiles.djangofiles.MainActivity
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.Server
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.ServerDao
import com.djangofiles.djangofiles.ServerDatabase
import com.djangofiles.djangofiles.databinding.FragmentLoginTwoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.view.isVisible

class LoginTwoFragment : Fragment() {

    private var _binding: FragmentLoginTwoBinding? = null
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
        _binding = FragmentLoginTwoBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Login[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")

        val packageInfo =
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
        val versionName = packageInfo.versionName
        Log.d("Login[onCreate]", "versionName: $versionName")

        val hostname = viewModel.hostname.value
        val authMethods = viewModel.authMethods.value
        Log.d("Login[onCreate]", "hostname: $hostname")
        Log.d("Login[onCreate]", "authMethods: $authMethods")
        Log.d("Login[onCreate]", "viewModel.siteName.value: ${viewModel.siteName.value}")

        if (!viewModel.siteName.value.isNullOrEmpty()) {
            binding.siteName.text = viewModel.siteName.value
        }

        for (method in authMethods!!) {
            Log.d("Login[onCreate]", "method.name: ${method.name}")
            Log.d("Login[onCreate]", "method.url: ${method.url}")
            when (method.name) {
                "local" -> binding.loginLocal.visibility = View.VISIBLE
                "discord" -> binding.loginDiscord.visibility = View.VISIBLE
                "github" -> binding.loginGithub.visibility = View.VISIBLE
                "google" -> binding.loginGoogle.visibility = View.VISIBLE
            }
        }

        if (binding.loginLocal.isVisible){
            binding.loginUsername.requestFocus()
        }

        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)

        val loginFunction = View.OnClickListener {
            Log.d("OnClickListener", "it: ${it.id}")

            val username = binding.loginUsername.text.toString().trim()
            Log.d("setOnClickListener", "username: $username")
            val password = binding.loginPassword.text.toString().trim()
            Log.d("setOnClickListener", "password: $password")
            var valid = true
            if (username.isEmpty()) {
                binding.loginUsername.error = "Required"
                valid = false
            }
            if (password.isEmpty()) {
                binding.loginPassword.error = "Required"
                valid = false
            }
            if (!valid) return@OnClickListener

            lifecycleScope.launch {
                val api = ServerApi(requireContext(), hostname!!)
                val token = api.login(username, password)
                Log.d("OMG", "token: $token")
                if (token == null) {
                    Log.d("OMG", "LOGIN FAILED")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Login Failed", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                Log.d("OMG", "SUCCESS")
                val dao: ServerDao = ServerDatabase.getInstance(requireContext()).serverDao()
                Log.d("OMG", "dao.add Server url = $hostname")
                try {
                    withContext(Dispatchers.IO) {
                        dao.add(Server(url = hostname, token = token, active = true))
                    }
                } catch (e: Exception) {
                    val msg = e.message ?: "Unknown Error"
                    Log.e("OMG", "Exception: msg: $msg")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                sharedPreferences.edit {
                    putString("saved_url", hostname)
                    putString("auth_token", token)
                }
                Log.d("OMG", "MainActivity: setDrawerLockMode(true)")
                (requireActivity() as MainActivity).setDrawerLockMode(true)
                withContext(Dispatchers.Main) {
                    Log.d("OMG", "navigate: nav_item_home")
                    findNavController().navigate(
                        R.id.nav_item_home, null, NavOptions.Builder()
                            .setPopUpTo(R.id.nav_item_login, true)
                            .build()
                    )
                }
                Log.d("OMG", "DONE")
            }
        }

        val oauthFunction = View.OnClickListener {
            Log.d("OnClickListener", "it: ${it.id}")
            val name = it.tag as? String
            Log.d("OnClickListener", "name: $name")
            val url = authMethods.firstOrNull { it.name == name }?.url
            Log.d("OnClickListener", "url: $url")

            if (url == null) {
                Toast.makeText(requireContext(), "Error. Report as Bug!", Toast.LENGTH_LONG).show()
                return@OnClickListener
            }

            Log.d("OnClickListener", "oauth_host: $hostname")
            sharedPreferences.edit {
                putString("oauth_host", hostname)
            }

            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)

        }

        binding.addServerLogin.setOnClickListener(loginFunction)
        binding.loginDiscord.setOnClickListener(oauthFunction)
        binding.loginGithub.setOnClickListener(oauthFunction)
        binding.loginGoogle.setOnClickListener(oauthFunction)
        binding.goBack.setOnClickListener {
            findNavController().navigateUp()
            //if (!findNavController().popBackStack()) {
            //    requireActivity().finishAffinity()
            //}
        }
    }
}
