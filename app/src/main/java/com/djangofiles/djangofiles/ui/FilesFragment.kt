package com.djangofiles.djangofiles.ui

import android.content.Context.MODE_PRIVATE
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
import com.djangofiles.djangofiles.databinding.FragmentFilesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!

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
        Log.d("File[onDestroyView]", "webView.destroy()")
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("File[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")

        val sharedPreferences = context?.getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val savedUrl = sharedPreferences?.getString("saved_url", "").toString()
        Log.d("File[onViewCreated]", "savedUrl: $savedUrl")
        val authToken = sharedPreferences?.getString("auth_token", null)
        Log.d("File[onViewCreated]", "authToken: $authToken")
        val filesPerPage = sharedPreferences?.getInt("files_per_page", 0)
        Log.d("AddServer", "filesPerPage: $filesPerPage")

        var perPage = filesPerPage ?: 25
        var atEnd = false

        val api = ServerApi(requireContext(), savedUrl)
        Log.d("File[onViewCreated]", "POST API - PRE LAUNCH")
        lifecycleScope.launch {
            try {
                val response = api.recent(perPage, 0)
                Log.d("File[onViewCreated]", "response: $response")
                if (response.isSuccessful) {
                    val fileResponse = response.body()
                    Log.d("File[onScrolled]", "fileResponse.count: ${fileResponse?.count()}")
                    if (fileResponse != null) {
                        if (fileResponse.count() < perPage) {
                            atEnd = true
                        }
                        val customAdapter =
                            FilesViewAdapter(requireContext(), fileResponse.toMutableList())
                        val recyclerView: RecyclerView = binding.myRecyclerView
                        recyclerView.layoutManager = LinearLayoutManager(requireContext())
                        recyclerView.adapter = customAdapter
                        binding.loadingSpinner.visibility = View.GONE
                    } else {
                        Log.d("File[onViewCreated]", "Fetch Failed - 1nd else")
                        binding.loadingSpinner.visibility = View.GONE
                        val msg = "Fetching Recent Files Failed."
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Log.d("File[onViewCreated]", "Fetch Failed - 2nd else")
                    binding.loadingSpinner.visibility = View.GONE
                    val msg = "Fetching Recent Files Failed."
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.loadingSpinner.visibility = View.GONE
                val msg = e.message ?: "Unknown Error!"
                Log.w("File[onViewCreated]", "msg: $msg")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.myRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (!rv.canScrollVertically(1)) {
                    if (atEnd) {
                        Log.i("File[onScrolled]", "AT END - NOTHING TO DO")
                        return
                    }
                    binding.loadingSpinner.visibility = View.VISIBLE
                    val currentCount = binding.myRecyclerView.adapter?.itemCount ?: 0
                    Log.d("File[onScrolled]", "currentCount: $currentCount")
                    val adapter = binding.myRecyclerView.adapter as? FilesViewAdapter
                    lifecycleScope.launch {
                        try {
                            val moreResponse = api.recent(perPage, currentCount)
                            Log.d("File[onScrolled]", "moreResponse.code: ${moreResponse.code()}")
                            if (moreResponse.isSuccessful) {
                                val moreData = moreResponse.body()
                                Log.d("File[onScrolled]", "moreData.count: ${moreData?.count()}")
                                if (!moreData.isNullOrEmpty()) {
                                    adapter?.addData(moreData)
                                    binding.loadingSpinner.visibility = View.GONE
                                    if (moreData.count() < perPage) {
                                        Log.i("File[onScrolled]", "LESS THAN $perPage - SET AT END")
                                        atEnd = true
                                    }
                                } else {
                                    Log.i("File[onScrolled]", "NO DATA RETURNED - SET AT END")
                                    binding.loadingSpinner.visibility = View.GONE
                                    atEnd = true
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("File[onScrolled]", "Exception: $e")
                            binding.loadingSpinner.visibility = View.GONE
                            val msg = e.message ?: "Error Fetching"
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("File[onSave]", "outState: ${outState.size()}")
        super.onSaveInstanceState(outState)
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
