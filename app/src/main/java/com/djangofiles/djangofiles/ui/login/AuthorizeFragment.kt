package com.djangofiles.djangofiles.ui.login

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.djangofiles.djangofiles.MainActivity
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.databinding.FragmentAuthorizeBinding
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

class AuthorizeFragment : Fragment() {

    private var _binding: FragmentAuthorizeBinding? = null
    private val binding get() = _binding!!

    private val navController by lazy { findNavController() }
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

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

    override fun onStart() {
        super.onStart()
        Log.d("Authorize[onStart]", "onStart - Hide UI")
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.GONE
    }

    override fun onStop() {
        Log.d("Authorize[onStop]", "onStop - Show UI")
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).visibility =
            View.VISIBLE
        super.onStop()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("Authorize[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

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
            ctx.authError("Error Parsing URL or Code.")
            return
        }

        val authLink = "<a href=\"${authUrl}\">${authUrl}</a>"
        binding.siteUrl.text = Html.fromHtml(authLink, Html.FROM_HTML_MODE_LEGACY)
        binding.siteUrl.movementMethod = LinkMovementMethod.getInstance()

        val api = ServerApi(ctx, authUrl)

        // TODO: Cache methodsResponse in viewModel
        lifecycleScope.launch {
            val methodsResponse = api.methods()
            Log.d("Authorize[onViewCreated]", "methodsResponse: $methodsResponse")
            if (methodsResponse.isSuccessful) {
                val methodsData = methodsResponse.body()
                Log.d("Authorize[onViewCreated]", "methodsData: $methodsData")
                if (methodsData != null) {
                    Log.d("Authorize[onViewCreated]", "siteName: ${methodsData.siteName}")
                    _binding?.siteName?.text = methodsData.siteName
                    _binding?.loadingLayout?.visibility = View.GONE
                    _binding?.gotoLoginBtn?.visibility = View.VISIBLE
                    _binding?.addServerBtn?.visibility = View.VISIBLE
                }
            }
        }

        binding.addServerBtn.setOnClickListener {
            binding.addServerBtn.visibility = View.GONE
            binding.loadingLayout.visibility = View.VISIBLE
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
                Log.d("loginFunction", "GlobalScope.launch")
                GlobalScope.launch(Dispatchers.IO) {
                    ctx.getAlbums(authUrl)
                    ctx.updateStats()
                }
                Log.i("loginFunction", "UNLOCK DRAWER: MainActivity: setDrawerLockMode(true)")
                (requireActivity() as MainActivity).setDrawerLockMode(true)
                Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_LONG).show()
                withContext(Dispatchers.Main) {
                    Log.d("Authorize[addServerBtn]", "navigate: nav_item_home")
                    navController.navigate(
                        navController.graph.startDestinationId, null, NavOptions.Builder()
                            .setPopUpTo(navController.graph.id, true)
                            .build()
                    )
                }
            }
        }

        binding.gotoLoginBtn.setOnClickListener {
            Log.d("Authorize[gotoLoginBtn]", "setOnClickListener")
            navController.navigate(R.id.nav_item_login)
        }
    }

    fun Context.authError(message: String = "Authentication Error.") {
        binding.loadingLayout.visibility = View.GONE
        binding.addServerBtn.visibility = View.GONE
        binding.errorMessage.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
