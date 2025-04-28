package com.djangofiles.djangofiles.ui.files

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.djangofiles.djangofiles.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily

// TODO: This is currently not used and will be refactored...
class FilesBottomSheet : BottomSheetDialogFragment() {

    private var imageUrl: String? = null

    companion object {
        fun newInstance(imageUrl: String) = FilesBottomSheet().apply {
            arguments = Bundle().apply {
                putString("imageUrl", imageUrl)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageUrl = arguments?.getString("imageUrl")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView =
            inflater.inflate(R.layout.fragment_files_bottom, container, false)

        Log.d("FilesBottomSheet", "onCreateView: $imageUrl")

        val imageView: ShapeableImageView = rootView.findViewById(R.id.image_preview)

        val radius = requireContext().resources.getDimension(R.dimen.image_preview_large)
        imageView.setShapeAppearanceModel(
            imageView.shapeAppearanceModel
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, radius)
                .build()
        )

        Glide.with(this)
            .load(imageUrl)
            .into(imageView)

        return rootView
    }
}
