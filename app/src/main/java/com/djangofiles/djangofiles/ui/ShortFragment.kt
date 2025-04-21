package com.djangofiles.djangofiles.ui

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.api.ServerApi
import com.djangofiles.djangofiles.copyToClipboard
import com.djangofiles.djangofiles.databinding.FragmentShortBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShortFragment : Fragment() {

    private var _binding: FragmentShortBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("ShortFragment", "onCreateView: $savedInstanceState")
        _binding = FragmentShortBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        Log.d("ShortFragment", "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("Short[onViewCreated]", "savedInstanceState: $savedInstanceState")
        Log.d("Short[onViewCreated]", "arguments: $arguments")

        navController = findNavController()

        val url = requireArguments().getString("url")
        Log.d("Short[onViewCreated]", "url: $url")

        if (url == null) {
            // TODO: Better Handle this Error
            Log.e("Short[onViewCreated]", "URL is null")
            Toast.makeText(requireContext(), "No URL to Process!", Toast.LENGTH_LONG).show()
            return
        }

        val sharedPreferences = context?.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val ziplineUrl = sharedPreferences?.getString("saved_url", null)
        val ziplineToken = sharedPreferences?.getString("auth_token", null)
        Log.d("Short[onViewCreated]", "ziplineUrl: $ziplineUrl")
        Log.d("Short[onViewCreated]", "ziplineToken: $ziplineToken")

        if (ziplineUrl == null) {
            Log.e("Short[onViewCreated]", "ziplineUrl is null")
            Toast.makeText(requireContext(), "Missing URL!", Toast.LENGTH_LONG)
                .show()
            navController.navigate(
                R.id.nav_item_setup, null, NavOptions.Builder()
                    .setPopUpTo(R.id.nav_item_home, true)
                    .build()
            )
            return
        }

        binding.urlText.setText(url)

        binding.shareButton.setOnClickListener {
            Log.d("shareButton", "setOnClickListener")
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }

        binding.optionsButton.setOnClickListener {
            Log.d("optionsButton", "setOnClickListener")
            navController.navigate(R.id.nav_item_settings)
        }

        binding.openButton.setOnClickListener {
            Log.d("openButton", "setOnClickListener")
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                data = url.toUri()
            }
            startActivity(Intent.createChooser(openIntent, null))
        }

        binding.shortButton.setOnClickListener {
            val longUrl = binding.urlText.text.toString().trim()
            Log.d("uploadButton", "longUrl: $longUrl")
            val vanityName = binding.vanityName.text.toString().trim()
            Log.d("uploadButton", "vanityName: $vanityName")
            processShort(longUrl, vanityName)
        }
    }

    // TODO: DUPLICATION: PreviewFragment.processUpload
    private fun processShort(url: String, vanity: String?) {
        Log.d("processShort", "url: $url")
        Log.d("processShort", "vanity: $vanity")
        //val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("saved_url", null)
        Log.d("processShort", "savedUrl: $savedUrl")
        val authToken = sharedPreferences.getString("auth_token", null)
        Log.d("processShort", "authToken: $authToken")
        if (savedUrl == null || authToken == null) {
            // TODO: Show settings dialog here...
            Log.w("processShort", "Missing OR savedUrl/authToken")
            Toast.makeText(requireContext(), getString(R.string.tst_no_url), Toast.LENGTH_SHORT)
                .show()
            return
        }
        val api = ServerApi(requireContext(), savedUrl)
        Log.d("processShort", "api: $api")
        Toast.makeText(requireContext(), "Creating Short URL...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val response = api.shorten(url, vanity)
                Log.d("processShort", "response: $response")
                if (response.isSuccessful) {
                    val shortResponse = response.body()
                    Log.d("processShort", "shortResponse: $shortResponse")
                    withContext(Dispatchers.Main) {
                        if (shortResponse != null) {
                            copyToClipboard(requireContext(), shortResponse.url)
                            val msg = getString(R.string.tst_url_copied)
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            val shareUrl = sharedPreferences.getBoolean("share_after_short", true)
                            Log.d("processShort", "shareUrl: $shareUrl")
                            if (shareUrl) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shortResponse.url)
                                }
                                startActivity(Intent.createChooser(shareIntent, null))
                            }
                            navController.navigate(
                                R.id.nav_item_home,
                                bundleOf("url" to "${savedUrl}/shorts/#shorts-table_wrapper"),
                                NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_graph, inclusive = true)
                                    .build()
                            )
                        } else {
                            Log.w("processShort", "fileResponse is null")
                            val msg = "Unknown Response!"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val msg = "Error: ${response.code()}: ${response.message()}"
                    Log.w("processShort", "Error: $msg")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = e.message ?: "Unknown Error!"
                Log.i("processShort", "msg: $msg")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

//    private fun processShort(longUrl: String, vanityName: String?) {
//        Log.d("processShort", "URL: $longUrl")
//        Log.d("processShort", "Vanity: $vanityName")
//
//        val sharedPreferences = context?.getSharedPreferences("AppPreferences", MODE_PRIVATE)
//        val ziplineUrl = sharedPreferences?.getString("ziplineUrl", null)
//        val ziplineToken = sharedPreferences?.getString("ziplineToken", null)
//        Log.d("processShort", "ziplineUrl: $ziplineUrl")
//        Log.d("processShort", "ziplineToken: $ziplineToken")
//
//        if (ziplineUrl == null || ziplineToken == null) {
//            Log.e("processShort", "ziplineUrl || ziplineToken is null")
//            Toast.makeText(requireContext(), "Missing Zipline Authentication!", Toast.LENGTH_LONG)
//                .show()
//            navController.navigate(
//                R.id.nav_item_setup, null, NavOptions.Builder()
//                    .setPopUpTo(R.id.nav_item_home, true)
//                    .build()
//            )
//            return
//        }
//
//        val api = ZiplineApi(requireContext())
//        lifecycleScope.launch {
//            val response = api.shorten(longUrl, vanityName, ziplineUrl)
//            Log.d("processShort", "response: $response")
//            if (response != null) {
//                Log.d("processShort", "response.url: ${response.url}")
//                copyToClipboard(response.url)
//                val shareUrl = sharedPreferences.getBoolean("share_after_short", true)
//                Log.d("processShort", "shareUrl: $shareUrl")
//                if (shareUrl) {
//                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
//                        type = "text/plain"
//                        putExtra(Intent.EXTRA_TEXT, response.url)
//                    }
//                    startActivity(Intent.createChooser(shareIntent, null))
//                }
//                navController.navigate(
//                    R.id.nav_item_home,
//                    bundleOf("url" to "${ziplineUrl}/dashboard/urls"),
//                    NavOptions.Builder()
//                        .setPopUpTo(R.id.nav_graph, inclusive = true)
//                        .build()
//                )
//                Log.d("processShort", "DONE")
//            } else {
//                Log.e("processShort", "uploadedFile is null")
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(requireContext(), "File Upload Failed!", Toast.LENGTH_LONG)
//                        .show()
//                }
//            }
//        }
//    }
}
