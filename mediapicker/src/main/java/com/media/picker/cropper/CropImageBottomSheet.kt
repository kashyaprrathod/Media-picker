package com.media.picker.cropper

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.media.picker.R
import com.media.picker.cropper.helper.util.bitmap.BitmapUtils
import com.media.picker.cropper.helper.util.model.AspectRatio
import com.media.picker.databinding.DialogCropImageBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class CropImageBottomSheet(
    private val isDarkTheme: Boolean,
    private val imagePath: Uri,
    private val arlRatio: ArrayList<AspectRatio>,
    private val onCrop: (Uri) -> Unit,
    private val onFailed: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var adapter: AspectRatioAdapter? = null

    private val binding: DialogCropImageBinding by lazy {
        val theme = ContextThemeWrapper(
            requireContext(),
            if (isDarkTheme) {
                R.style.DarkTheme
            } else {
                R.style.LightTheme
            }
        )
        DialogCropImageBinding.inflate(LayoutInflater.from(theme))
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

        binding.txtSave.setOnClickListener {
            lifecycleScope.launch {
                val croppedBitmap = File(requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath + "/crop" + System.currentTimeMillis() + ".jpg")
                val bitmap = binding.cropView.getCroppedData()
                if (bitmap != null) {
                    BitmapUtils.saveBitmap(bitmap, croppedBitmap) {
                        onCrop(FileProvider.getUriForFile(requireActivity(), requireActivity().packageName + ".provider", File(croppedBitmap.absolutePath)))
                        dismissAllowingStateLoss()
                    }
                } else {
                    onFailed("")
                }
            }
        }

        binding.cvCropper.post {
            loadImage()
        }

        createRatioAdapter()
    }

    /**
     * Crop Operations
     * **/
    private fun loadImage() {
        BitmapUtils.resize(imagePath, requireActivity()) {
            if (it != null) {
                requireActivity().runOnUiThread {
                    Log.e("TAG", "loadImage: ${it.width} :: ${it.height}")
                    binding.cropView.setBitmap(it)
                    lifecycleScope.launch {
                        delay(500)
                        binding.cropView.setAspectRatio(AspectRatio.ASPECT_INS_1_1)
                        adapter?.selectAspectRatio(AspectRatio.ASPECT_INS_1_1)
                    }
                }
            }
        }
    }
    //END:: -- Crop Operations --

    /**
     * Aspect ratio operation
     */
    private fun createRatioAdapter() {
        adapter = AspectRatioAdapter(arlRatio) {
            binding.cropView.setAspectRatio(it)
        }
        binding.rvRatio.adapter = adapter
    }
}