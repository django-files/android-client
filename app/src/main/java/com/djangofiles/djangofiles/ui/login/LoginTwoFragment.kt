package com.djangofiles.djangofiles.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.djangofiles.djangofiles.MainActivity
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.databinding.FragmentLoginTwoBinding
import com.djangofiles.djangofiles.db.Server
import com.djangofiles.djangofiles.db.ServerDao
import com.djangofiles.djangofiles.db.ServerDatabase
import com.djangofiles.djangofiles.ui.files.getAlbums
import com.djangofiles.djangofiles.work.updateStats
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginTwoFragment : Fragment() {

    private var _binding: FragmentLoginTwoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by activityViewModels()

    private val navController by lazy { findNavController() }
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

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

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Login[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        val appContext = requireContext()
        val packageInfo =
            appContext.packageManager.getPackageInfo(appContext.packageName, 0)
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

        if (binding.loginLocal.isVisible) {
            binding.loginUsername.requestFocus()
        }

        val loginFunction = View.OnClickListener {
            Log.d("OnClickListener", "it: ${it.id}")

            val username = binding.loginUsername.text.toString().trim()
            Log.d("loginFunction", "username: $username")
            val password = binding.loginPassword.text.toString().trim()
            Log.d("loginFunction", "password: $password")
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
                val api = ServerApi(appContext, hostname!!)
                val token = api.login(username, password)
                Log.d("loginFunction", "token: $token")
                if (token == null) {
                    Log.d("loginFunction", "LOGIN FAILED")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, "Login Failed", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                Log.d("loginFunction", "SUCCESS")
                val dao: ServerDao = ServerDatabase.getInstance(appContext).serverDao()
                Log.d("loginFunction", "dao.add Server url = $hostname")
                try {
                    withContext(Dispatchers.IO) {
                        dao.addOrUpdate(Server(url = hostname, token = token, active = true))
                    }
                } catch (e: Exception) {
                    // TODO: This needs to be handled...
                    val msg = e.message ?: "Unknown Error"
                    Log.e("loginFunction", "Exception: msg: $msg")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                preferences.edit {
                    putString("saved_url", hostname)
                    putString("auth_token", token)
                }

                Log.d("loginFunction", "GlobalScope.launch")
                GlobalScope.launch(Dispatchers.IO) {
                    appContext.getAlbums(hostname)
                    appContext.updateStats()
                }

                Log.i("loginFunction", "UNLOCK DRAWER: MainActivity: setDrawerLockMode(true)")
                (requireActivity() as MainActivity).setDrawerLockMode(true)
                withContext(Dispatchers.Main) {
                    Log.d("loginFunction", "navigate: startDestinationId")
                    navController.navigate(
                        navController.graph.startDestinationId, null, NavOptions.Builder()
                            .setPopUpTo(navController.graph.id, true)
                            .build()
                    )
                }
                Log.d("loginFunction", "DONE")
            }
        }

        val oauthFunction = View.OnClickListener {
            Log.d("OnClickListener", "it: ${it.id}")
            val name = it.tag as? String
            Log.d("OnClickListener", "name: $name")
            val url = authMethods.firstOrNull { it.name == name }?.url
            Log.d("OnClickListener", "url: $url")

            if (url == null) {
                Toast.makeText(appContext, "Error. Report as Bug!", Toast.LENGTH_LONG).show()
                return@OnClickListener
            }

            Log.d("OnClickListener", "oauth_host: $hostname")
            preferences.edit { putString("oauth_host", hostname) }

            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)

        }

        binding.addServerLogin.setOnClickListener(loginFunction)
        binding.loginDiscord.setOnClickListener(oauthFunction)
        binding.loginGithub.setOnClickListener(oauthFunction)
        binding.loginGoogle.setOnClickListener(oauthFunction)
        binding.goBack.setOnClickListener { navController.navigateUp() }
    }
}
