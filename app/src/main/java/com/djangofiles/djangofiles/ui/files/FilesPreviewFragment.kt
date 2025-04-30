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
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.djangofiles.djangofiles.MediaCache
import com.djangofiles.djangofiles.databinding.FragmentFilesPreviewBinding

//import android.view.Gravity
//import androidx.transition.Slide
//import androidx.navigation.fragment.navArgs

class FilesPreviewFragment : Fragment() {

    //private val args: FilesPreviewFragmentArgs by navArgs()

    private var _binding: FragmentFilesPreviewBinding? = null
    private val binding get() = _binding!!

    private var isPlaying: Boolean? = null
    private var currentPosition: Long = 0

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
            Log.d("FilesPreviewFragment", "EXOPLAYER")
            binding.playerView.visibility = View.VISIBLE

            player = ExoPlayer.Builder(requireContext()).build()
            binding.playerView.player = player
            binding.playerView.controllerShowTimeoutMs = 1000
            binding.playerView.setShowNextButton(false)
            binding.playerView.setShowPreviousButton(false)

            //player.addListener(object : Player.Listener {
            //    override fun onIsPlayingChanged(isPlaying: Boolean) {
            //        if (isPlaying) {
            //            binding.playerView.hideController()
            //        } else {
            //            binding.playerView.showController()
            //        }
            //    }
            //})
            if (savedInstanceState != null) {
                isPlaying = savedInstanceState.getBoolean("is_playing", false)
                currentPosition = savedInstanceState.getLong("current_position", 0L)
            }
            Log.d("FilesPreviewFragment", "isPlaying: $isPlaying")
            Log.d("FilesPreviewFragment", "currentPosition: $currentPosition")

            val mediaItem = MediaItem.fromUri(viewUrl!!)
            val mediaSource = ProgressiveMediaSource.Factory(MediaCache.cacheDataSourceFactory)
                .createMediaSource(mediaItem)
            player.setMediaSource(mediaSource)
            player.prepare()
            player.seekTo(currentPosition)
            if (isPlaying ?: autoPlay) {
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
            Log.d("FilesPreviewFragment", "GLIDE")
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
            Log.d("FilesPreviewFragment", "OTHER - NO PREVIEW")

            binding.previewImageView.visibility = View.VISIBLE
            binding.previewImageView.setImageResource(getGenericIcon(mimeType.toString()))
            binding.previewImageView.setOnClickListener {
                //findNavController().popBackStack()
                findNavController().navigateUp()
            }
        }
    }

    //override fun onPause() {
    //    Log.d("FilesPreviewFragment", "ON PAUSE")
    //    super.onPause()
    //}

    override fun onStop() {
        Log.d("Files[onStop]", "1 - ON STOP")
        super.onStop()
        if (::player.isInitialized) {
            Log.d("Files[onStop]", "player.isPlaying: ${player.isPlaying}")
            isPlaying = player.isPlaying
            player.pause()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("Files[onSave]", "2 - ON SAVE")
        super.onSaveInstanceState(outState)
        if (::player.isInitialized) {
            Log.d("Files[onSave]", "isPlaying: $isPlaying")
            if (isPlaying != null) {
                outState.putBoolean("is_playing", isPlaying!!)
            }
            Log.d("Files[onSave]", "player.currentPosition: ${player.currentPosition}")
            outState.putLong("current_position", player.currentPosition)
        }
    }

    override fun onDestroyView() {
        Log.d("Files[onDestroy]", "3 - ON DESTROY")
        super.onDestroyView()
        if (::player.isInitialized) {
            player.release()
        }
    }

    //override fun onStart() {
    //    Log.d("FilesPreviewFragment", "ON START")
    //    super.onStart()
    //}

    //override fun onResume() {
    //    Log.d("FilesPreviewFragment", "ON RESUME")
    //    super.onResume()
    //}
}
