package com.ealiox.app.utils.permission

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ealiox.app.R
import com.ealiox.app.databinding.DialogPermissionSettingsBinding
import com.ealiox.app.di.dialog.BaseCustomBottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kashyap.mediapicker.permission.PermissionInHand

class PermissionManager : Fragment() {

    private val arlPermission: ArrayList<Pair<String, String>> = arrayListOf()
    private val needToManageRationale = true

    private val multiplePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.containsValue(false).not()) {
            PermissionInHand.getInstance()?.onAllPermissionGrantedCallback?.invoke()
        } else {
            manageRationalDialog()
//            PermissionInHand.getInstance()?.onPermissionDeniedCallback?.invoke()
        }
    }

    private val singlePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        Log.e("TAG", "singlePermission: ")
        if (it) {
            PermissionInHand.getInstance()?.onAllPermissionGrantedCallback?.invoke()
        } else {
            manageRationalDialog()
//            PermissionInHand.getInstance()?.onPermissionDeniedCallback?.invoke()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        arlPermission.addAll(Gson().fromJson<ArrayList<Pair<String, String>>>(arguments?.getString("permission"), object : TypeToken<ArrayList<Pair<String, String>>>() {}.type))
        askPermission()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun askPermission() {
        if (arlPermission.size == 1) {
            singlePermission.launch(arlPermission.first().first)
        } else {
            multiplePermissionLauncher.launch(arlPermission.map { it.first }.toTypedArray())
        }
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireActivity(), permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun manageRationalDialog() {
        Log.e("TAG", "manageRationalDialog: $needToManageRationale")
        if (needToManageRationale.not()) {
            return
        }
        arlPermission.forEach { permission ->
            Log.e("TAG", "${permission.first} :: ${isPermissionGranted(permission.first)} :: ${shouldShowRequestPermissionRationale(permission.first)}")
            if (requireActivity().shouldShowRequestPermissionRationale(permission.first) && isPermissionGranted(permission.first).not() && permission.second.isNotEmpty()) {
                showForceSettingsDialog(permission.second)
                return@forEach
            } else {
                PermissionInHand.getInstance()?.onPermissionDeniedCallback?.invoke()
            }
        }
    }

    //Show settings dialog for force open the permission screen of app
    private fun showForceSettingsDialog(message: String) {
        if (message.isEmpty()) {
            return
        }

        var dialogGalleryRational: BaseCustomBottomSheetDialog<DialogPermissionSettingsBinding>? = null
        dialogGalleryRational = BaseCustomBottomSheetDialog(requireActivity(), R.layout.dialog_permission_settings, object : BaseCustomBottomSheetDialog.Listener {
            override fun onViewClick(view: View) {
                when (view.id) {
                    R.id.clMain -> {
                        dialogGalleryRational?.dismiss()
                    }

                    R.id.txtConfirm -> {
                        val intent = Intent()
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", requireActivity().packageName, null)
                        intent.setData(uri)
                        ContextCompat.startActivity(requireActivity(), intent, null)
                        dialogGalleryRational?.dismiss()
                    }

                    R.id.txtCancel -> {
                        dialogGalleryRational?.dismiss()
                    }
                }
            }
        })

        dialogGalleryRational.show()
        dialogGalleryRational.getBinding()?.heading = requireActivity().getString(R.string.permission_required)
        dialogGalleryRational.getBinding()?.message = message
        dialogGalleryRational.getBinding()?.buttonOK = requireActivity().getString(R.string.settings)

        if (dialogGalleryRational.window != null) {
            val lWindowParams = WindowManager.LayoutParams()
            lWindowParams.copyFrom((dialogGalleryRational.window ?: return).attributes)
            lWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT
            lWindowParams.height = WindowManager.LayoutParams.MATCH_PARENT
            (dialogGalleryRational.window ?: return).attributes = lWindowParams
        }
    }
}