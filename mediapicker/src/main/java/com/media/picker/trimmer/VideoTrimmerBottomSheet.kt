package com.media.picker.trimmer

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.arthenica.ffmpegkit.FFmpegKit
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.media.picker.R
import com.media.picker.databinding.DialogTrimVideoBinding
import com.media.picker.utils.FileOperationHelper
import com.media.picker.utils.FileOperationHelper.getUriFromPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class VideoTrimmerBottomSheet(
    private val isDarkTheme: Boolean,
    private val videoPath: Uri,
    private val onTrim: (Uri) -> Unit,
    private val onFailed: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var exoplayer: ExoPlayer? = null

    private val binding: DialogTrimVideoBinding by lazy {
        val theme = ContextThemeWrapper(
            requireContext(),
            if (isDarkTheme) {
                R.style.DarkTheme
            } else {
                R.style.LightTheme
            }
        )
        DialogTrimVideoBinding.inflate(LayoutInflater.from(theme))
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dialog ->
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheetInternal: FrameLayout = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)!!
            bottomSheetInternal.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheetInternal.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheetInternal.requestLayout()

            bottomSheetDialog.behavior.maxWidth = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheetDialog.behavior.maxHeight = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetDialog.behavior.isDraggable = false
            bottomSheetDialog.behavior.isFitToContents = true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.imgPlayPause.setOnClickListener {
            if (binding.isPlaying) {
                pauseVideo()
            } else {
                playVideo()
            }
        }

        binding.txtSave.setOnClickListener {
            trimVideo()
        }

        binding.rangeSeek.addRangeChangeListener {
            val trimPoints = binding.rangeSeek.getTrimPoints()
            val startDuration = exoplayer?.duration?.times(it)?.div(trimPoints.third) ?: 0
            exoplayer?.seekTo(startDuration)
        }

        CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            loadVideo()
        }
    }

    override fun onPause() {
        super.onPause()
        pauseVideo()
    }

    /**
     * Trim Operations
     * **/
    private fun createRatioAdapter() {
        binding.rangeSeek.setVideoPath(videoPath)
    }
    //END:: -- Crop Operations --

    /**
     * Video Player
     */
    private fun loadVideo() {
        exoplayer = ExoPlayer.Builder(requireActivity()).build()
        exoplayer?.setMediaItem(MediaItem.fromUri(videoPath))
        exoplayer?.prepare()
        binding.playerView.player = exoplayer

        exoplayer?.playingProgressListener {
            binding.rangeSeek.showVideoProgress(exoplayer?.currentPosition ?: 0, exoplayer?.duration ?: 0)
            pauseVideoIfRequired()
        }

        exoplayer?.onReadyOnce {
            createRatioAdapter()
        }
    }

    private fun seekVideoToPlayingTime() {
        val trimPoints = binding.rangeSeek.getTrimPoints()
        val startDuration = exoplayer?.duration?.times(trimPoints.first)?.div(trimPoints.third) ?: 0
        val endDuration = exoplayer?.duration?.times(trimPoints.second)?.div(trimPoints.third) ?: 0
        if ((exoplayer?.currentPosition ?: 0) < startDuration || (exoplayer?.currentPosition ?: 0) >= endDuration)
            exoplayer?.seekTo(startDuration)
    }

    private fun playVideo() {
        seekVideoToPlayingTime()
        exoplayer?.play()
        binding.isPlaying = true
    }

    private fun pauseVideo() {
        exoplayer?.pause()
        binding.isPlaying = false
    }

    private fun pauseVideoIfRequired() {
        val trimPoints = binding.rangeSeek.getTrimPoints()
        val endDuration = exoplayer?.duration?.times(trimPoints.second)?.div(trimPoints.third) ?: 0
        if ((exoplayer?.currentPosition ?: 0) >= endDuration)
            pauseVideo()
    }

    private fun ExoPlayer.playingProgressListener(time: () -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                delay(100)
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                    if (isPlaying) {
                        time()
                    }
                }
            }
        }
    }

    private fun ExoPlayer.onReadyOnce(action: () -> Unit) {
        var isCalled = false
        this.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY && !isCalled) {
                    isCalled = true
                    action()
                }
            }
        })
    }

    /**
     * FFMPEG operation
     * **/
    private fun trimVideo() {
        try {
            requireActivity().runOnUiThread {
                binding.isLoading = true
            }
            val trimPoints = binding.rangeSeek.getTrimPoints()
            val startDuration = exoplayer?.duration?.times(trimPoints.first)?.div(trimPoints.third) ?: 0
            val endDuration = exoplayer?.duration?.times(trimPoints.second)?.div(trimPoints.third) ?: 0
            if (endDuration > 1) {
//                    val storageLocation = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath, context?.getString(R.string.app_name).orEmpty())
                val storageLocation = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath, context?.getString(R.string.app_name).orEmpty())
                storageLocation.mkdirs()
                val trimmedPath = File(storageLocation.absolutePath, "trimmed_${System.currentTimeMillis()}.mp4")

                val f = arrayOf(
                    "-ss",
                    "${startDuration}ms",
                    "-i",
                    FileOperationHelper.getPath(requireContext(), videoPath),
                    "-to",
                    "${endDuration}ms",
                    "-c",
                    "copy",
                    "-preset",
                    "ultrafast",
                    trimmedPath.absolutePath
                )
                CoroutineScope(Dispatchers.IO).launch {
                    FFmpegKit.executeWithArgumentsAsync(f, { session ->
                        if (session?.returnCode?.isValueSuccess == true) {
                            //Conversion is done
                            requireActivity().runOnUiThread {
                                binding.isLoading = false
                                trimComplete(trimmedPath.absolutePath)
                            }
                        }
                    }, null, { static ->
                        Log.e("TAG", "trimVideo: $static")
                    })
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.isLoading = false
            trimFailed(e.message.orEmpty())
        }
    }
    //END:: -- FFMPEG operation --

    private fun trimComplete(path: String) {
        onTrim(getUriFromPath(requireContext(), path))
        dismissAllowingStateLoss()
    }

    private fun trimFailed(message: String) {
        onFailed(message)
    }
}