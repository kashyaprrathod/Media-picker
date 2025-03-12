package com.kashyap.mediapicker

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.kashyap.mediapicker.databinding.ActivityMainBinding
import com.media.picker.MediaPickerHelper
import com.media.picker.builders.AudioMediaBuilder
import com.media.picker.builders.ImageMediaBuilder
import com.media.picker.builders.VideoMediaBuilder
import com.media.picker.utils.getVideoDuration


class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding?.root)

        binding?.btnPick?.setOnClickListener {
            MediaPickerHelper.createMediaPicker(this)
                .addBuilder(
                    ImageMediaBuilder()
                        .cropImage()
                        .pickImage()
                        .setSelectionCounts(5)
                        .pickMultipleSuccessStatusListener {
                        }
                        .pickSuccessStatusListener {
                            val bitmap = BitmapFactory.decodeFileDescriptor(contentResolver.openFileDescriptor(it.path ?: Uri.EMPTY, "r")?.fileDescriptor)
                            Log.e("TAG", "onCreate: pickSuccessStatusListener: $it ")
                            val path = it.path
                        }
                        .pickFailStatusListener {

                        })
                .makeDarkTheme()
                .pick()
        }
        binding?.btnCapture?.setOnClickListener {
            MediaPickerHelper.createMediaPicker(this)
                .addBuilder(
                    ImageMediaBuilder()
                        .cropImage()
                        .captureImage()
                        .pickSuccessStatusListener {
                            val uri = it.path
                            val bitmap = BitmapFactory.decodeFileDescriptor(contentResolver.openFileDescriptor(it.path ?: Uri.EMPTY, "r")?.fileDescriptor)
                        }.pickFailStatusListener {

                        })
                .pick()
        }
        binding?.btnPickVideo?.setOnClickListener {
            MediaPickerHelper.createMediaPicker(this)
                .addBuilder(
                    VideoMediaBuilder()
                        .pickVideo()
                        .setMaxVideoDurationInSeconds(60)
                        .pickSuccessStatusListener {
                            Log.e("TAG", "onCreate: pickSuccessStatusListener ${this.getVideoDuration(it.path)}")
                        }.pickFailStatusListener {
                            Log.e("TAG", "onCreate: pickFailStatusListener")
                        })
                .makeDarkTheme()
                .pick()
        }
        binding?.btnCaptureVideo?.setOnClickListener {
            Log.e("TAG", "onCreate: ")
            MediaPickerHelper.createMediaPicker(this)
                .addBuilder(
                    VideoMediaBuilder()
                        .recordVideo()
                        .setMaxVideoDurationInSeconds(10)
                        .pickSuccessStatusListener {

                        }.pickFailStatusListener {

                        })
                .pick()
        }

        binding?.btnPickAudio?.setOnClickListener {
            Log.e("TAG", "onCreate: ")
            MediaPickerHelper.createMediaPicker(this)
                .addBuilder(
                    AudioMediaBuilder()
                        .setSelectionCounts(2)
                        .pickMultipleSuccessStatusListener {
                            Log.e("TAG", "onCreate: pickMultipleSuccessStatusListener : $it")
                        }
                        .pickSuccessStatusListener {
                            Log.e("TAG", "onCreate: pickSuccessStatusListener: $it")
                        }.pickFailStatusListener {

                        })
                .pick()
        }
    }
}