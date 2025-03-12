package com.media.picker.cropper.helper.util.file

import android.content.Context
import android.os.Environment
import java.io.File


object FileCreator {

    fun createFile(fileOperationRequest: FileOperationRequest, context: Context): File {
        return createCacheFile(fileOperationRequest, context)
        /*return when (fileOperationRequest.storageType) {
            CACHE ->
            EXTERNAL -> createExternalFile(fileOperationRequest, context)
        }*/
    }

    private fun createCacheFile(
        fileOperationRequest: FileOperationRequest,
        context: Context
    ): File {
        val outputDir = context.cacheDir
        return File.createTempFile(
            "img",
            fileOperationRequest.fileName + fileOperationRequest.fileExtension.fileExtensionName,
            outputDir
        )
    }

    private fun createExternalFile(
        fileOperationRequest: FileOperationRequest,
        context: Context
    ): File {
        val path = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val parentFolder = File(path, "croppy")
            .also { it.mkdirs() }

        return File(
            parentFolder,
            "${fileOperationRequest.fileName}${fileOperationRequest.fileExtension.fileExtensionName}"
        )
    }
}