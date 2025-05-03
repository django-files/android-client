package com.djangofiles.djangofiles.ui.upload

import android.content.Context.MODE_PRIVATE
import android.net.Uri
import android.os.Build
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
import androidx.recyclerview.widget.GridLayoutManager
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.databinding.FragmentUploadMultiBinding
import kotlinx.coroutines.launch

class UploadMultiFragment : Fragment() {

    private var _binding: FragmentUploadMultiBinding? = null
    private val binding get() = _binding!!

    private var fileUris: ArrayList<Uri>? = null

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("UploadMultiFragment", "onCreateView: $savedInstanceState")
        _binding = FragmentUploadMultiBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        Log.d("UploadMultiFragment", "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("Multi[onViewCreated]", "savedInstanceState: $savedInstanceState")
        Log.d("Multi[onViewCreated]", "arguments: $arguments")

        navController = findNavController()

        val sharedPreferences = context?.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences?.getString("saved_url", null)
        Log.d("Multi[onViewCreated]", "savedUrl: $savedUrl")
        val authToken = sharedPreferences?.getString("auth_token", null)
        Log.d("Multi[onViewCreated]", "authToken: $authToken")

        if (savedUrl == null) {
            Log.e("Multi[onViewCreated]", "savedUrl is null")
            Toast.makeText(requireContext(), "Missing URL!", Toast.LENGTH_LONG)
                .show()
            navController.navigate(
                R.id.nav_item_login_two, null, NavOptions.Builder()
                    .setPopUpTo(R.id.nav_item_home, true)
                    .build()
            )
            return
        }

        // TODO: Determine how to better pass fileUris argument
        arguments?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                fileUris = it.getParcelableArrayList("fileUris", Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                fileUris = it.getParcelableArrayList("fileUris")
            }
        }
        Log.d("Multi[onViewCreated]", "fileUris: $fileUris")

        if (fileUris == null) {
            // TODO: Better Handle this Error
            Log.e("Multi[onViewCreated]", "URI is null")
            Toast.makeText(requireContext(), "No URI to Process!", Toast.LENGTH_LONG).show()
            return
        }

        // ViewAdapter
        binding.previewRecycler.layoutManager = GridLayoutManager(requireContext(), 2)
        val adapter = UploadMultiAdapter(fileUris!!) { selectedUris ->
            //Log.d("Multi[onItemClick]", "selectedUris: $selectedUris")
            Log.d("Multi[onItemClick]", "selectedUris.size: ${selectedUris.size}")
            binding.uploadButton.text = getString(R.string.upload_multi, selectedUris.size)
        }
        binding.previewRecycler.adapter = adapter

        // Upload Button
        binding.uploadButton.text = getString(R.string.upload_multi, adapter.selectedUris.size)
        binding.uploadButton.setOnClickListener {
            Log.d("uploadButton", "selectedUris: ${adapter.selectedUris}")
            Log.d("uploadButton", "selectedUris.size: ${adapter.selectedUris.size}")
            processUpload(adapter.selectedUris)
        }
        // Options Button
        binding.optionsButton.setOnClickListener {
            Log.d("optionsButton", "setOnClickListener")
            navController.navigate(R.id.nav_item_settings)
        }
    }

    // TODO: DUPLICATION: ShortFragment.processShort
    private fun processUpload(fileUris: MutableSet<Uri>) {
        Log.d("processUpload", "fileUris: $fileUris")
        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("saved_url", null)
        Log.d("processUpload", "savedUrl: $savedUrl")
        val authToken = sharedPreferences.getString("auth_token", null)
        Log.d("processUpload", "authToken: $authToken")

        if (savedUrl == null || authToken == null) {
            // TODO: Show settings dialog here...
            Log.w("processUpload", "Missing OR savedUrl/authToken/fileName")
            Toast.makeText(requireContext(), getString(R.string.tst_no_url), Toast.LENGTH_SHORT)
                .show()
            return
        }
        val msg = "Uploading ${fileUris.size} Files..."
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

        val api = ServerApi(requireContext(), savedUrl)
        Log.d("processUpload", "api: $api")
        val currentContext = requireContext()
        lifecycleScope.launch {
            for (fileUri in fileUris) {
                Log.d("processUpload", "fileUri: $fileUri")
                val fileName = getFileNameFromUri(currentContext, fileUri)
                Log.d("processUpload", "fileName: $fileName")
                try {
                    val inputStream = currentContext.contentResolver.openInputStream(fileUri)
                    if (inputStream == null) {
                        Log.w("processUpload", "inputStream is null")
                        continue
                    }
                    val response = api.upload(fileName!!, inputStream)
                    Log.d("processUpload", "response: $response")
                    if (response.isSuccessful) {
                        val uploadResponse = response.body()
                        Log.d("processUpload", "uploadResponse: $uploadResponse")
                    } else {
                        val msg = "Error: ${response.code()}: ${response.message()}"
                        Log.w("processUpload", "UPLOAD ERROR: $msg")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Toast.makeText(requireContext(), "Upload Complete.", Toast.LENGTH_SHORT).show()
            navController.navigate(
                R.id.nav_item_home,
                bundleOf("url" to "${savedUrl}/files/"),
                NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, inclusive = true)
                    .build()
            )
        }
    }
}
