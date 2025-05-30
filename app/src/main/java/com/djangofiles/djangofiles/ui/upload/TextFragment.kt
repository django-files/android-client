package com.djangofiles.djangofiles.ui.upload

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.ServerApi.FileEditRequest
import com.djangofiles.djangofiles.copyToClipboard
import com.djangofiles.djangofiles.databinding.FragmentTextBinding
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TextFragment : Fragment() {

    private var _binding: FragmentTextBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("TextFragment", "onCreateView: $savedInstanceState")
        _binding = FragmentTextBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        Log.d("TextFragment", "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("Text[onViewCreated]", "savedInstanceState: $savedInstanceState")
        Log.d("Text[onViewCreated]", "arguments: $arguments")

        navController = findNavController()

        val extraText = requireArguments().getString("text")
        Log.d("Text[onViewCreated]", "extraText: $extraText")

        if (extraText == null) {
            // TODO: Better Handle this Error
            Log.e("Text[onViewCreated]", "extraText is null")
            Toast.makeText(requireContext(), "No extraText to Process!", Toast.LENGTH_LONG).show()
            return
        }

        val sharedPreferences = context?.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences?.getString("saved_url", null)
        Log.d("Text[onViewCreated]", "savedUrl: $savedUrl")
        val authToken = sharedPreferences?.getString("auth_token", null)
        Log.d("Text[onViewCreated]", "authToken: $authToken")

        if (savedUrl == null) {
            Log.e("Text[onViewCreated]", "savedUrl is null")
            Toast.makeText(requireContext(), "Missing URL!", Toast.LENGTH_LONG)
                .show()
            navController.navigate(
                R.id.nav_item_login, null, NavOptions.Builder()
                    .setPopUpTo(R.id.nav_item_home, true)
                    .build()
            )
            return
        }

        binding.textContent.setText(extraText)

        binding.shareButton.setOnClickListener {
            Log.d("shareButton", "setOnClickListener")
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, extraText)
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }

        binding.optionsButton.setOnClickListener {
            Log.d("optionsButton", "setOnClickListener")
            navController.navigate(R.id.nav_item_settings)
        }

        binding.uploadButton.setOnClickListener {
            val finalText = binding.textContent.text.toString().trim()
            Log.d("uploadButton", "finalText: $finalText")
            val fileNameInput = binding.vanityName.text.toString().trim()
            Log.d("uploadButton", "fileNameInput: $fileNameInput")
            val fileName = when {
                fileNameInput.isEmpty() -> "paste.txt" // TODO: Add Default Name Option...
                !fileNameInput.contains('.') -> "${fileNameInput}.txt"
                else -> fileNameInput
            }
            Log.d("uploadButton", "fileName: $fileName")
            processUpload(finalText, fileName)
        }
    }

    // TODO: DUPLICATION: UploadFragment.processUpload
    private fun processUpload(
        textContent: String,
        fileName: String,
    ) {
        Log.d("processUpload", "textContent: $textContent")
        Log.d("processUpload", "fileName: $fileName")

        val preferences = requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = preferences.getString("saved_url", null)
        Log.d("processUpload", "savedUrl: $savedUrl")
        val authToken = preferences.getString("auth_token", null)
        Log.d("processUpload", "authToken: $authToken")
        if (savedUrl == null || authToken == null) {
            // TODO: Show settings dialog here...
            Log.w("processUpload", "Missing OR savedUrl/authToken/fileName")
            Toast.makeText(requireContext(), getString(R.string.tst_no_url), Toast.LENGTH_SHORT)
                .show()
            return
        }
        val inputStream = textContent.byteInputStream()
        val api = ServerApi(requireContext(), savedUrl)
        Log.d("processUpload", "api: $api")
        Toast.makeText(requireContext(), getString(R.string.tst_uploading_file), Toast.LENGTH_SHORT)
            .show()
        Firebase.analytics.logEvent("upload_file", null)
        lifecycleScope.launch {
            try {
                // TODO: Implement editRequest
                val response = api.upload(fileName, inputStream, FileEditRequest())
                Log.d("processUpload", "response: $response")
                if (response.isSuccessful) {
                    val uploadResponse = response.body()
                    Log.d("processUpload", "uploadResponse: $uploadResponse")
                    withContext(Dispatchers.Main) {
                        if (uploadResponse != null) {
                            val params = Bundle().apply { putString("text", "true") }
                            Firebase.analytics.logEvent("upload_file", params)
                            copyToClipboard(requireContext(), uploadResponse.url)
                            navController.navigate(
                                R.id.nav_item_home,
                                bundleOf("url" to uploadResponse.url),
                                NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_graph, inclusive = true)
                                    .build()
                            )
                        } else {
                            Log.w("processUpload", "uploadResponse is null")
                            val msg = "Unknown Response!"
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val msg = "Error: ${response.code()}: ${response.message()}"
                    Log.w("processUpload", "Error: $msg")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
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
    }
}
