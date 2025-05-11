package com.djangofiles.djangofiles.ui.settings

import android.content.Context
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
import androidx.core.view.isVisible
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
import com.djangofiles.djangofiles.db.AlbumDao
import com.djangofiles.djangofiles.db.AlbumDatabase
import com.djangofiles.djangofiles.db.AlbumEntity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    @OptIn(DelicateCoroutinesApi::class)
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

        if (binding.loginLocal.isVisible) {
            binding.loginUsername.requestFocus()
        }

        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)

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
                val api = ServerApi(requireContext(), hostname!!)
                val token = api.login(username, password)
                Log.d("loginFunction", "token: $token")
                if (token == null) {
                    Log.d("loginFunction", "LOGIN FAILED")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Login Failed", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                Log.d("loginFunction", "SUCCESS")
                val dao: ServerDao = ServerDatabase.getInstance(requireContext()).serverDao()
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
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                sharedPreferences.edit {
                    putString("saved_url", hostname)
                    putString("auth_token", token)
                }

                Log.d("loginFunction", "GlobalScope.launch")
                GlobalScope.launch(Dispatchers.IO) {
                    Log.d("loginFunction", "getAlbums: $hostname")
                    getAlbums(requireContext(), hostname)
                }

                Log.d("loginFunction", "MainActivity: setDrawerLockMode(true)")
                (requireActivity() as MainActivity).setDrawerLockMode(true)
                withContext(Dispatchers.Main) {
                    Log.d("loginFunction", "navigate: nav_item_home")
                    findNavController().navigate(
                        R.id.nav_item_home, null, NavOptions.Builder()
                            .setPopUpTo(R.id.nav_item_login, true)
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

suspend fun getAlbums(context: Context, savedUrl: String) {
    val api = ServerApi(context, savedUrl)
    val response = api.albums()
    Log.d("getAlbums", "response: $response")
    if (response.isSuccessful) {
        val dao: AlbumDao = AlbumDatabase.getInstance(context, savedUrl).albumDao()
        val albumResponse = response.body()
        Log.d("getAlbums", "albumResponse: $albumResponse")
        if (albumResponse != null) {
            dao.syncAlbums(albumResponse.albums)
            //for (album in albumResponse.albums) {
            //    Log.d("getAlbums", "album: $album")
            //    val albumEntry = AlbumEntity(
            //        id = album.id,
            //        name = album.name,
            //        password = album.password,
            //        private = album.private,
            //        info = album.info,
            //        expr = album.expr,
            //        date = album.date,
            //        url = album.url,
            //    )
            //    Log.d("getAlbums", "albumEntry: $albumEntry")
            //    dao.addOrUpdate(album = albumEntry)
            //}
            Log.d("getAlbums", "DONE")
        }
    }
}
