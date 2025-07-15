package com.djangofiles.djangofiles.ui.upload

import android.content.res.Configuration
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.ServerApi.FileEditRequest
import com.djangofiles.djangofiles.databinding.FragmentUploadMultiBinding
import com.djangofiles.djangofiles.db.AlbumDatabase
import com.djangofiles.djangofiles.ui.files.AlbumFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadMultiFragment : Fragment() {

    private var _binding: FragmentUploadMultiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UploadViewModel by activityViewModels()

    private lateinit var adapter: UploadMultiAdapter

    private val navController by lazy { findNavController() }
    private val preferences by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("Multi[onCreateView]", "savedInstanceState: ${savedInstanceState?.size()}")
        _binding = FragmentUploadMultiBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        Log.d("UploadMultiFragment", "onDestroyView")
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        Log.d("Multi[onStart]", "onStart - Hide UI")
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).visibility = View.GONE
    }

    override fun onStop() {
        Log.d("Multi[onStop]", "onStop - Show UI")
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav).visibility =
            View.VISIBLE
        super.onStop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("Multi[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")
        Log.d("Multi[onViewCreated]", "arguments: $arguments")

        val savedUrl = preferences.getString("saved_url", null)
        val authToken = preferences.getString("auth_token", null)
        Log.d("Multi[onViewCreated]", "savedUrl: $savedUrl - authToken: $authToken")
        if (savedUrl.isNullOrEmpty() || authToken.isNullOrEmpty()) {
            Log.w("Multi[onViewCreated]", "Missing Saved URL or Auth Token!")
            Toast.makeText(requireContext(), "Invalid Authentication!", Toast.LENGTH_LONG)
                .show()
            navController.navigate(
                R.id.nav_item_login, null, NavOptions.Builder()
                    .setPopUpTo(navController.graph.id, true)
                    .build()
            )
            return
        }

        val fileUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelableArrayList("fileUris", Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelableArrayList("fileUris")
        }
        if (fileUris == null) {
            // TODO: Better Handle this Error
            Log.w("Multi[onCreate]", "fileUris is null")
            return
        }

//        Log.d("Multi[onViewCreated]", "viewModel.selectedUris.value?.size: ${viewModel.selectedUris.value?.size}")
//        Log.d("Multi[onViewCreated]", "fileUris.size: ${fileUris.size}")
//        if (viewModel.selectedUris.value == null) {
//            Log.i("Multi[onCreate]", "RESET SELECTED URIS on viewModel null")
//            viewModel.selectedUris.value = fileUris.toSet()
//        } else if (viewModel.selectedUris.value?.size != fileUris.size) {
//            Log.i("Multi[onCreate]", "RESET SELECTED URIS on size mismatch")
//            viewModel.selectedUris.value = fileUris.toSet()
//        } else {
//            Log.i("Multi[onCreate]", "REUSE OLD SELECTED DATA")
//        }
//        val selectedUris = viewModel.selectedUris.value!!.toMutableSet()
//        Log.d("Multi[onViewCreated]", "selectedUris.size: ${selectedUris.size}")

        // TODO: Implement viewModel functions vs using selectedUris
        viewModel.setInitialUris(fileUris)
        val selectedUris = viewModel.getSelectedUris()
        Log.d("Multi[onViewCreated]", "selectedUris: $selectedUris")

        if (!::adapter.isInitialized) {
            Log.i("Multi[onViewCreated]", "INITIALIZE NEW ADAPTER")
            adapter = UploadMultiAdapter(fileUris, selectedUris.toMutableSet()) { updated ->
                viewModel.selectedUris.value = updated
                binding.uploadButton.text = getString(R.string.upload_multi, updated.size)
            }
        }

        val spanCount =
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 2 else 3
        Log.i("Multi[onViewCreated]", "GridLayoutManager: $spanCount")
        binding.previewRecycler.layoutManager = GridLayoutManager(requireContext(), spanCount)
        if (binding.previewRecycler.adapter == null) {
            binding.previewRecycler.adapter = adapter
        }

        // Upload Options - TODO: Set default options here...
        //val fileAlbums = mutableListOf<Int>()
        val editRequest = FileEditRequest()

        // Upload Button
        binding.uploadButton.text = getString(R.string.upload_multi, selectedUris.size)
        binding.uploadButton.setOnClickListener {
            val selectedUris = viewModel.getSelectedUris()
            //Log.d("uploadButton", "selectedUris: $selectedUris")
            Log.d("uploadButton", "selectedUris.size: ${selectedUris.size}")
            if (selectedUris.isEmpty()) {
                Toast.makeText(requireContext(), "No Files Selected!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("uploadButton", "editRequest: $editRequest")
            processMultiUpload(selectedUris, editRequest)
        }

        // Options Button
        binding.optionsButton.setOnClickListener {
            Log.d("optionsButton", "setOnClickListener: navigate: nav_item_settings")
            navController.navigate(R.id.nav_item_settings, bundleOf("hide_bottom_nav" to true))
        }

        // Albums Button
        // TODO: Duplicate from UploadFragment
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

    private fun processMultiUpload(fileUris: Set<Uri>, editRequest: FileEditRequest? = null) {
        Log.d("processMultiUpload", "fileUris: $fileUris")
        Log.d("processMultiUpload", "fileUris.size: ${fileUris.size}")
        Log.d("processMultiUpload", "editRequest: $editRequest")

        val savedUrl = preferences.getString("saved_url", null)
        val authToken = preferences.getString("auth_token", null)
        Log.d("processMultiUpload", "savedUrl: $savedUrl - authToken: $authToken")
        val shareUrl = preferences.getBoolean("share_after_upload", true)
        Log.d("processMultiUpload", "shareUrl: $shareUrl")

        if (savedUrl == null || authToken == null) {
            Log.w("processMultiUpload", "Missing OR savedUrl/authToken")
            Toast.makeText(requireContext(), getString(R.string.tst_no_url), Toast.LENGTH_SHORT)
                .show()
            return
        }

        val msg = "Uploading ${fileUris.size} Files…"
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

        val api = ServerApi(requireContext(), savedUrl)
        Log.d("processMultiUpload", "api: $api")
        val results: MutableList<ServerApi.UploadResponse> = mutableListOf()
        // TODO: Determine why we have to set currentContext here...
        val currentContext = requireContext()
        lifecycleScope.launch {
            for (fileUri in fileUris) {
                Log.d("processMultiUpload", "fileUri: $fileUri")
                val fileName = currentContext.getFileNameFromUri(fileUri)
                Log.d("processMultiUpload", "fileName: $fileName")
                try {
                    val inputStream = currentContext.contentResolver.openInputStream(fileUri)
                    if (inputStream == null) {
                        Log.w("processMultiUpload", "inputStream is null")
                        continue
                    }
                    val response =
                        api.upload(fileName!!, inputStream, editRequest ?: FileEditRequest())
                    Log.d("processMultiUpload", "response: $response")
                    if (response.isSuccessful) {
                        val uploadResponse = response.body()
                        Log.d("processMultiUpload", "uploadResponse: $uploadResponse")
                        if (uploadResponse != null) {
                            results.add(uploadResponse)
                        }
                    } else {
                        val msg = "Error: ${response.code()}: ${response.message()}"
                        Log.w("processMultiUpload", "UPLOAD ERROR: $msg")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Log.d("processMultiUpload", "results: $results")
            Log.d("processMultiUpload", "results,size: ${results.size}")
            if (results.isEmpty()) {
                // TODO: Handle upload failures better...
                Toast.makeText(requireContext(), "All Uploads Failed!", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val params = Bundle().apply { putString("multi", "true") }
            Firebase.analytics.logEvent("upload_file", params)
            val destUrl = if (results.size != 1) "${savedUrl}/files/" else results[0].url
            Log.d("processMultiUpload", "destUrl: $destUrl")
            val msg = "Uploaded ${results.size} Files."
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            val bundle = bundleOf("url" to destUrl)
            navController.navigate(
                R.id.nav_item_home, bundle, NavOptions.Builder()
                    .setPopUpTo(navController.graph.id, true)
                    .build()
            )
        }
    }
}
