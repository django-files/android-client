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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
import com.djangofiles.djangofiles.ServerApi.FileEditRequest
import com.djangofiles.djangofiles.copyToClipboard
import com.djangofiles.djangofiles.databinding.FragmentFilesBottomBinding
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
        val fileId = arguments?.getInt("fileId")
        Log.d("Bottom[onCreateView]", "fileId: $fileId")
        val fileName = arguments?.getString("fileName")
        Log.d("Bottom[onCreateView]", "fileName: $fileName")
        val mimeType = arguments?.getString("mimeType")
        Log.d("Bottom[onCreateView]", "mimeType: $mimeType")
        val thumbUrl = arguments?.getString("thumbUrl")
        Log.d("Bottom[onCreateView]", "thumbUrl: $thumbUrl")
        val shareUrl = arguments?.getString("shareUrl")
        Log.d("Bottom[onCreateView]", "shareUrl: $shareUrl")
        filePassword = arguments?.getString("filePassword") ?: ""
        Log.d("Bottom[onCreateView]", "filePassword: $filePassword")
        var isPrivate = requireArguments().getBoolean("isPrivate")
        Log.d("Bottom[onCreateView]", "isPrivate: $isPrivate")

        // Name
        binding.fileName.text = fileName
        // Share
        binding.shareButton.setOnClickListener {
            shareUrl(requireContext(), shareUrl!!)
        }
        // Open
        binding.openButton.setOnClickListener {
            openUrl(requireContext(), shareUrl!!)
        }
        // Copy
        binding.copyButton.setOnClickListener {
            copyToClipboard(requireContext(), shareUrl!!)
        }
        // Delete
        binding.deleteButton.setOnClickListener {
            Log.d("deleteButton", "fileId: $fileId")
            deleteConfirmDialog(savedUrl, fileId!!, fileName!!)
        }
        // Private
        if (isPrivate) {
            tintImage(binding.togglePrivate)
        }
        binding.togglePrivate.setOnClickListener {
            isPrivate = !isPrivate
            Log.d("togglePrivate", "New Value: $isPrivate")
            val api = ServerApi(requireContext(), savedUrl)
            lifecycleScope.launch {
                val response = api.edit(fileId!!, FileEditRequest(private = isPrivate))
                Log.d("deleteButton", "response: $response")
                viewModel.editRequest.value = FileEditRequest(id = fileId, private = isPrivate)
                if (isPrivate) {
                    tintImage(binding.togglePrivate)
                } else {
                    binding.togglePrivate.imageTintList = null
                }
            }
        }
        // Password
        if (!filePassword.isEmpty()) {
            tintImage(binding.setPassword)
        }
        binding.setPassword.setOnClickListener {
            Log.d("setPassword", "setOnClickListener")
            setPasswordDialog(requireContext(), fileId!!, fileName!!)
        }

        // Image Preview
        val radius = requireContext().resources.getDimension(R.dimen.image_preview_large)
        binding.imagePreview.setShapeAppearanceModel(
            binding.imagePreview.shapeAppearanceModel
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, radius)
                .build()
        )

        if (isGlideMime(mimeType!!)) {
            Log.d("Bottom[onCreateView]", "isGlideMime")
            Glide.with(this)
                .load(thumbUrl)
                .into(binding.imagePreview)
        } else {
            binding.imagePreview.setImageResource(getGenericIcon(mimeType))
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
