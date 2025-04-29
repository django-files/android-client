package com.djangofiles.djangofiles.ui.files

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.ServerApi
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
        val savedUrl = sharedPreferences.getString("saved_url", "").toString()

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

        binding.fileName.text = fileName

        binding.shareButton.setOnClickListener {
            shareUrl(requireContext(), shareUrl!!)
        }
        binding.openButton.setOnClickListener {
            openUrl(requireContext(), shareUrl!!)
        }
        binding.copyButton.setOnClickListener {
            //val message = requireContext().getString(R.string.tst_copied_clipboard)
            //Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            copyToClipboard(requireContext(), shareUrl!!)
        }
        binding.deleteButton.setOnClickListener {
            Log.d("Bottom[onCreateView]", "deleteById: $fileId")
            deleteConfirmDialog(savedUrl, fileId!!, fileName!!)
        }

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

    private fun deleteConfirmDialog(savedUrl: String, fileId: Int,fileName: String) {
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
                        Toast.makeText(requireContext(), "Ralf Broke It!", Toast.LENGTH_SHORT)
                            .show()
                    }
                    dismiss()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
