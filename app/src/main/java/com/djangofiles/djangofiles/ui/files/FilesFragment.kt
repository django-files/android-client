package com.djangofiles.djangofiles.ui.files

import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.djangofiles.djangofiles.api.ServerApi
import com.djangofiles.djangofiles.api.ServerApi.RecentResponse
import com.djangofiles.djangofiles.databinding.FragmentFilesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!

    private var atEnd = false
    private var errorCount = 0

    private lateinit var key: String

    private lateinit var api: ServerApi
    private lateinit var filesAdapter: FilesViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("File[onCreateView]", "savedInstanceState: ${savedInstanceState?.size()}")
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

        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences.getString("saved_url", "").toString()
        Log.d("File[onViewCreated]", "savedUrl: $savedUrl")
        val authToken = sharedPreferences.getString("auth_token", "")
        Log.d("File[onViewCreated]", "authToken: $authToken")
        if (authToken.isNullOrEmpty()) {
            Log.w("File[onViewCreated]", "NO AUTH TOKEN")
            Toast.makeText(context, "Missing Auth Token!", Toast.LENGTH_LONG).show()
            return
        }
        key = authToken.take(6)
        Log.d("File[onViewCreated]", "key: $key")
        val perPage = sharedPreferences.getInt("files_per_page", 25)
        Log.d("File[onViewCreated]", "perPage: $perPage")

        api = ServerApi(requireContext(), savedUrl)
        filesAdapter = FilesViewAdapter(requireContext(), mutableListOf())
        binding.filesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.filesRecyclerView.adapter = filesAdapter

        // TODO: Implement Parcelize/Parcelable
        val savedData: List<RecentResponse>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState?.getParcelableArrayList("files_data_$key", RecentResponse::class.java)
            } else {
                @Suppress("DEPRECATION")
                savedInstanceState?.getParcelableArrayList("files_data_$key")
            }
        Log.d("getFiles", "savedData: ${savedData?.size}")
        if (savedInstanceState != null && savedData != null) {
            Log.i("File[onViewCreated]", "LOADING SAVED DATA")
            atEnd = savedInstanceState.getBoolean("files_at_end_$key")
            Log.d("File[onViewCreated]", "atEnd: $atEnd")
            filesAdapter.addData(savedData)
            binding.loadingSpinner.visibility = View.GONE
        } else {
            Log.i("File[onViewCreated]", "FETCHING NEW DATA")
            lifecycleScope.launch {
                getFiles(perPage)
            }
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("File[onSave]", "ON SAVE: ${outState.size()}")
        Log.d("File[onSave]", "key: $key")
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("files_data_$key", ArrayList(filesAdapter.getData()))
        outState.putBoolean("files_at_end_$key", atEnd)
        Log.d("File[onSave]", "SAVE DONE: ${outState.size()}")
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
                    if (data.count() < perPage) {
                        Log.i("getFiles", "LESS THAN $perPage - SET AT END")
                        atEnd = true
                    }
                } else {
                    Log.i("getFiles", "NO DATA RETURNED - SET AT END")
                    atEnd = true
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
            val msg = "Recieved $errorCount Errors. Aborting!"
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPause() {
        Log.d("File[onPause]", "ON PAUSE")
        super.onPause()
    }

    override fun onResume() {
        Log.d("File[onResume]", "ON RESUME")
        super.onResume()
    }
}
