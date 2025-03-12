package com.media.picker.utils

import android.app.Activity
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.net.Uri
import android.util.Log
import androidx.fragment.app.FragmentActivity
import java.io.File

fun FragmentActivity.isMediaExist(filePath: Uri?): Boolean {
    return try {
        contentResolver.openInputStream(filePath ?: Uri.parse(""))?.use { true } ?: false
    } catch (e: Exception) {
        return false
    }
}

fun FragmentActivity.getMediaDimension(filePath: Uri?, isImage: Boolean): Pair<Int, Int>? {
    if (isMediaExist(filePath).not()) return null
    return try {
        if (isImage) {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFileDescriptor(contentResolver.openFileDescriptor(filePath ?: Uri.parse(""), "r")?.fileDescriptor, null, options)
            Pair(options.outWidth, options.outHeight)
        } else {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(contentResolver.openFileDescriptor(filePath ?: Uri.parse(""), "r")?.fileDescriptor)
            val width = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: return null
            val height = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: return null
            val rotation = retriever.extractMetadata(METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: return null

            retriever.release()
            if (rotation == 90 || rotation == 270) Pair(height, width) else Pair(width, height)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Pair(1, 1)
    }
}

fun FragmentActivity.getVideoDuration(filePath: Uri?): Long? {
    Log.e("TAG", "getVideoDuration: ${isMediaExist(filePath)} :: ${filePath}")
    if (isMediaExist(filePath).not()) return null
    try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(contentResolver.openFileDescriptor(filePath ?: Uri.parse(""), "r")?.fileDescriptor)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val duration = durationStr?.toLong() ?: 0
        retriever.release()
        return duration
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun FragmentActivity.isAudioFile(uri: Uri): Boolean {
    return contentResolver?.getType(uri)?.startsWith("audio/") == true
}