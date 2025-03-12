package com.media.picker.cropper.helper.util.file


data class FileOperationRequest(
    val fileName: String,
    val fileExtension: FileExtension = FileExtension.PNG
) {

    companion object {
        fun createRandom(): FileOperationRequest {
            return FileOperationRequest(
                System.currentTimeMillis().toString(),
                FileExtension.PNG
            )
        }
    }

}