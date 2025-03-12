package com.media.picker.utils

import android.app.Activity
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileOperationHelper {

    @Throws(IOException::class)
    fun createImageFile(activity: Activity?): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"

        val storageDir = File(activity?.filesDir, "images")
        storageDir.mkdirs()

        return File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir  /* directory */
        )
    }

    @Throws(IOException::class)
    fun createVideoFile(activity: Activity?): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())
        val imageFileName = "MP4_" + timeStamp + "_"

        val storageDir = File(activity?.filesDir, "videos")
        storageDir.mkdirs()

        return File.createTempFile(
            imageFileName, /* prefix */
            ".mp4", /* suffix */
            storageDir  /* directory */
        )
    }

    fun getFile(context: Context, uri: Uri?): File? {
        if (uri != null) {
            val file = getPath(context, uri)
            if (file != null && isLocal(file)) {
                return File(file)
            }
        }
        return null
    }

    fun getPath(context: Context, uri: Uri): String? {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isLocalStorageDocument(uri)) {
                return DocumentsContract.getDocumentId(uri)
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                if (id != null && id.startsWith("raw:")) {
                    return id.substring(4)
                }
                val contentUriPrefixesToTry =
                    arrayOf(
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads"
                    )
                for (contentUriPrefix in contentUriPrefixesToTry) {
                    try {
                        val path = getFileFromUri(uri, context)?.absolutePath
                        if (path != null) {
                            return path
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val fileName = getFileName(context, uri)
                val cacheDir = getDocumentCacheDir(context)
                val file = generateFileName(fileName, cacheDir)
                var destinationPath: String? = null
                if (file != null) {
                    destinationPath = file.absolutePath
                    saveFileFromUri(context, uri, destinationPath)
                }
                return destinationPath
            } else if (isMediaDocument(uri)) {
                return getFileFromUri(uri, context)?.absolutePath
            }
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {
            if (isGooglePhotosUri(uri)) {
                return uri.lastPathSegment
            }
            return getFileFromUri(uri, context)?.absolutePath
        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    fun getUriFromPath(context: Context, path: String): Uri {
        return FileProvider.getUriForFile(context, context.packageName + ".provider", File(path))
    }

    private fun saveFileFromUri(
        context: Context,
        uri: Uri,
        destinationPath: String
    ) {
        var inputStream: InputStream? = null
        var bos: BufferedOutputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
            bos = BufferedOutputStream(FileOutputStream(destinationPath, false))
            val buf = ByteArray(1024)
            inputStream!!.read(buf)
            do {
                bos.write(buf)
            } while (inputStream.read(buf) != -1)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
                bos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun generateFileName(n: String?, directory: File): File? {
        var name: String? = n ?: return null

        var file = File(directory, name!!)

        if (file.exists()) {
            var fileName: String = name
            var extension = ""
            val dotIndex = name.lastIndexOf('.')
            if (dotIndex > 0) {
                fileName = name.substring(0, dotIndex)
                extension = name.substring(dotIndex)
            }

            var index = 0

            while (file.exists()) {
                index++
                name = "$fileName($index)$extension"
                file = File(directory, name)
            }
        }

        try {
            if (!file.createNewFile()) {
                return null
            }
        } catch (e: IOException) {

            return null
        }
        return file
    }

    private fun getDocumentCacheDir(context: Context): File {

        val baseDir: String? = context.filesDir?.absolutePath

        val storageDir = File(
            baseDir,
            context.packageName.substringAfter("com.").replace(".", "-")
        )
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return storageDir
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        val mimeType = context.contentResolver.getType(uri)
        var filename: String? = null

        if (mimeType == null) {
            val path = getPath(context, uri)
            filename = if (path == null) {
                getName(uri.toString())
            } else {
                val file = File(path)
                file.name
            }
        } else {
            val returnCursor =
                context.contentResolver.query(uri, null, null, null, null)
            if (returnCursor != null) {
                val nameIndex =
                    returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                returnCursor.moveToFirst()
                filename = returnCursor.getString(nameIndex)
                returnCursor.close()
            }
        }

        return filename
    }

    private fun getName(filename: String?): String? {
        if (filename == null) {
            return null
        }
        val index = filename.lastIndexOf('/')
        return filename.substring(index + 1)
    }

    private fun getFileFromUri(uri: Uri?, context: Context): File? {
        kotlin.runCatching {
            val pfd = context.contentResolver?.openFileDescriptor(uri!!, "r")
            val inputStream = FileInputStream(pfd?.fileDescriptor)

            val displayName = getDataColumn(context, uri, null, null)
            val copedFile = getImageFile(context, getExtension(displayName)!!)

            val out = FileOutputStream(copedFile)

            val buffer = ByteArray(1024)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            out.close()
            inputStream.close()
            pfd?.close()

            return copedFile
        }.getOrElse {
            return null
        }
    }

    private fun getImageFile(context: Context, extension: String): File {
        val baseDir = context.filesDir?.absolutePath
        val storageDir = File(
            baseDir,
            "${
                context.packageName.substringAfter("com.").replace(".", "-")
            }/${"DOC_" + System.currentTimeMillis() + "_${extension}"}"
        )
        storageDir.parentFile?.mkdirs()
        return storageDir
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {

        var cursor: Cursor? = null
        val column = OpenableColumns.DISPLAY_NAME
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(
                uri!!,
                projection,
                selection,
                selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun getExtension(uri: String?): String? {
        if (uri == null) {
            return null
        }

        val dot = uri.lastIndexOf(".")
        return if (dot >= 0) {
            uri.substring(dot)
        } else {
            // No extension.
            ""
        }
    }

    fun compressImage(filePath: String?, fileName: String): String {
        var scaledBitmap: Bitmap? = null
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        val file = File(filePath!!)
        try {
            var inputStream: InputStream? = null
            inputStream = FileInputStream(file)
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        var actualHeight = options.outHeight
        var actualWidth = options.outWidth


        val maxHeight = 1280.0f
        val maxWidth = 720.0f
        var imgRatio = actualWidth.toFloat() / actualHeight
        val maxRatio = maxWidth / maxHeight

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            when {
                imgRatio < maxRatio -> {
                    imgRatio = maxHeight / actualHeight
                    actualWidth = (imgRatio * actualWidth).toInt()
                    actualHeight = maxHeight.toInt()
                }

                imgRatio > maxRatio -> {
                    imgRatio = maxWidth / actualWidth
                    actualHeight = (imgRatio * actualHeight).toInt()
                    actualWidth = maxWidth.toInt()
                }

                else -> {
                    actualHeight = maxHeight.toInt()
                    actualWidth = maxWidth.toInt()
                }
            }
        }

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight)
        options.inJustDecodeBounds = false
        options.inPurgeable = true
        options.inInputShareable = true
        options.inTempStorage = ByteArray(16 * 1024)

        try {
            scaledBitmap = BitmapFactory.decodeFile(filePath, options)
            val bitmap = Bitmap.createBitmap(
                scaledBitmap,
                0,
                0,
                scaledBitmap.width,
                scaledBitmap.height,
                getMatrix(file),
                true
            )
            if (bitmap != scaledBitmap) scaledBitmap.recycle()
            scaledBitmap = bitmap
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(fileName)
            scaledBitmap!!.compress(Bitmap.CompressFormat.JPEG, 99, out)
            scaledBitmap.recycle()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return fileName
    }

    private fun getMatrix(f: File): Matrix {
        val mat = Matrix()
        try {
            val exif = ExifInterface(f.path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION, -1
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_180 -> mat.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_90 -> mat.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_270 -> mat.postRotate(270f)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return mat
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())
            inSampleSize =
                if (heightRatio < widthRatio) heightRatio else widthRatio
        }
        val totalPixels = (width * height).toFloat()
        val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++
        }
        return inSampleSize
    }


    private fun isLocal(url: String?): Boolean {
        return url != null && !url.startsWith("http://") && !url.startsWith("https://")
    }

    private fun isLocalStorageDocument(uri: Uri): Boolean {
        return "" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }

    fun getVideoDuration(filePath: String, onSuccess: (Long?) -> Unit) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLong() ?: 0
            retriever.release()
            onSuccess.invoke(duration)
        } catch (e: Exception) {
            e.printStackTrace()
            onSuccess(null)
        }
    }

    //Get dimensions of image/video
    fun getMediaDimension(filePath: String, isImage: Boolean): Pair<Int, Int>? {
        if (!File(filePath).exists()) return null
        return try {
            if (isImage) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(filePath, options)
                Pair(options.outWidth, options.outHeight)
            } else {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)
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

    //Get dimensions of image/video
    fun getMediaDimension(activity: FragmentActivity, filePath: Uri, isImage: Boolean): Pair<Int, Int>? {
        if (activity.isMediaExist(filePath).not()) return null
        return try {
            if (isImage) {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFileDescriptor(activity.contentResolver.openFileDescriptor(filePath, "r")?.fileDescriptor)
                Pair(options.outWidth, options.outHeight)
            } else {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(activity.contentResolver.openFileDescriptor(filePath, "r")?.fileDescriptor)
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
}