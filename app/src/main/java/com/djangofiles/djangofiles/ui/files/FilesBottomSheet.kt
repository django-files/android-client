package com.djangofiles.djangofiles.ui.files

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.ServerApi.FileEditRequest
import com.djangofiles.djangofiles.copyToClipboard
import com.djangofiles.djangofiles.databinding.FragmentFilesBottomBinding
import com.djangofiles.djangofiles.db.AlbumDatabase
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentFilesBottomBinding? = null
    private val binding get() = _binding!!

    //private val viewModel: FilesViewModel by viewModels()
    private val viewModel: FilesViewModel by activityViewModels()

    private lateinit var savedUrl: String
    private lateinit var filePassword: String

    companion object {
        fun newInstance(bundle: Bundle) = FilesBottomSheet().apply {
            arguments = bundle
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFilesBottomBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onStart() {
        Log.d("Bottom[onStart]", "ON START")
        super.onStart()
        // Force max height sheet in landscape
        val behavior = BottomSheetBehavior.from(requireView().parent as View)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("File[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")

        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
        savedUrl = sharedPreferences.getString("saved_url", "").toString()

        Log.d("Bottom[onCreateView]", "arguments: $arguments")
        //val fileId = arguments?.getInt("fileId")
        //Log.d("Bottom[onCreateView]", "fileId: $fileId")
        //val fileName = arguments?.getString("fileName")
        //Log.d("Bottom[onCreateView]", "fileName: $fileName")
        //val mimeType = arguments?.getString("mimeType")
        //Log.d("Bottom[onCreateView]", "mimeType: $mimeType")
        //val thumbUrl = arguments?.getString("thumbUrl")
        //Log.d("Bottom[onCreateView]", "thumbUrl: $thumbUrl")
        //val shareUrl = arguments?.getString("shareUrl")
        //Log.d("Bottom[onCreateView]", "shareUrl: $shareUrl")
        //filePassword = arguments?.getString("filePassword") ?: ""
        //Log.d("Bottom[onCreateView]", "filePassword: $filePassword")
        //var isPrivate = requireArguments().getBoolean("isPrivate")
        //Log.d("Bottom[onCreateView]", "isPrivate: $isPrivate")

        val position = requireArguments().getInt("position")
        Log.i("Bottom[onCreateView]", "position: $position")
        val data = viewModel.filesData.value?.get(position)
        Log.i("Bottom[onCreateView]", "data: $data")
        if (data == null) {
            // TODO: HANDLE THIS ERROR FOR SURE!!!
            return
        }
        filePassword = data.password // TODO: FUCKING KILL THIS WITH FIRE!!!

        // Name
        binding.fileName.text = data.name

        // Private
        if (data.private) {
            tintImage(binding.togglePrivate)
        }
        binding.togglePrivate.setOnClickListener {
            data.private = !data.private
            Log.d("togglePrivate", "New Value: ${data.private}")
            val api = ServerApi(requireContext(), savedUrl)
            lifecycleScope.launch {
                val response = api.edit(data.id, FileEditRequest(private = data.private))
                Log.d("deleteButton", "response: $response")
                viewModel.editRequest.value = FileEditRequest(id = data.id, private = data.private)
                if (data.private) {
                    tintImage(binding.togglePrivate)
                } else {
                    binding.togglePrivate.imageTintList = null
                }
            }
        }
        // Album
        binding.albumButton.setOnClickListener {
            Log.d("albumButton", "Album Button")
            val dao = AlbumDatabase.getInstance(requireContext(), savedUrl).albumDao()
            lifecycleScope.launch {
                Log.d("File[albumButton]", "viewModel.selected.value: ${viewModel.selected.value}")
                setFragmentResultListener("albums_result") { _, bundle ->
                    val albums = bundle.getIntegerArrayList("albums")
                    Log.i("File[albumButton]", "albums: $albums")
                    data.albums = albums!!.toList() // TODO: This is enough for non-display items?
                    //viewModel.updateRequest.value = data
                }

                val albums =  withContext(Dispatchers.IO) { dao.getAll() }
                Log.i("File[albumButton]", "albums: $albums")
                val albumFragment = AlbumFragment()
                albumFragment.setAlbumData(albums, listOf(data.id), data.albums)
                albumFragment.show(parentFragmentManager, "AlbumFragment")
            }
        }
        // Share
        binding.shareButton.setOnClickListener {
            shareUrl(requireContext(), data.url)
        }
        // Copy
        binding.copyButton.setOnClickListener {
            copyToClipboard(requireContext(), data.url)
        }

        // Delete
        binding.deleteButton.setOnClickListener {
            Log.d("deleteButton", "fileId: ${data.id}")
            deleteConfirmDialog(savedUrl, data.id, data.name)
        }
        // Password
        if (!filePassword.isEmpty()) {
            tintImage(binding.setPassword)
        }
        binding.setPassword.setOnClickListener {
            Log.d("setPassword", "setOnClickListener")
            setPasswordDialog(requireContext(), data.id, data.name)
        }
        // Expire
        binding.expireButton.setOnClickListener {
            Log.d("expireButton", "Expire Button")
        }
        // Open
        binding.openButton.setOnClickListener {
            openUrl(requireContext(), data.url)
        }

        // Image
        val radius = requireContext().resources.getDimension(R.dimen.image_preview_large)
        binding.imagePreview.setShapeAppearanceModel(
            binding.imagePreview.shapeAppearanceModel
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, radius)
                .build()
        )
        if (isGlideMime(data.mime)) {
            Log.d("Bottom[onCreateView]", "isGlideMime")
            Glide.with(this)
                .load(data.thumb)
                .into(binding.imagePreview)
        } else {
            binding.imagePreview.setImageResource(getGenericIcon(data.mime))
        }
        binding.imagePreview.setOnClickListener {
            Log.d("Bottom[onCreateView]", "onClick: imagePreview")
            findNavController().navigate(R.id.nav_item_files_action_preview, arguments)
            dismiss()
        }
    }

    private fun tintImage(item: ImageView) {
        item.imageTintList =
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.holo_orange_light
                )
            )
    }

    private fun deleteConfirmDialog(savedUrl: String, fileId: Int, fileName: String) {
        Log.d("deleteConfirmDialog", "$fileId - savedUrl: $fileId")
        AlertDialog.Builder(requireContext())
            .setTitle("Delete File?")
            .setMessage(fileName)
            .setPositiveButton("Delete") { _, _ ->
                Log.d("deleteConfirmDialog", "Delete Confirm: fileId $fileId")
                val api = ServerApi(requireContext(), savedUrl)
                lifecycleScope.launch {
                    api.deleteFile(fileId)
                    viewModel.deleteId.value = fileId
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "File Deleted!", Toast.LENGTH_SHORT)
                            .show()
                    }
                    dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setPasswordDialog(context: Context, fileId: Int, fileName: String) {
        Log.d("setPasswordDialog", "$fileId - savedUrl: $fileId")

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(10, 0, 10, 40)

        val input = EditText(context)
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT
        input.maxLines = 1
        input.hint = "Leave Blank to Remove"
        input.setText(filePassword)
        input.requestFocus()
        layout.addView(input)

        AlertDialog.Builder(requireContext())
            .setView(layout)
            .setTitle("Set Password")
            .setMessage(fileName)
            .setPositiveButton("Save") { _, _ ->
                val newPassword = input.text.toString().trim()
                Log.d("setPasswordDialog", "newPassword: $newPassword")
                if (newPassword == filePassword) {
                    Log.d("setPasswordDialog", "Password Not Changed.")
                    return@setPositiveButton
                }
                filePassword = newPassword
                val api = ServerApi(requireContext(), savedUrl)
                lifecycleScope.launch {
                    val response = api.edit(fileId, FileEditRequest(password = newPassword))
                    Log.d("setPasswordDialog", "response: $response")
                    viewModel.editRequest.value =
                        FileEditRequest(id = fileId, password = newPassword)
                    if (newPassword.isEmpty()) {
                        binding.setPassword.imageTintList = null
                    } else {
                        tintImage(binding.setPassword)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
