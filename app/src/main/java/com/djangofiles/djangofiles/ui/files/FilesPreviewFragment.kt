package com.djangofiles.djangofiles.ui.files

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.djangofiles.djangofiles.R
import com.djangofiles.djangofiles.databinding.FragmentFilesPreviewBinding

//import android.view.Gravity
//import androidx.transition.Slide
//import androidx.navigation.fragment.navArgs

class FilesPreviewFragment : Fragment() {

    //private val args: FilesPreviewFragmentArgs by navArgs()

    private var _binding: FragmentFilesPreviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FilesPre[onCreate]", "savedInstanceState: ${savedInstanceState?.size()}")
        sharedElementEnterTransition =
            TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("FilesPre[onCreateView]", "savedInstanceState: ${savedInstanceState?.size()}")
        _binding = FragmentFilesPreviewBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FilesPre[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")

        val imageView = view.findViewById<ImageView>(R.id.preview_image_view)

        val fileId = arguments?.getInt("fileId")
        Log.d("FilesPreviewFragment", "fileId: $fileId")
        val thumbUrl = arguments?.getString("thumbUrl")
        Log.d("FilesPreviewFragment", "thumbUrl: $thumbUrl")
        val viewUrl = arguments?.getString("viewUrl")
        Log.d("FilesPreviewFragment", "viewUrl: $viewUrl")

        imageView.transitionName = fileId.toString()

        //Glide.with(this)
        //    .load(thumbUrl)
        //    .into(imageView)

        postponeEnterTransition()

        Glide.with(this)
            .load(thumbUrl)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    startPostponedEnterTransition()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    startPostponedEnterTransition()
                    return false
                }
            })
            .into(imageView)

        imageView.setOnClickListener {
            //findNavController().popBackStack()
            findNavController().navigateUp()
        }
    }
}
