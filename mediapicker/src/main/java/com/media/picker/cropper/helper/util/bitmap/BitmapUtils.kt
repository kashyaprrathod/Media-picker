package com.media.picker.cropper.helper.util.bitmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.media.picker.cropper.helper.util.extensions.rotateBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object BitmapUtils {

    private const val MAX_SIZE = 1024

    fun saveBitmap(croppedBitmapData: Bitmap, file: File, onComplete: (errorMessage: String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FileOutputStream(file).use { out ->
                    croppedBitmapData.compress(Bitmap.CompressFormat.PNG, 100, out)
                    onComplete("")
                }
            } catch (e: Exception) {
                onComplete(e.message.orEmpty())
            }
        }
    }

    fun resize(uri: Uri, context: Context, resized: (Bitmap?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)

            var widthTemp = options.outWidth
            var heightTemp = options.outHeight
            var scale = 1

            while (true) {
                if (widthTemp / 2 < MAX_SIZE || heightTemp / 2 < MAX_SIZE)
                    break
                widthTemp /= 2
                heightTemp /= 2
                scale *= 2
            }

            val resultOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            var resizedBitmap = BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(uri),
                null,
                resultOptions
            )

            resizedBitmap = resizedBitmap?.rotateBitmap(getOrientation(context.contentResolver.openInputStream(uri)))

            resized(resizedBitmap)
        }
    }

    private fun getOrientation(inputStream: InputStream?): Int {
        val exifInterface: ExifInterface
        var orientation = 0
        try {
            exifInterface = ExifInterface(inputStream!!)
            orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return orientation
    }
}