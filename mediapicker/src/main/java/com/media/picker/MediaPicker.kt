package com.media.picker

import android.app.Activity
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.media.picker.builders.AudioMediaBuilder
import com.media.picker.builders.ImageMediaBuilder
import com.media.picker.builders.MediaBuilderBase
import com.media.picker.builders.VideoMediaBuilder
import com.media.picker.cropper.CropImageBottomSheet
import com.media.picker.cropper.helper.util.model.AspectRatio
import com.media.picker.model.MediaBean
import com.media.picker.trimmer.VideoTrimmerBottomSheet
import com.media.picker.utils.FileOperationHelper
import com.media.picker.utils.getMediaDimension
import com.media.picker.utils.getVideoDuration
import com.media.picker.utils.isAudioFile
import java.io.File
import java.io.IOException

class MediaPicker(
    private val mediaBuilder: MediaBuilderBase?,
    private val darkTheme: Boolean = false,
) : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val theme = ContextThemeWrapper(
            requireContext(),
            if (darkTheme) {
                R.style.LightTheme
            } else {
                R.style.DarkTheme
            }
        )
        when (mediaBuilder) {
            is ImageMediaBuilder -> {
                if (mediaBuilder.capture)
                    captureImageFromCamera()
                else
                    pickImage()
            }

            is VideoMediaBuilder -> {
                if (mediaBuilder.capture)
                    recordVideo()
                else
                    pickVideo()
            }

            is AudioMediaBuilder -> {
                pickAudio()
            }
        }
        return super.onCreateView(LayoutInflater.from(theme), container, savedInstanceState)
    }

    /***
     *
     * Image Operations
     * **/
    /**
     * Capture Image
     * **/
    private var captureImageUri: Uri? = null

    private fun captureImageFromCamera() {
        var photoURI: Uri? = null
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            var photoFile: File? = null
            try {
                photoFile = FileOperationHelper.createImageFile(activity)

                photoURI = FileProvider.getUriForFile(
                    requireActivity(),
                    requireActivity().packageName + ".provider",
                    photoFile
                )

                captureImageUri = photoURI

            } catch (ex: IOException) {
                ex.printStackTrace()
            }

            if (photoFile != null) {
                takePictureIntent.putExtra(
                    MediaStore.EXTRA_OUTPUT,
                    photoURI
                )

                //Grant uri permission for all packages

                requireActivity().grantUriPermission(
                    requireActivity().packageName,
                    photoURI,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                capturePhotoIntent.launch(takePictureIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val capturePhotoIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        Log.e("TAG", "registerForActivityResult :::: ")
        if (it.resultCode == Activity.RESULT_OK && captureImageUri != null) {
            Log.e("TAG", "it.resultCode == Activity.RESULT_OK :::: ")
            if ((mediaBuilder as ImageMediaBuilder).canCropImage()) {
                cropImage(captureImageUri ?: Uri.parse(""))
            } else
                try {
                    MediaScannerConnection.scanFile(
                        activity,
                        arrayOf(FileOperationHelper.getPath(requireContext(), captureImageUri ?: Uri.parse(""))),
                        null,
                        null
                    )
                    sendPickedMedia(captureImageUri, false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
        } else {
//            onError.onError("Error code :- ${it.resultCode}")
        }
    }
    //END:: -- Capture Image --

    /**
     * Pick Image
     * **/
    private fun pickImage() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(requireActivity())) {
            if (mediaBuilder?.canPickMultiple() == true)
                pickPhotoForTiramisuMultiple.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            else
                pickPhotoForTiramisu.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

        } else {
            openGalleryForPickImage()
        }
    }

    private val pickPhotoForTiramisu = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            if ((mediaBuilder as ImageMediaBuilder).canCropImage()) {
                cropImage(uri)
            } else {
                try {
                    sendPickedMedia(uri, false)
                } catch (e: Exception) {
                    e.printStackTrace()
                    mediaBuilder.getPickFailedListener()?.invoke("Failed")
                }
            }
        } else {
            Log.e("ImagePicker", "No media selected")
        }
    }

    private val pickPhotoForTiramisuMultiple = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(if (mediaBuilder?.canPickMultiple() == true) mediaBuilder.getSelectionCounts() else 2)) { uris ->
        if (uris.isNotEmpty()) {
            try {
                sendPickedMedia(uris, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.e("ImagePicker", "No media selected")
        }
    }

    private fun openGalleryForPickImage() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ).apply {
            type = "image/*"
            if (mediaBuilder?.canPickMultiple() == true) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }

        pickPhotoIntent.launch(galleryIntent)
    }

    private val pickPhotoIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        /**
        # If there is multiple pickers enable then you need to get data form #getClipData()

        if (pickerOptions.maxMediaPick > 2) {
        val result = it.data?.clipData
        } else {
        val result = it.data?.data
        }*/
        if (it.data?.data != null) {
            if ((mediaBuilder as ImageMediaBuilder).canCropImage()) {
                cropImage(it.data?.data!!)
            } else {
                try {
                    sendPickedMedia(it.data?.data, false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    //END:: -- Pick Image --

    private fun cropImage(path: Uri) {
        CropImageBottomSheet(
            darkTheme,
            path,
            arrayListOf(
                AspectRatio.ASPECT_INS_1_1,
                AspectRatio.ASPECT_INS_4_5,
                AspectRatio.ASPECT_16_9,
                AspectRatio.ASPECT_9_16
            ), {
                sendPickedMedia(it, false)
            }, {
            }
        ).show(requireActivity().supportFragmentManager, "crop")
    }
    //END:: ------------ Image Operations ------------

    /***
     *
     * Video Operations
     * **/
    /**
     * Record Video
     * **/
    private var captureVideoUri: Uri? = null

    private fun recordVideo() {
        var videoURI: Uri? = null
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        try {
            var videoFile: File? = null
            try {
                videoFile = FileOperationHelper.createVideoFile(activity)
                videoURI = FileProvider.getUriForFile(requireActivity(), requireActivity().packageName + ".provider", videoFile)
                captureVideoUri = videoURI
            } catch (ex: IOException) {
                ex.printStackTrace()
            }

            if (videoFile != null) {
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI)

                requireActivity().grantUriPermission(requireActivity().packageName, videoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

                if ((mediaBuilder as VideoMediaBuilder).getMaxVideoDurationInMillis() > 0) {
                    takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, mediaBuilder.getMaxVideoDurationInSecond().toInt())
                }
                captureVideoIntent.launch(takeVideoIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val captureVideoIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK && captureVideoUri != null) {
            try {
                val duration = requireActivity().getVideoDuration(captureVideoUri)
                if (duration != null) {
                    val videoBuilder = mediaBuilder as VideoMediaBuilder
                    if (videoBuilder.isTrimEnabled()) {
                        trimVideo(captureVideoUri ?: Uri.EMPTY)
                    } else {
                        sendPickedMedia(captureVideoUri, true)
                    }
                } else {
                    //Failed:::
                    mediaBuilder?.getPickFailedListener()?.invoke("Failed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mediaBuilder?.getPickFailedListener()?.invoke("Failed")
            }
        } else {
//            onError.onError("Error code :- ${it.resultCode}")
        }
    }
    //END:: -- Record Video --

    /**
     * Pick Video
     * **/
    private fun pickVideo() {
        if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(requireActivity())) {
            if (mediaBuilder?.canPickMultiple() == true)
                pickVideoForTiramisuMultiple.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.VideoOnly
                    )
                )
            else
                pickVideoForTiramisu.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.VideoOnly
                    )
                )

        } else {
            openGalleryForVideoImage()
        }
    }

    private val pickVideoForTiramisuMultiple = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(if (mediaBuilder?.canPickMultiple() == true) mediaBuilder.getSelectionCounts() else 2)) { uris ->
        if (uris.isNotEmpty()) {
            try {
                sendPickedMedia(uris, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.e("ImagePicker", "No media selected")
        }
    }

    private val pickVideoForTiramisu = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            try {
                val duration = requireActivity().getVideoDuration(uri)
                Log.e("TAG", ":video duration ::: $duration")
                if (duration != null) {
                    val videoBuilder = mediaBuilder as VideoMediaBuilder
                    if (videoBuilder.isTrimEnabled() || videoBuilder.getMaxVideoDurationInMillis() < duration) {
                        trimVideo(uri)
                    } else {
                        sendPickedMedia(uri, true)
                    }
                } else {
                    //Failed:::
                    mediaBuilder?.getPickFailedListener()?.invoke("Failed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mediaBuilder?.getPickFailedListener()?.invoke("Failed")
            }
        } else {
            Log.e("ImagePicker", "No media selected")
        }
    }

    private fun openGalleryForVideoImage() {
        val galleryIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).apply {
            type = "video/*"
            if (mediaBuilder?.canPickMultiple() == true) {
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
        pickVideoIntent.launch(galleryIntent)
    }

    private val pickVideoIntent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.data?.data != null) {
            try {
                val duration = requireActivity().getVideoDuration(captureVideoUri)
                if (duration != null) {
                    val videoBuilder = mediaBuilder as VideoMediaBuilder
                    if (videoBuilder.isTrimEnabled()) {
                        trimVideo(captureVideoUri ?: Uri.EMPTY)
                    } else {
                        sendPickedMedia(captureVideoUri, true)
                    }
                } else {
                    //Failed:::
                    mediaBuilder?.getPickFailedListener()?.invoke("Failed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mediaBuilder?.getPickFailedListener()?.invoke("Failed")
            }
        }
    }
    //END:: -- Pick Video --

    private fun trimVideo(path: Uri) {
        VideoTrimmerBottomSheet(
            darkTheme,
            path,
            {
                sendPickedMedia(it, true)
            }, {

            }
        ).show(requireActivity().supportFragmentManager, "crop")
    }
    //END:: ------------ Video Operations ------------

    /***
     * Audio Operations
     * **/
    private fun pickAudio() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
        }
        if (mediaBuilder?.canPickMultiple() == true) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Allow multiple selection
        }
        audioPickerLauncher.launch(intent)
    }

    private val audioPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                data.clipData?.let { clipData ->
                    val uriList = ArrayList<Uri>()
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        if (requireActivity().isAudioFile(uri)) {
                            uriList.add(uri)
                        }
                    }
                    if (uriList.isEmpty()) {
                        sendPickedAudioMedia(uriList)
                    } else {
                        sendPickedAudioMedia(uriList)
                    }
                } ?: data.data?.let { singleUri ->
                    requireActivity().contentResolver.takePersistableUriPermission(singleUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    sendPickedAudioMedia(singleUri)
                }
            }
        }
    }
    // END:: ------------ Audio Operations ------------

    private fun sendPickedMedia(path: Uri?, isVideo: Boolean) {
        val media = MediaBean()
        media.path = path
        media.isVideo = isVideo
        val size = requireActivity().getMediaDimension(path, isVideo.not())
        media.width = size?.first ?: 1
        media.height = size?.second ?: 1
        mediaBuilder?.getPickSuccessListener()?.invoke(media)
    }

    private fun sendPickedMedia(arl: List<Uri>, isVideo: Boolean) {
        val arlMedia = ArrayList<MediaBean>()
        arl.forEach {
            val media = MediaBean()
            media.path = it
            media.isVideo = isVideo
            val size = requireActivity().getMediaDimension(it, isVideo.not())
            media.width = size?.first ?: 1
            media.height = size?.second ?: 1
            arlMedia.add(media)
        }
        mediaBuilder?.getMultiplePickSuccessListener()?.invoke(arlMedia)
    }

    private fun sendPickedAudioMedia(path: Uri) {
        val media = MediaBean()
        media.path = path
        media.isAudio = true
        media.duration = requireActivity().getVideoDuration(path) ?: 0
        mediaBuilder?.getPickSuccessListener()?.invoke(media)
    }

    private fun sendPickedAudioMedia(arl: ArrayList<Uri>) {
        val arlMedia = ArrayList<MediaBean>()
        arl.forEach {
            val media = MediaBean()
            media.path = it
            media.isAudio = true
            media.duration = requireActivity().getVideoDuration(it) ?: 0
        }
        mediaBuilder?.getMultiplePickSuccessListener()?.invoke(arlMedia)
    }
}