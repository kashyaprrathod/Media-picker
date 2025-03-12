package com.media.picker.trimmer.helper.seekbar

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.media.picker.databinding.ViewTrimSliderBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RangeSeekbar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private var thumbCount = 8
    private var videoPath: Uri? = null
    private var videoDuration: Long = 100

    private var adapterThumb: SeekThumbAdapter? = null

    val binding: ViewTrimSliderBinding by lazy {
        ViewTrimSliderBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private var isWidthCalculated = false

    fun setVideoPath(path: Uri) {
        videoPath = path
        binding.rvThumb.viewTreeObserver.addOnGlobalLayoutListener {
            if (binding.rvThumb.width > 0 && binding.rvThumb.height > 0 && isWidthCalculated.not()) {
                isWidthCalculated = true
                val cellWidth = binding.rvThumb.width.div(thumbCount)
                adapterThumb = SeekThumbAdapter(thumbCount, cellWidth)
                binding.rvThumb.adapter = adapterThumb
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            loadVideoThumbnail()
        }
    }

    //Load video thumbnail
    private fun loadVideoThumbnail() {
        try {
            val mataData = MediaMetadataRetriever()
            mataData.setDataSource(context.contentResolver.openFileDescriptor(videoPath ?: Uri.EMPTY, "r")?.fileDescriptor)
            val stringDur = mataData.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            videoDuration = stringDur?.toLong() ?: 100
            for (i in thumbCount downTo 1) {
                val thumb = mataData.getFrameAtTime(videoDuration.div(i))
                CoroutineScope(Dispatchers.Main).launch {
                    adapterThumb?.addThumb(thumb)
                }
            }
            mataData.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showVideoProgress(progress: Long, duration: Long) {
        Log.e("TAG", "showVideoProgress: ${binding.rangeSlider.end} :: $progress :: $videoDuration")
        binding.rangeSlider.showPlayingIndicator(
            try {
                binding.rangeSlider.end.times(progress).div(duration.toFloat())
            } catch (e: Exception) {
                e.printStackTrace()
                0f
            }
        )
    }

    //Return three points
    // 1 -> start point
    // 2 -> end point
    // 3 -> max tick count of slider for calculation
    fun getTrimPoints(): Triple<Int, Int, Int> {
        return Triple(binding.rangeSlider.leftIndex, binding.rangeSlider.rightIndex, binding.rangeSlider.maxTickCount)
    }

    fun addRangeChangeListener(rangeChanged: (Int) -> Unit) {
        binding.rangeSlider.setRangeChangeListener(object : RangeSlider.OnRangeChangeListener {
            override fun onRangeChange(view: RangeSlider?, leftPinIndex: Int, rightPinIndex: Int) {

            }

            override fun onAnySlider(leftPinIndex: Int, isLeft: Boolean?) {
                rangeChanged(leftPinIndex)
            }
        })
    }
}