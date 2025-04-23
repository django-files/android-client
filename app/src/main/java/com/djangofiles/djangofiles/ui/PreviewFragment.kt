package com.djangofiles.djangofiles.ui

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.copyToClipboard
import com.djangofiles.djangofiles.databinding.FragmentPreviewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("PreviewFragment", "onCreateView: $savedInstanceState")
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        Log.d("PreviewFragment", "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("Prev[onViewCreated]", "savedInstanceState: $savedInstanceState")
        Log.d("Prev[onViewCreated]", "arguments: $arguments")

        navController = findNavController()

        //val uri = arguments?.getString("uri")?.toUri()
        val uri = requireArguments().getString("uri")?.toUri()
        Log.d("Prev[onViewCreated]", "uri: $uri")
        val type = arguments?.getString("type")
        Log.d("Prev[onViewCreated]", "type: $type")
        //val text = arguments?.getString("text")
        //Log.d("Prev[onViewCreated]", "text: $text")

        if (uri == null) {
            // TODO: Better Handle this Error
            Log.e("Prev[onViewCreated]", "URI is null")
            Toast.makeText(requireContext(), "No URI to Process!", Toast.LENGTH_LONG).show()
            return
        }

        val fileName = getFileNameFromUri(requireContext(), uri)
        Log.d("Prev[onViewCreated]", "fileName: $fileName")
        binding.fileName.setText(fileName)

        if (type?.startsWith("image/") == true) {
            // Show Image Preview
            binding.imagePreview.setImageURI(uri)
        } else {
            // Set Tint of Icon
            val typedValue = TypedValue()
            val theme = binding.imagePreview.context.theme
            theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            val tint = ContextCompat.getColor(binding.imagePreview.context, typedValue.resourceId)
            val dimmedTint = ColorUtils.setAlphaComponent(tint, (0.5f * 255).toInt())
            binding.imagePreview.setColorFilter(dimmedTint, PorterDuff.Mode.SRC_IN)
            // Set Mime Type Text
            binding.imageOverlayText.text = type
            binding.imageOverlayText.visibility = View.VISIBLE
            // Set Icon Based on Type
            // TODO: Create Mapping...
            if (type?.startsWith("text/") == true) {
                binding.imagePreview.setImageResource(R.drawable.fa_file_lines)
            } else if (type?.startsWith("video/") == true) {
                binding.imagePreview.setImageResource(R.drawable.fa_file_video)
            } else if (type?.startsWith("audio/") == true) {
                binding.imagePreview.setImageResource(R.drawable.fa_file_audio)
            } else {
                binding.imagePreview.setImageResource(R.drawable.fa_file_circle_question)
            }
        }

        val sharedPreferences = context?.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences?.getString("saved_url", null)
        Log.d("Prev[onViewCreated]", "savedUrl: $savedUrl")
        val authToken = sharedPreferences?.getString("auth_token", null)
        Log.d("Prev[onViewCreated]", "authToken: $authToken")

        if (savedUrl == null) {
            Log.e("Prev[onViewCreated]", "savedUrl is null")
            Toast.makeText(requireContext(), "Missing URL!", Toast.LENGTH_LONG)
                .show()
            navController.navigate(
                R.id.nav_item_setup, null, NavOptions.Builder()
                    .setPopUpTo(R.id.nav_item_home, true)
                    .build()
            )
            return
        }

        binding.shareButton.setOnClickListener {
            Log.d("shareButton", "setOnClickListener")
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                this.type = type
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
                setDataAndType(uri, type)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(openIntent, null))
        }

        binding.uploadButton.setOnClickListener {
            val fileName = binding.fileName.text.toString().trim()
            Log.d("uploadButton", "fileName: $fileName")
            processUpload(uri, fileName)
        }
    }

    // TODO: DUPLICATION: ShortFragment.processShort
    private fun processUpload(fileUri: Uri, fileName: String?) {
        Log.d("processUpload", "fileUri: $fileUri")
        //val preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("saved_url", null)
        Log.d("processUpload", "savedUrl: $savedUrl")
        val authToken = sharedPreferences.getString("auth_token", null)
        Log.d("processUpload", "authToken: $authToken")
        val fileName = fileName ?: getFileNameFromUri(requireContext(), fileUri)
        Log.d("processUpload", "fileName: $fileName")
        if (savedUrl == null || authToken == null || fileName == null) {
            // TODO: Show settings dialog here...
            Log.w("processUpload", "Missing OR savedUrl/authToken/fileName")
            Toast.makeText(requireContext(), getString(R.string.tst_no_url), Toast.LENGTH_SHORT)
                .show()
            return
        }
        //val contentType = URLConnection.guessContentTypeFromName(fileName)
        //Log.d("processUpload", "contentType: $contentType")
        val inputStream = requireContext().contentResolver.openInputStream(fileUri)
        if (inputStream == null) {
            Log.w("processUpload", "inputStream is null")
            val msg = getString(R.string.tst_upload_error)
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            return
        }
        val api = ServerApi(requireContext(), savedUrl)
        Log.d("processUpload", "api: $api")
        Toast.makeText(requireContext(), getString(R.string.tst_uploading_file), Toast.LENGTH_SHORT)
            .show()
        lifecycleScope.launch {
            try {
                val response = api.upload(fileName, inputStream)
                Log.d("processUpload", "response: $response")
                if (response.isSuccessful) {
                    val fileResponse = response.body()
                    Log.d("processUpload", "fileResponse: $fileResponse")
                    withContext(Dispatchers.Main) {
                        if (fileResponse != null) {
                            copyToClipboard(requireContext(), fileResponse.url)
                            val msg = getString(R.string.tst_copied_clipboard)
                            Log.d("processUpload", "msg: $msg")
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                            navController.navigate(
                                R.id.nav_item_home,
                                bundleOf("url" to fileResponse.url),
                                NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_graph, inclusive = true)
                                    .build()
                            )
                        } else {
                            Log.w("processUpload", "fileResponse is null")
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

    // TODO: This was originally in ZiplineApi but being refactored in DF
    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var fileName: String? = null
        context.contentResolver.query(uri, null, null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName
    }
}
