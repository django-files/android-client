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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.djangofiles.djangofiles.ServerApi.FilesEditRequest
import com.djangofiles.djangofiles.databinding.FragmentFilesBinding
import kotlinx.coroutines.CoroutineScope
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
    private var isMetered = false

    private lateinit var api: ServerApi
    private lateinit var filesAdapter: FilesViewAdapter

    //private val viewModel: FilesViewModel by viewModels()
    private val viewModel: FilesViewModel by activityViewModels()

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
        val previewMetered = sharedPreferences.getBoolean("file_preview_metered", false)
        Log.d("File[checkMetered]", "previewMetered: $previewMetered")

        if (authToken.isNullOrEmpty()) {
            Log.e("File[onViewCreated]", "NO AUTH TOKEN")
            Toast.makeText(context, "Missing Auth Token!", Toast.LENGTH_LONG).show()
            return
        }

        Log.d("File[onViewCreated]", "Glide CookieMonster")
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

//        val previewMetered = sharedPreferences.getBoolean("file_preview_metered", false)
//        Log.d("File[onViewCreated]", "previewMetered: $previewMetered")
//        val connectivityManager =
//            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        Log.d("File[onViewCreated]", "METERED: ${connectivityManager.isActiveNetworkMetered}")
//        val isMetered = if (previewMetered) false else connectivityManager.isActiveNetworkMetered
//        Log.d("File[onViewCreated]", "isMetered: $isMetered")
//        Log.d("File[onViewCreated]", "viewModel.meterHidden.value: ${viewModel.meterHidden.value}")
//        displayMetered = if (viewModel.meterHidden.value == true) false else isMetered
//        Log.d("File[onViewCreated]", "displayMetered: $displayMetered")
//
//        if (displayMetered && connectivityManager.isActiveNetworkMetered) {
//            binding.meteredText.visibility = View.VISIBLE
//            binding.meteredText.setOnClickListener {
//                // TODO: The recycler view does not slide until after this animation completes...
//                //binding.meteredText.visibility = View.GONE
//                viewModel.meterHidden.value = true
//                binding.meteredText.animate()
//                    .translationY(-binding.meteredText.height.toFloat())
//                    .alpha(0f)
//                    .setDuration(300)
//                    .withEndAction {
//                        binding.meteredText.visibility = View.GONE
//                    }
//                    .start()
//            }
//        }

        val key = "${savedUrl}/${authToken.take(8)}"
        if (viewModel.viewKey.value != null) {
            Log.d("File[onViewCreated]", "Found Saved viewKey: ${viewModel.viewKey.value}")
            if (viewModel.viewKey.value != key) {
                Log.i("File[onViewCreated]", "VIEW KEY MISMATCH - CLEARING VIEW MODEL DATA")
                viewModel.filesData.value = null
                viewModel.viewKey.value = key
                viewModel.selected.value = mutableSetOf<Int>()
            }
        } else {
            Log.i("File[onViewCreated]", "Set viewKey: $key")
            viewModel.viewKey.value = key
        }

        //key = authToken.take(6)
        //Log.d("File[onViewCreated]", "key: $key")
        val perPage = sharedPreferences.getInt("files_per_page", 25)
        Log.d("File[onViewCreated]", "perPage: $perPage")

        val selected = viewModel.selected.value?.toMutableSet() ?: mutableSetOf<Int>()
        Log.d("File[onViewCreated]", "selected.size: ${selected.size}")

        api = ServerApi(requireContext(), savedUrl)
        //filesAdapter = FilesViewAdapter(requireContext(), mutableListOf(), selectedUris, isMetered)

        checkMetered(previewMetered) // Set isMetered

        //if (!::filesAdapter.isInitialized) {
        //    Log.i("File[onViewCreated]", "INITIALIZE ADAPTER isMetered: $isMetered")
        //    filesAdapter =
        //        FilesViewAdapter(requireContext(), mutableListOf(), selected, isMetered)
        //}

        if (!::filesAdapter.isInitialized) {
            Log.i("File[onViewCreated]", "INITIALIZE ADAPTER isMetered: $isMetered")
            filesAdapter =
                FilesViewAdapter(requireContext(), mutableListOf(), selected, isMetered) { list ->
                    Log.d("File[onViewCreated]", "list.size: ${list.size}")
                    viewModel.selected.value = list
                }
        }

        Log.d("File[selectedUris]", "viewModel.selectedUris.value: ${viewModel.selected.value}")
        if (viewModel.selected.value != null && viewModel.selected.value!!.isEmpty() != true) {
            binding.filesSelectedHeader.visibility = View.VISIBLE
            binding.filesSelectedText.text =
                getString(R.string.files_selected, viewModel.selected.value?.size)
        }

        binding.filesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.filesRecyclerView.adapter = filesAdapter

        Log.d("File[onViewCreated]", "filesAdapter.itemCount: ${filesAdapter.itemCount}")

        viewModel.filesData.observe(viewLifecycleOwner) { list ->
            Log.d("filesData[observe]", "list: ${list?.size}")
            if (list != null && filesAdapter.itemCount == 0) {
                Log.i("filesData[observe]", "CACHE LOAD")
                filesAdapter.addData(list)
                Log.d("loadingSpinner", "loadingSpinner: View.GONE")
                binding.loadingSpinner.visibility = View.GONE
            } else if (list == null) {
                Log.i("filesData[observe]", "FETCH NEW DATA")
                lifecycleScope.launch { getFiles(perPage) }
            } else {
                // TODO: Consider setting default view to GONE
                Log.d("filesData[observe]", "loadingSpinner: iew.GONE")
                binding.loadingSpinner.visibility = View.GONE
            }

            //(view.parent as? ViewGroup)?.doOnPreDraw {
            //    Log.i("File[onViewCreated]", "startPostponedEnterTransition")
            //    startPostponedEnterTransition()
            //}
        }
        viewModel.selected.observe(viewLifecycleOwner) { selected ->
            Log.d("selected[observe]", "selected.size: ${selected?.size}")
            Log.d("selected[observe]", "selected: $selected")
            //viewModel.selected.value = selected
            if (selected.isNotEmpty()) {
                binding.filesSelectedHeader.visibility = View.VISIBLE
            } else {
                binding.filesSelectedHeader.visibility = View.GONE
            }
            binding.filesSelectedText.text = getString(R.string.files_selected, selected.size)
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
                        Log.d("File[onScrolled]", "loadingSpinner: View.VISIBLE")
                        binding.loadingSpinner.visibility = View.VISIBLE
                        lifecycleScope.launch {
                            getFiles(perPage)
                        }
                    } else {
                        Log.d("File[onScrolled]", "AT END - NOTHING TO DO")
                    }
                }
            }
        })

        //binding.fileMenu.setOnClickListener {
        //    (requireActivity() as MainActivity).binding.drawerLayout.openDrawer(GravityCompat.START)
        //}

        binding.filesSelectAll.setOnClickListener {
            Log.d("File[filesSelectAll]", "setOnClickListener")
            Log.d("File[filesSelectAll]", "size: ${viewModel.selected.value?.size}")
            binding.filesSelectedHeader.visibility = View.VISIBLE
            val positionIds: MutableSet<Int> =
                viewModel.filesData.value?.indices?.toMutableSet() ?: mutableSetOf<Int>()
            Log.d("deleteId[observe]", "positionIds: $positionIds")
            viewModel.selected.value = positionIds
            filesAdapter.selected.addAll(viewModel.selected.value!!)
            Log.d("File[filesSelectAll]", "size: ${viewModel.selected.value?.size}")
            binding.filesSelectedText.text =
                getString(R.string.files_selected, viewModel.selected.value?.size)

            if (positionIds.isNotEmpty()) {
                //val first = positionIds.first()
                //Log.d("File[filesDeselect]", "first: $first ")
                val last = positionIds.size
                Log.d("File[filesDeselect]", "last: $last")
                filesAdapter.notifyItemRangeChanged(0, positionIds.size)
                //filesAdapter.notifyItemRangeChanged(first, last - first + 1)
            }

            //filesAdapter.notifyDataSetChanged()

            //for (i in 0 until binding.filesRecyclerView.childCount) {
            //    val view = binding.filesRecyclerView.getChildAt(i)
            //    val holder =
            //        binding.filesRecyclerView.getChildViewHolder(view) as FilesViewAdapter.ViewHolder
            //    holder.checkMark.visibility = View.VISIBLE
            //    holder.itemBorder.setBackgroundResource(R.drawable.image_border_selected_2dp)
            //}
        }

        binding.filesDeselect.setOnClickListener {
            Log.d("File[filesDeselect]", "setOnClickListener")
            val currentSelected = viewModel.selected.value?.toSet()
            Log.d("File[filesDeselect]", "currentSelected: $currentSelected")
            viewModel.selected.value = mutableSetOf<Int>()
            Log.d("File[filesDeselect]", "currentSelected: $currentSelected")
            filesAdapter.selected.clear()
            Log.d("File[filesDeselect]", "currentSelected: $currentSelected")
            Log.d(
                "File[filesDeselect]",
                "viewModel.selected.value.size: ${viewModel.selected.value?.size}"
            )
            binding.filesSelectedHeader.visibility = View.GONE

            Log.d("File[filesDeselect]", "currentSelected: $currentSelected")
            currentSelected?.forEach { position ->
                Log.d("File[filesDeselect]", "position: $position")
                filesAdapter.notifyItemChanged(position)
            }
            Log.d("File[filesDeselect]", "currentSelected: $currentSelected")

            //filesAdapter.notifyDataSetChanged()

            //for (i in 0 until binding.filesRecyclerView.childCount) {
            //    val view = binding.filesRecyclerView.getChildAt(i)
            //    val holder =
            //        binding.filesRecyclerView.getChildViewHolder(view) as FilesViewAdapter.ViewHolder
            //    holder.checkMark.visibility = View.GONE
            //    holder.itemBorder.background = null
            //}
        }
        binding.deleteAllButton.setOnClickListener {
            Log.d("File[deleteAllButton]", "viewModel.selected.value: ${viewModel.selected.value}")
            if (viewModel.selected.value.isNullOrEmpty()) {
                return@setOnClickListener
            }

            val positions = viewModel.selected.value!!.toList()
            Log.d("File[deleteAllButton]", "positions: $positions")

            val data = viewModel.filesData.value!!.toList()
            Log.d("File[deleteAllButton]", "data.size: ${data.size}")
            val selectedPositions: List<Int> = viewModel.selected.value!!.toList()
            Log.d("File[deleteAllButton]", "selectedPositions: $selectedPositions")
            val ids: List<Int> = selectedPositions.map { index -> data[index].id }
            Log.d("File[deleteAllButton]", "ids: $ids")

            //filesAdapter.deleteIds(positions)
            //viewModel.selected.value?.clear()

            lifecycleScope.launch {
                val response = api.filesDelete(FilesEditRequest(ids = ids))
                Log.d("File[deleteAllButton]", "response: $response")
                if (response.isSuccessful) {
                    Log.d("File[deleteAllButton]", "filesAdapter.deleteIds: $selectedPositions")
                    filesAdapter.deleteIds(selectedPositions)
                }
            }
        }
        binding.expireAllButton.setOnClickListener {
            Log.d("File[expireAllButton]", "setOnClickListener")
            //val ids = mutableListOf<Int>()
            //viewModel.selected.value?.forEach { fileResponse ->
            //    Log.d("File[expireAllButton]", "fileResponse: $fileResponse")
            //    ids.add(fileResponse.id)
            //}
            //Log.d("File[expireAllButton]", "ids: $ids")
            Log.d("File[expireAllButton]", "viewModel.selected.value: ${viewModel.selected.value}")
            fun onUpdated(newExpr: String) {
                Log.d("onUpdated", "viewModel.selected.value: ${viewModel.selected.value}")
                for (index in viewModel.selected.value!!) {
                    val file = viewModel.filesData.value?.get(index)
                    file?.expr = newExpr
                }
                filesAdapter.notifyIdsUpdated(viewModel.selected.value!!.toList())
            }

            val fileIds = getFileIds(viewModel.selected.value!!.toList())
            Log.d("File[expireAllButton]", "fileIds: $fileIds")
            showExpireDialog(requireContext(), fileIds, ::onUpdated)
            //lifecycleScope.launch {
            //    val response = api.filesEdit(FilesEditRequest(ids = ids, expr = "1y"))
            //    Log.d("File[expireAllButton]", "response: $response")
            //    if (response.isSuccessful) {
            //        Log.d("File[expireAllButton]", "Process Success...")
            //    }
            //}
        }
        binding.albumAllButton.setOnClickListener {
            Log.d("File[albumAllButton]", "viewModel.selected.value: ${viewModel.selected.value}")
            showAlbumsDialog(requireContext(), viewModel.selected.value!!)
        }

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

    fun getFileIds(positions: List<Int>): List<Int> {
        Log.d("File[getFileIds]", "positions: $positions")
        val data = viewModel.filesData.value!!.toList()
        Log.d("File[getFileIds]", "data.size: ${data.size}")
        val ids: List<Int> = positions.map { index -> data[index].id }
        Log.d("File[getFileIds]", "ids: $ids")
        return ids
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
        Log.d("loadingSpinner", "loadingSpinner: View.GONE")
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
        checkMetered()
//        val connectivityManager =
//            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//        if (!connectivityManager.isActiveNetworkMetered) {
//            Log.d("File[onResume]", "GONE")
//            binding.meteredText.visibility = View.GONE
//            displayMetered = false
//            filesAdapter.isMetered = false
//        }
    }

    private fun checkMetered(metered: Boolean? = null) {
        Log.d("File[checkMetered]", "checkMetered")
        val previewMetered = if (metered != null) {
            metered
        } else {
            val sharedPreferences =
                requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
            sharedPreferences.getBoolean("file_preview_metered", false)
        }
        Log.d("File[checkMetered]", "previewMetered: $previewMetered")
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        Log.d("File[checkMetered]", "METERED: ${connectivityManager.isActiveNetworkMetered}")
        isMetered = if (previewMetered) false else connectivityManager.isActiveNetworkMetered
        Log.d("File[checkMetered]", "isMetered: $isMetered")

        if (::filesAdapter.isInitialized) {
            filesAdapter.isMetered = isMetered
            Log.d("File[checkMetered]", "filesAdapter.isMetered: ${filesAdapter.isMetered}")
        }

        Log.d("File[checkMetered]", "viewModel.meterHidden.value: ${viewModel.meterHidden.value}")
        val displayMetered = if (viewModel.meterHidden.value == true) false else isMetered
        Log.d("File[checkMetered]", "displayMetered: $displayMetered")

        if (displayMetered && connectivityManager.isActiveNetworkMetered) {
            binding.meteredText.visibility = View.VISIBLE
            binding.meteredText.setOnClickListener {
                // TODO: The recycler view does not slide until after this animation completes...
                //binding.meteredText.visibility = View.GONE
                viewModel.meterHidden.value = true
                binding.meteredText.animate()
                    .translationY(-binding.meteredText.height.toFloat())
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        binding.meteredText.visibility = View.GONE
                    }
                    .start()
            }
        } else {
            binding.meteredText.visibility = View.GONE
        }
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

private fun showExpireDialog(
    context: Context,
    fileIds: List<Int>,
    callback: (newExpr: String) -> Unit
) {
    Log.d("showExpireDialog", "$fileIds: $fileIds")

    val layout = LinearLayout(context)
    layout.orientation = LinearLayout.VERTICAL
    layout.setPadding(10, 0, 10, 40)

    val input = EditText(context)
    input.inputType = android.text.InputType.TYPE_CLASS_TEXT
    input.maxLines = 1
    input.hint = "6mo"
    input.requestFocus()
    layout.addView(input)

    val savedUrl =
        context.getSharedPreferences("AppPreferences", MODE_PRIVATE).getString("saved_url", "")
            .toString()

    AlertDialog.Builder(context)
        .setView(layout)
        .setTitle("Set Expiration")
        .setMessage("Leave Blank for None")
        .setPositiveButton("Save") { _, _ ->
            val newExpire = input.text.toString().trim()
            Log.d("showExpireDialog", "newExpire: $newExpire")
            val api = ServerApi(context, savedUrl)
            CoroutineScope(Dispatchers.IO).launch {
                val response =
                    api.filesEdit(FilesEditRequest(ids = fileIds, expr = newExpire))
                Log.d("showExpireDialog", "response: $response")
                if (response.isSuccessful) {
                    CoroutineScope(Dispatchers.Main).launch {
                        callback(newExpire)
                    }
                } else {
                    Log.w("showExpireDialog", "RESPONSE FAILURE")
                }
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
}


private fun showAlbumsDialog(context: Context, fileIds: MutableSet<Int>) {
    Log.d("showAlbumsDialog", "$fileIds: $fileIds")

    val layout = LinearLayout(context)
    layout.orientation = LinearLayout.VERTICAL
    layout.setPadding(10, 0, 10, 40)

    val input = EditText(context)
    input.inputType = android.text.InputType.TYPE_CLASS_TEXT
    input.maxLines = 1
    input.hint = "Album ID"
    input.requestFocus()
    layout.addView(input)

    AlertDialog.Builder(context)
        .setView(layout)
        .setTitle("Manage Albums")
        .setMessage("Enter the Album ID")
        .setPositiveButton("Save") { _, _ ->
            val newAlbum = input.text.toString().trim()
            Log.d("showAlbumsDialog", "newAlbum: $newAlbum")
        }
        .setNegativeButton("Cancel", null)
        .show()
}
