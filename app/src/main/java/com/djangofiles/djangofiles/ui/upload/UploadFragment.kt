package com.djangofiles.djangofiles.ui.upload

import android.annotation.SuppressLint
import android.content.Context
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
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.ServerApi.FileEditRequest
import com.djangofiles.djangofiles.copyToClipboard
import com.djangofiles.djangofiles.databinding.FragmentUploadBinding
import com.djangofiles.djangofiles.db.AlbumDatabase
import com.djangofiles.djangofiles.ui.files.AlbumFragment
import com.djangofiles.djangofiles.ui.files.getGenericIcon
import com.djangofiles.djangofiles.ui.files.isCodeMime
import com.djangofiles.djangofiles.ui.files.isGlideMime
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!

    private lateinit var player: ExoPlayer
    private lateinit var webView: WebView

    private val navController by lazy { findNavController() }
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

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
        if (::player.isInitialized) {
            Log.d("UploadFragment", "player.release")
            player.release()
        }
        if (::webView.isInitialized) {
            Log.d("UploadFragment", "webView.destroy")
            webView.destroy()
        }
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        Log.d("Upload[onStart]", "onStart - Hide UI")
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.GONE
    }

    override fun onStop() {
        Log.d("Upload[onStop]", "1 - ON STOP")
        if (::player.isInitialized) {
            Log.d("Upload[onStop]", "player.isPlaying: ${player.isPlaying}")
            if (player.isPlaying) {
                Log.d("Upload[onStop]", "player.pause")
                player.pause()
            }
        }
        Log.d("Upload[onStop]", "onStop - Show UI")
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).visibility =
            View.VISIBLE
        super.onStop()
    }

    @SuppressLint("SetJavaScriptEnabled")
    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("Upload[onViewCreated]", "savedInstanceState: $savedInstanceState")
        Log.d("Upload[onViewCreated]", "arguments: $arguments")

        val savedUrl = preferences.getString("saved_url", null)
        val authToken = preferences.getString("auth_token", null)
        Log.d("Upload[onViewCreated]", "savedUrl: $savedUrl - authToken: $authToken")
        if (savedUrl.isNullOrEmpty() || authToken.isNullOrEmpty()) {
            Log.w("Upload[onViewCreated]", "Missing Saved URL or Auth Token!")
            Toast.makeText(requireContext(), "Invalid Authentication!", Toast.LENGTH_LONG)
                .show()
            navController.navigate(
                R.id.nav_item_login, null, NavOptions.Builder()
                    .setPopUpTo(navController.graph.id, true)
                    .build()
            )
            return
        }

        val uri = requireArguments().getString("uri")?.toUri()
        Log.d("Upload[onViewCreated]", "uri: $uri")
        if (uri == null) {
            // TODO: Better Handle this Error
            Log.e("Upload[onViewCreated]", "URI is null")
            Toast.makeText(requireContext(), "No URI to Process!", Toast.LENGTH_LONG).show()
            return
        }

        val mimeType = requireContext().contentResolver.getType(uri)
        Log.d("Upload[onViewCreated]", "mimeType: $mimeType")

        val fileName = requireContext().getFileNameFromUri(uri)
        Log.d("Upload[onViewCreated]", "fileName: $fileName")
        binding.fileName.setText(fileName)

        // Upload Options - TODO: Set default options here...
        //val fileAlbums = mutableListOf<Int>()
        val editRequest = FileEditRequest()

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
            binding.contentLayout.addView(webView)

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
                this.type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, null))
        }

        binding.optionsButton.setOnClickListener {
            Log.d("optionsButton", "setOnClickListener")
            navController.navigate(R.id.nav_item_settings, bundleOf("hide_bottom_nav" to true))
        }

        binding.openButton.setOnClickListener {
            Log.d("openButton", "setOnClickListener")
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(openIntent, null))
        }

        binding.uploadButton.setOnClickListener {
            val fileName = binding.fileName.text.toString().trim()
            Log.d("uploadButton", "fileName: $fileName")
            processUpload(uri, fileName, editRequest)
        }

        // TODO: Duplicate from UploadMultiFragment
        binding.albumButton.setOnClickListener {
            Log.d("File[albumButton]", "Album Button")
            Log.d("File[albumButton]", "editRequest: $editRequest")

            val savedUrl = preferences.getString("saved_url", null).toString()
            Log.d("File[albumButton]", "savedUrl: $savedUrl")

            val dao = AlbumDatabase.getInstance(requireContext(), savedUrl).albumDao()
            lifecycleScope.launch {
                setFragmentResultListener("albums_result") { _, bundle ->
                    val albums = bundle.getIntegerArrayList("albums")
                    Log.d("File[albumButton]", "albums: $albums")
                    if (albums != null) {
                        editRequest.albums = albums.toList()
                    }
                }

                val albums = withContext(Dispatchers.IO) { dao.getAll() }
                Log.d("File[albumButton]", "albums: $albums")
                val albumFragment = AlbumFragment()
                albumFragment.setAlbumData(albums, listOf(), editRequest.albums)
                albumFragment.show(parentFragmentManager, "AlbumFragment")
            }
        }
    }

    // TODO: DUPLICATION: ShortFragment.processShort
    private fun processUpload(
        fileUri: Uri,
        fileName: String?,
        editRequest: FileEditRequest? = null,
    ) {
        Log.d("processUpload", "fileUri: $fileUri")
        val savedUrl = preferences.getString("saved_url", null)
        val authToken = preferences.getString("auth_token", null)
        Log.d("processUpload", "savedUrl: $savedUrl - authToken: $authToken")
        val fileName = fileName ?: requireContext().getFileNameFromUri(fileUri)
        Log.d("processUpload", "fileName: $fileName")
        val shareUrl = preferences.getBoolean("share_after_upload", true)
        Log.d("processUpload", "shareUrl: $shareUrl")
        if (savedUrl == null || authToken == null || fileName == null) {
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
        Firebase.analytics.logEvent("upload_file", null)
        lifecycleScope.launch {
            try {
                val response = api.upload(fileName, inputStream, editRequest ?: FileEditRequest())
                Log.d("processUpload", "response: $response")
                if (response.isSuccessful) {
                    val uploadResponse = response.body()
                    Log.d("processUpload", "uploadResponse: $uploadResponse")
                    withContext(Dispatchers.Main) {
                        if (uploadResponse != null) {
                            Log.d("processUpload", "url: ${uploadResponse.url}")
                            copyToClipboard(requireContext(), uploadResponse.url)
                            if (shareUrl) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, uploadResponse.url)
                                }
                                startActivity(Intent.createChooser(shareIntent, null))
                            }
                            val bundle = bundleOf("url" to uploadResponse.url)
                            navController.navigate(
                                R.id.nav_item_home, bundle, NavOptions.Builder()
                                    .setPopUpTo(navController.graph.id, true)
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

fun Context.getFileNameFromUri(uri: Uri): String? {
    var fileName: String? = null
    this.contentResolver.query(uri, null, null, null, null).use { cursor ->
        if (cursor != null && cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }
    return fileName
}
