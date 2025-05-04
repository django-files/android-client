package com.djangofiles.djangofiles.ui.files

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.webkit.CookieManager
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Slide
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.databinding.FragmentFilesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.InputStream

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
        _binding = null
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

        Log.e("File[onViewCreated]", "Glide CookieMonster")
        val cookie = CookieManager.getInstance().getCookie(savedUrl)
        Log.d("Glide", "cookie: $cookie")
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Cookie", cookie)
                    .build()
                chain.proceed(request)
            }
            .build()
        val okHttpUrlLoader = OkHttpUrlLoader.Factory(okHttpClient)
        Glide.get(requireContext()).registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            okHttpUrlLoader
        )

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
                // TODO: The recycler view does not slide until after this animation completes...
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
        // Monitor viewModel.editRequest for changes and do something...
        viewModel.editRequest.observe(viewLifecycleOwner) { editRequest ->
            Log.d("editId[observe]", "editRequest: $editRequest")
            if (editRequest != null) {
                filesAdapter.editById(editRequest)
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

    //override fun onStart() {
    //    Log.d("File[onStart]", "ON START")
    //    super.onStart()
    //    val request = NetworkRequest.Builder().build()
    //    connectivityManager.registerNetworkCallback(request, networkCallback)
    //}

    //override fun onStop() {
    //    Log.d("File[onStop]", "ON STOP")
    //    super.onStop()
    //    connectivityManager.unregisterNetworkCallback(networkCallback)
    //}

    override fun onPause() {
        Log.d("File[onPause]", "ON PAUSE")
        super.onPause()
    }

    override fun onResume() {
        Log.d("File[onResume]", "ON RESUME")
        super.onResume()
    }
}

fun openUrl(context: Context, url: String) {
    val openIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(url.toUri(), "text/plain")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(openIntent, null))
}

fun shareUrl(context: Context, url: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(shareIntent, null))
}

fun isGlideMime(mimeType: String): Boolean {
    return when (mimeType.lowercase()) {
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
        "image/heif",
            -> true

        else -> false
    }
}

fun isCodeMime(mimeType: String): Boolean {
    if (mimeType.startsWith("text/x-script")) return true
    return when (mimeType.lowercase()) {
        "application/atom+xml",
        "application/javascript",
        "application/json",
        "application/ld+json",
        "application/rss+xml",
        "application/xml",
        "application/x-httpd-php",
        "application/x-python",
        "application/x-www-form-urlencoded",
        "application/yaml",
        "text/javascript",
        "text/python",
        "text/x-go",
        "text/x-ruby",
        "text/x-php",
        "text/x-python",
        "text/x-shellscript",
            -> true

        else -> false
    }
}

fun getGenericIcon(mimeType: String): Int = when {
    isCodeMime(mimeType) -> R.drawable.md_code_blocks_24
    mimeType.startsWith("application/json") -> R.drawable.md_file_json_24
    mimeType.startsWith("application/pdf") -> R.drawable.md_picture_as_pdf_24
    mimeType.startsWith("image/gif") -> R.drawable.md_gif_box_24
    mimeType.startsWith("image/png") -> R.drawable.md_file_png_24
    mimeType.startsWith("text/csv") -> R.drawable.md_csv_24
    mimeType.startsWith("audio/") -> R.drawable.md_music_note_24
    mimeType.startsWith("image/") -> R.drawable.md_imagesmode_24
    mimeType.startsWith("text/") -> R.drawable.md_docs_24
    mimeType.startsWith("video/") -> R.drawable.md_videocam_24
    else -> R.drawable.md_unknown_document_24
}
