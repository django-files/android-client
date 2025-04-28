package com.djangofiles.djangofiles.ui.files

import android.content.Context.MODE_PRIVATE
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.djangofiles.djangofiles.databinding.FragmentFilesPreviewBinding

//import android.view.Gravity
//import androidx.transition.Slide
//import androidx.navigation.fragment.navArgs

class FilesPreviewFragment : Fragment() {

    //private val args: FilesPreviewFragmentArgs by navArgs()

    private var _binding: FragmentFilesPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var player: ExoPlayer

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

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FilesPre[onViewCreated]", "savedInstanceState: ${savedInstanceState?.size()}")

        binding.goBack.setOnClickListener {
            Log.d("FilesPreviewFragment", "GO BACK")
            //findNavController().popBackStack()
            findNavController().navigateUp()
        }

        val fileId = arguments?.getInt("fileId")
        Log.d("FilesPreviewFragment", "fileId: $fileId")
        val fileName = arguments?.getString("fileName")
        Log.d("FilesPreviewFragment", "fileName: $fileName")
        val mimeType = arguments?.getString("mimeType")
        Log.d("FilesPreviewFragment", "mimeType: $mimeType")
        val thumbUrl = arguments?.getString("thumbUrl")
        Log.d("FilesPreviewFragment", "thumbUrl: $thumbUrl")
        val viewUrl = arguments?.getString("viewUrl")
        Log.d("FilesPreviewFragment", "viewUrl: $viewUrl")

        binding.fileName.text = fileName

        val sharedPreferences =
            requireContext().getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val autoPlay = sharedPreferences.getBoolean("file_preview_autoplay", false)
        Log.d("FilesPreviewFragment", "autoPlay: $autoPlay")

        binding.playerView.transitionName = fileId.toString()
        //Log.d("FilesPreviewFragment", "transitionName: ${imageView.transitionName}")

        if (mimeType?.startsWith("video/") == true || mimeType?.startsWith("audio/") == true) {
            Log.d("FilesPreviewFragment", "EXOPLAYER LOAD")

            binding.playerView.visibility = View.VISIBLE
            player = ExoPlayer.Builder(requireContext()).build()
            binding.playerView.player = player
            binding.playerView.controllerShowTimeoutMs = 1000

            //player.addListener(object : Player.Listener {
            //    override fun onIsPlayingChanged(isPlaying: Boolean) {
            //        if (isPlaying) {
            //            binding.playerView.hideController()
            //        } else {
            //            binding.playerView.showController()
            //        }
            //    }
            //})

            val mediaItem = MediaItem.fromUri(viewUrl!!)
            player.setMediaItem(mediaItem)
            player.prepare()
            if (autoPlay) {
                Log.d("FilesPreviewFragment", "player.play")
                player.play()
            }

            //player.addListener(
            //    object : Player.Listener {
            //        override fun onIsPlayingChanged(isPlaying: Boolean) {
            //            if (isPlaying) {
            //                // Active playback.
            //            } else {
            //                // Not playing because playback is paused, ended, suppressed, or the player
            //                // is buffering, stopped or failed. Check player.playWhenReady,
            //                // player.playbackState, player.playbackSuppressionReason and
            //                // player.playerError for details.
            //            }
            //        }
            //    }
            //)

        } else if (isGlideMime(mimeType.toString())) {
            Log.d("FilesPreviewFragment", "GLIDE LOAD")

            binding.previewImageView.visibility = View.VISIBLE

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
                .into(binding.previewImageView)
            binding.previewImageView.setOnClickListener {
                Log.d("FilesPreviewFragment", "IMAGE BACK")
                //findNavController().popBackStack()
                findNavController().navigateUp()
            }

        } else {
            Log.d("FilesPreviewFragment", "NO PREVIEW 4 U")

            binding.previewImageView.visibility = View.VISIBLE
            binding.previewImageView.setImageResource(getGenericIcon(mimeType.toString()))
            binding.previewImageView.setOnClickListener {
                //findNavController().popBackStack()
                findNavController().navigateUp()
            }
        }
    }

    override fun onStop() {
        Log.d("FilesPreviewFragment", "onStop: player.pause")
        super.onStop()
        if (::player.isInitialized) {
            player.pause()
        }
    }

    override fun onDestroyView() {
        Log.d("FilesPreviewFragment", "onDestroyView: player.release")
        super.onDestroyView()
        if (::player.isInitialized) {
            player.release()
        }
    }
}
