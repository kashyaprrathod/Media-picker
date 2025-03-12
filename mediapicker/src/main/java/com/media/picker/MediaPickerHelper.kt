package com.media.picker

import androidx.appcompat.app.AppCompatActivity
import com.media.picker.builders.MediaBuilderBase
import com.media.picker.builders.VideoMediaBuilder


class MediaPickerHelper(private val activity: AppCompatActivity?) {
    private var isDarkTheme: Boolean = false
    private var builder: MediaBuilderBase? = null

    companion object {
        fun createMediaPicker(activity: AppCompatActivity?): MediaPickerHelper {
            return MediaPickerHelper(activity)
        }
    }

    fun addBuilder(builder: MediaBuilderBase): MediaPickerHelper {
        this.builder = builder
        return this
    }

    fun makeDarkTheme(): MediaPickerHelper {
        isDarkTheme = true
        return this
    }

    fun pick() {
        val fragment = MediaPicker(builder, isDarkTheme)
        try {
            activity?.supportFragmentManager?.beginTransaction()
                ?.add(fragment, "picker")
                ?.commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}