package com.djangofiles.djangofiles.ui.settings

import android.content.Context
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

        binding.versionName.text = versionName

        val authUrl = arguments?.getString("url")
        Log.d("Authorize[onViewCreated]", "authUrl: $authUrl")
        val signature = arguments?.getString("signature")
        Log.d("Authorize[onViewCreated]", "signature: $signature")

        if (authUrl == null || signature == null) {
            Log.w("Authorize[onViewCreated]", "Missing URL or Signature")
            //ctx.authError("Error Parsing URL or Code.")
            return
        }

        val preferences = ctx.getSharedPreferences("AppPreferences", MODE_PRIVATE)

        val api = ServerApi(ctx, authUrl)

        lifecycleScope.launch {
            val methodsResponse = api.methods()
            Log.d("Authorize[onViewCreated]", "methodsResponse: $methodsResponse")
            if (methodsResponse.isSuccessful) {
                val methodsData = methodsResponse.body()
                Log.d("Authorize[onViewCreated]", "methodsData: $methodsData")
                if (methodsData != null) {
                    Log.d("Authorize[onViewCreated]", "siteName: ${methodsData.siteName}")
                    binding.siteName.text = methodsData.siteName
                    binding.loadingLayout.visibility = View.GONE
                    binding.addServerLayout.visibility = View.VISIBLE
                }
            }
        }

        binding.addServerBtn.setOnClickListener {
            lifecycleScope.launch {
                Log.d("Authorize[addServerBtn]", "api: $api")
                // TODO: All Verification BEFORE this, successful auth adds the cookie...
                val token = api.authorize(signature)
                Log.d("Authorize[addServerBtn]", "token: $token")
                if (token.isNullOrEmpty()) {
                    Log.w("Authorize[addServerBtn]", "AUTH FAILED")
                    ctx.authError("Authentication Failed.")
                    return@launch
                }
                preferences.edit {
                    putString("saved_url", authUrl)
                    putString("auth_token", token)
                }
                val server = Server(url = authUrl, token = token, active = true)
                Log.d("Authorize[addServerBtn]", "server: $server")
                val dao: ServerDao = ServerDatabase.getInstance(ctx).serverDao()
                withContext(Dispatchers.IO) { dao.addOrUpdate(server) }
                Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_LONG).show()
                withContext(Dispatchers.Main) {
                    Log.d("Authorize[addServerBtn]", "navigate: nav_item_home")
                    findNavController().navigate(
                        R.id.nav_item_home, null, NavOptions.Builder()
                            .setPopUpTo(R.id.nav_item_authorize, true)
                            .build()
                    )
                }
            }
        }


        binding.gotoLoginBtn.setOnClickListener {
            Log.d("Authorize[gotoLoginBtn]", "setOnClickListener")
            findNavController().navigate(R.id.nav_item_login)
        }
    }

    fun Context.authError(message: String = "Authentication Error.") {
        binding.addServerBtn.visibility = View.GONE
        binding.authError.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
