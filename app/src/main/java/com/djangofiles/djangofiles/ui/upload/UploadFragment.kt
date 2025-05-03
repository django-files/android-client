package com.djangofiles.djangofiles.ui.upload

import android.annotation.SuppressLint
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
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.copyToClipboard
import com.djangofiles.djangofiles.databinding.FragmentUploadBinding
import com.djangofiles.djangofiles.ui.files.getGenericIcon
import com.djangofiles.djangofiles.ui.files.isCodeMime
import com.djangofiles.djangofiles.ui.files.isGlideMime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private lateinit var navController: NavController

    private lateinit var player: ExoPlayer
    private lateinit var webView: WebView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("UploadFragment", "onCreateView: $savedInstanceState")
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        Log.d("UploadFragment", "onDestroyView")
        super.onDestroyView()
        if (::player.isInitialized) {
            Log.d("UploadFragment", "player.release")
            player.release()
        }
        if (::webView.isInitialized) {
            Log.d("UploadFragment", "webView.destroy")
            webView.destroy()
        }
        _binding = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("Upload[onViewCreated]", "savedInstanceState: $savedInstanceState")
        Log.d("Upload[onViewCreated]", "arguments: $arguments")

        navController = findNavController()

        //val callback = object : OnBackPressedCallback(true) {
        //    override fun handleOnBackPressed() {
        //        requireActivity().finish()
        //    }
        //}
        //requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        //val uri = arguments?.getString("uri")?.toUri()
        val uri = requireArguments().getString("uri")?.toUri()
        Log.d("Upload[onViewCreated]", "uri: $uri")
        val mimeType = arguments?.getString("type")
        Log.d("Upload[onViewCreated]", "mimeType: $mimeType")
        //val text = arguments?.getString("text")
        //Log.d("Upload[onViewCreated]", "text: $text")

        if (uri == null) {
            // TODO: Better Handle this Error
            Log.e("Upload[onViewCreated]", "URI is null")
            Toast.makeText(requireContext(), "No URI to Process!", Toast.LENGTH_LONG).show()
            return
        }

        val fileName = getFileNameFromUri(requireContext(), uri)
        Log.d("Upload[onViewCreated]", "fileName: $fileName")
        binding.fileName.setText(fileName)

        // TODO: Overhaul with Glide and ExoPlayer...
        if (mimeType?.startsWith("video/") == true || mimeType?.startsWith("audio/") == true) {
            Log.d("Upload[onViewCreated]", "EXOPLAYER")
            binding.playerView.visibility = View.VISIBLE

            player = ExoPlayer.Builder(requireContext()).build()
            binding.playerView.player = player
            binding.playerView.controllerShowTimeoutMs = 1000
            binding.playerView.setShowNextButton(false)
            binding.playerView.setShowPreviousButton(false)
            val dataSourceFactory = DefaultDataSource.Factory(requireContext())
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
            player.setMediaSource(mediaSource)
            player.prepare()

        } else if (isGlideMime(mimeType.toString())) {
            Log.d("Upload[onViewCreated]", "GLIDE")
            binding.imageHolder.visibility = View.VISIBLE

            Glide.with(binding.imagePreview).load(uri).into(binding.imagePreview)

        } else if (mimeType?.startsWith("text/") == true || isCodeMime(mimeType!!)) {
            Log.d("Upload[onViewCreated]", "WEBVIEW")
            webView = WebView(requireContext())
            binding.frameLayout.addView(webView)

            val url = "file:///android_asset/preview/preview.html"
            Log.d("Upload[onViewCreated]", "url: $url")

            val content = requireContext().contentResolver.openInputStream(uri)?.bufferedReader()
                ?.use { it.readText() }
            if (content == null) {
                // TODO: Handle null content error...
                Log.w("Upload[onViewCreated]", "content is null")
                return
            }
            //Log.d("Upload[onViewCreated]", "content: $content")
            val escapedContent = JSONObject.quote(content)
            //Log.d("Upload[onViewCreated]", "escapedContent: $escapedContent")
            val jsString = "addContent(${escapedContent});"
            //Log.d("Upload[onViewCreated]", "jsString: $jsString")
            webView.apply {
                settings.javaScriptEnabled = true
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun notifyReady() {
                        webView.post {
                            Log.i("Upload[onViewCreated]", "evaluateJavascript")
                            webView.evaluateJavascript(jsString, null)
                        }
                    }
                }, "Android")
                Log.d("Upload[onViewCreated]", "loadUrl: $url")
                loadUrl(url)
            }

        } else {
            Log.d("Upload[onViewCreated]", "OTHER")
            binding.imageHolder.visibility = View.VISIBLE

            // Set Tint of Icon
            val typedValue = TypedValue()
            val theme = binding.imagePreview.context.theme
            theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            val tint = ContextCompat.getColor(binding.imagePreview.context, typedValue.resourceId)
            val dimmedTint = ColorUtils.setAlphaComponent(tint, (0.5f * 255).toInt())
            binding.imagePreview.setColorFilter(dimmedTint, PorterDuff.Mode.SRC_IN)
            // Set Mime Type Text
            binding.imageOverlayText.text = mimeType
            binding.imageOverlayText.visibility = View.VISIBLE
            // Set Icon Based on Type
            binding.imagePreview.setImageResource(getGenericIcon(mimeType.toString()))
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
                    val uploadResponse = response.body()
                    Log.d("processUpload", "uploadResponse: $uploadResponse")
                    withContext(Dispatchers.Main) {
                        if (uploadResponse != null) {
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

    override fun onStop() {
        Log.d("Upload[onStop]", "1 - ON STOP")
        super.onStop()
        if (::player.isInitialized) {
            Log.d("Upload[onStop]", "player.isPlaying: ${player.isPlaying}")
            if (player.isPlaying) {
                Log.d("Upload[onStop]", "player.pause")
                player.pause()
            }
        }
    }
}

// TODO: This was originally in ZiplineApi but being refactored in DF
fun getFileNameFromUri(context: Context, uri: Uri): String? {
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
