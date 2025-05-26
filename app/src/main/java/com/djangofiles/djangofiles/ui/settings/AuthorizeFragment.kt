package com.djangofiles.djangofiles.ui.settings

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.databinding.FragmentAuthorizeBinding
import com.djangofiles.djangofiles.db.Server
import com.djangofiles.djangofiles.db.ServerDao
import com.djangofiles.djangofiles.db.ServerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthorizeFragment : Fragment() {

    private var _binding: FragmentAuthorizeBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Authorize[onCreate]", "savedInstanceState: ${savedInstanceState?.size()}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("Authorize[onCreateView]", "savedInstanceState: ${savedInstanceState?.size()}")
        _binding = FragmentAuthorizeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Authorize[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")

        val ctx = requireContext()

        val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        val versionName = packageInfo.versionName
        Log.d("Authorize[onViewCreated]", "versionName: $versionName")

        val authUrl = arguments?.getString("url")
        Log.d("Authorize[onViewCreated]", "authUrl: $authUrl")
        val authorization = arguments?.getString("authorization")
        Log.d("Authorize[onViewCreated]", "authorization: $authorization")

        if (authUrl == null || authorization == null) {
            Log.w("Authorize[onViewCreated]", "Missing URL or Authorization")
            return
        }

        val preferences = ctx.getSharedPreferences("AppPreferences", MODE_PRIVATE)

        lifecycleScope.launch {
            val api = ServerApi(ctx, authUrl)
            Log.d("Authorize[onViewCreated]", "api: $api")
            // TODO: All Verification BEFORE this, successful auth adds the cookie...
            val token = api.authorize(authorization)
            Log.d("Authorize[onViewCreated]", "token: $token")
            if (token.isNullOrEmpty()) {
                Log.w("Authorize[onViewCreated]", "AUTH FAILED")
                Toast.makeText(requireContext(), "Authorization Failed!", Toast.LENGTH_LONG).show()
                return@launch
            }

            preferences.edit {
                putString("saved_url", authUrl)
                putString("auth_token", token)
            }

            val server = Server(url = authUrl, token = token, active = true)
            Log.d("reAuthenticate", "server: $server")
            val dao: ServerDao = ServerDatabase.getInstance(ctx).serverDao()
            withContext(Dispatchers.IO) { dao.addOrUpdate(server) }
            Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_LONG).show()
            withContext(Dispatchers.Main) {
                Log.d("loginFunction", "navigate: nav_item_home")
                findNavController().navigate(
                    R.id.nav_item_home, null, NavOptions.Builder()
                        .setPopUpTo(R.id.nav_item_authorize, true)
                        .build()
                )
            }
        }
    }
}
