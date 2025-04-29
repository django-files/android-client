package com.djangofiles.djangofiles.ui.files

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Slide
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.databinding.FragmentFilesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//import android.net.Network
//import android.net.NetworkCapabilities
//import android.net.NetworkRequest
//import android.view.Gravity
//import android.view.ViewTreeObserver
//import androidx.core.view.doOnPreDraw
//import androidx.transition.Slide
//import androidx.transition.TransitionInflater

class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!

    private var atEnd = false
    private var errorCount = 0

    //private lateinit var key: String

    private lateinit var api: ServerApi
    private lateinit var filesAdapter: FilesViewAdapter

    //private val viewModel: FilesViewModel by viewModels()
    private val viewModel: FilesViewModel by activityViewModels()

    //private lateinit var connectivityManager: ConnectivityManager
    //
    //override fun onCreate(savedInstanceState: Bundle?) {
    //    super.onCreate(savedInstanceState)
    //    connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    //}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("File[onCreateView]", "savedInstanceState: ${savedInstanceState?.size()}")

        //enterTransition = Slide(Gravity.END)
        returnTransition = Slide(Gravity.END)

        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        Log.d("File[onDestroyView]", "ON DESTROY")
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("File[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")

        Log.d("File[onViewCreated]", "DELAY: postponeEnterTransition")
        postponeEnterTransition()
        binding.filesRecyclerView.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    binding.filesRecyclerView.viewTreeObserver.removeOnPreDrawListener(this)
                    Log.d("File[onViewCreated]", "BEGIN: startPostponedEnterTransition")
                    startPostponedEnterTransition()
                    return true
                }
            }
        )

        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("saved_url", "").toString()
        //Log.d("File[onViewCreated]", "savedUrl: $savedUrl")
        val authToken = sharedPreferences.getString("auth_token", "")
        //Log.d("File[onViewCreated]", "authToken: $authToken")
        if (authToken.isNullOrEmpty()) {
            Log.e("File[onViewCreated]", "NO AUTH TOKEN")
            Toast.makeText(context, "Missing Auth Token!", Toast.LENGTH_LONG).show()
            return
        }
        val previewMetered = sharedPreferences.getBoolean("file_preview_metered", false)
        Log.i("File[onViewCreated]", "previewMetered: $previewMetered")
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        Log.i("File[onViewCreated]", "METERED: ${connectivityManager.isActiveNetworkMetered}")
        val isMetered = if (previewMetered) false else connectivityManager.isActiveNetworkMetered
        Log.i("File[onViewCreated]", "isMetered: $isMetered")

        if (connectivityManager.isActiveNetworkMetered) {
            binding.meteredText.visibility = View.VISIBLE
            binding.meteredText.setOnClickListener {
                //binding.meteredText.visibility = View.GONE
                binding.meteredText.animate()
                    .translationY(-binding.meteredText.height.toFloat())
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        binding.meteredText.visibility = View.GONE
                    }
                    .start()
            }
        }

        if (viewModel.savedUrl.value != null) {
            Log.d("File[onViewCreated]", "SAVED DATA FOR URL: ${viewModel.savedUrl.value}")
            if (viewModel.savedUrl.value != savedUrl) {
                Log.i("File[onViewCreated]", "OLD SAVED DATA FOUND - CLEARING DATA")
                viewModel.filesData.value = null
                viewModel.savedUrl.value = savedUrl
            }
        } else {
            viewModel.savedUrl.value = savedUrl
        }

        //key = authToken.take(6)
        //Log.d("File[onViewCreated]", "key: $key")
        val perPage = sharedPreferences.getInt("files_per_page", 25)
        Log.d("File[onViewCreated]", "perPage: $perPage")

        api = ServerApi(requireContext(), savedUrl)
        filesAdapter = FilesViewAdapter(requireContext(), mutableListOf(), isMetered)
        binding.filesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.filesRecyclerView.adapter = filesAdapter

        Log.d("File[onViewCreated]", "filesAdapter.itemCount: ${filesAdapter.itemCount}")

        viewModel.filesData.observe(viewLifecycleOwner) { list ->
            Log.d("filesData[observe]", "list: ${list?.size}")
            if (list != null && filesAdapter.itemCount == 0) {
                Log.i("filesData[observe]", "CACHE LOAD")
                filesAdapter.addData(list)
                binding.loadingSpinner.visibility = View.GONE
            } else if (list == null) {
                Log.i("filesData[observe]", "FETCH NEW DATA")
                lifecycleScope.launch { getFiles(perPage) }
            }

            //(view.parent as? ViewGroup)?.doOnPreDraw {
            //    Log.i("File[onViewCreated]", "startPostponedEnterTransition")
            //    startPostponedEnterTransition()
            //}
        }

        viewModel.atEnd.observe(viewLifecycleOwner) {
            Log.d("atEnd[observe]", "atEnd: $atEnd")
            atEnd = it ?: false
        }

        if (viewModel.filesData.value == null) {
            Log.i("File[onViewCreated]", "MANUALLY LOADING DATA")
            //viewModel.filesData.value = null
            lifecycleScope.launch { getFiles(perPage) }
        }

        binding.filesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (!rv.canScrollVertically(1)) {
                    Log.d("File[onScrolled]", "atEnd: $atEnd")
                    if (!atEnd) {
                        binding.loadingSpinner.visibility = View.VISIBLE
                        lifecycleScope.launch {
                            getFiles(perPage)
                        }
                    } else {
                        Log.i("File[onScrolled]", "AT END - NOTHING TO DO")
                    }
                }
            }
        })

        //viewModel.filesData.observe(viewLifecycleOwner) { newList ->
        //    Log.d("filesData[observe]", "newList.size: ${newList.size}")
        //}

        // Monitor viewModel.deleteId for changes and attempt to filesAdapter.deleteById the ID
        viewModel.deleteId.observe(viewLifecycleOwner) { fileId ->
            Log.d("deleteId[observe]", "fileId: $fileId")
            if (fileId != null) {
                filesAdapter.deleteById(fileId)
            }
        }
    }

    suspend fun getFiles(perPage: Int) {
        try {
            Log.d("getFiles", "filesAdapter.itemCount: ${filesAdapter.itemCount}")
            val response = api.recent(perPage, filesAdapter.itemCount)
            Log.d("getFiles", "moreResponse.code: ${response.code()}")
            if (response.isSuccessful) {
                val data = response.body()
                Log.d("getFiles", "moreData.count: ${data?.count()}")
                if (!data.isNullOrEmpty()) {
                    filesAdapter.addData(data)
                    viewModel.filesData.value = filesAdapter.getData()
                    if (data.count() < perPage) {
                        Log.i("getFiles", "LESS THAN $perPage - SET AT END")
                        atEnd = true
                        viewModel.atEnd.value = atEnd
                    }
                } else {
                    Log.i("getFiles", "NO DATA RETURNED - SET AT END")
                    atEnd = true
                    viewModel.atEnd.value = atEnd
                }
            } else {
                Log.e("getFiles", "Error Fetching Files")
                errorCount += 1
                val msg = "Error ${response.code()} Fetching Files"
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Log.e("getFiles", "Exception: $e")
            errorCount += 1
            val msg = e.message ?: "Exception Fetching Files"
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }
        binding.loadingSpinner.visibility = View.GONE
        if (errorCount > 5) {
            atEnd = true
            viewModel.atEnd.value = atEnd
            val msg = "Recieved $errorCount Errors. Aborting!"
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    //private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    //    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
    //        val isMetered = connectivityManager.isActiveNetworkMetered
    //        Log.d("onCapabilitiesChanged", "isMetered: $isMetered")
    //    }
    //
    //    override fun onLost(network: Network) {
    //        val isMetered = connectivityManager.isActiveNetworkMetered
    //        Log.d("onLost", "isMetered: $isMetered")
    //    }
    //}
    //
    //override fun onStart() {
    //    Log.d("File[onStart]", "ON START")
    //    super.onStart()
    //    val request = NetworkRequest.Builder().build()
    //    connectivityManager.registerNetworkCallback(request, networkCallback)
    //}
    //
    //override fun onStop() {
    //    Log.d("File[onStop]", "ON STOP")
    //    super.onStop()
    //    connectivityManager.unregisterNetworkCallback(networkCallback)
    //}
    //
    override fun onPause() {
        Log.d("File[onPause]", "ON PAUSE")
        super.onPause()
    }

    override fun onResume() {
        Log.d("File[onResume]", "ON RESUME")
        super.onResume()
    }
}
