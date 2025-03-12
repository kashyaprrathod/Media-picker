package com.kashyap.mediapicker.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.ealiox.app.utils.permission.PermissionManager
import com.google.gson.Gson

class PermissionInHand(private val activity: FragmentActivity) {

    companion object {
        private var permissionInHand: PermissionInHand? = null
        fun init(activity: FragmentActivity): PermissionInHand {
            permissionInHand = PermissionInHand(activity)
            return permissionInHand!!
        }

        fun getInstance(): PermissionInHand? {
            return permissionInHand
        }

        /**
         * Permissions
         * **/

        //Get gallery permission
        fun getGalleryPermission(): ArrayList<Pair<String, String>> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayListOf(
                    Pair(Manifest.permission.READ_MEDIA_IMAGES, "Photos permission needed. Go to android setting and allow photos and videos access."),
                    Pair(Manifest.permission.READ_MEDIA_VIDEO, "Photos permission needed. Go to android setting and allow photos and videos access.")
                )
            } else {
                arrayListOf(
                    Pair(Manifest.permission.READ_EXTERNAL_STORAGE, "Photos permission needed. Go to android setting and allow photos and videos access."),
                    Pair(Manifest.permission.WRITE_EXTERNAL_STORAGE, "Photos permission needed. Go to android setting and allow photos and videos access.")
                )
            }
        }

        //Get gallery permission
        fun getCameraPermission(): ArrayList<Pair<String, String>> {
            return arrayListOf(
                Pair(Manifest.permission.CAMERA, "Camera permission needed. Go to android setting and allow camera access."),
                Pair(Manifest.permission.RECORD_AUDIO, "Record audio permission needed. Go to android setting and allow record audio access.")
            )
        }

        //Get gallery permission
        fun getRecordAudioPermission(): ArrayList<Pair<String, String>> {
            return arrayListOf(
                Pair(Manifest.permission.RECORD_AUDIO, "Record audio permission needed. Go to android setting and allow record audio access.")
            )
        }

        //Get gallery permission
        fun getLocationPermissions(): ArrayList<Pair<String, String>> {
            return arrayListOf(
                Pair(Manifest.permission.ACCESS_COARSE_LOCATION, "Location permission needed. Go to android setting and allow location access."),
                Pair(Manifest.permission.ACCESS_FINE_LOCATION, "Location permission needed. Go to android setting and allow location access.")
            )
        }

        fun getNotificationPermission(): ArrayList<Pair<String, String>> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayListOf(
                    Pair(Manifest.permission.POST_NOTIFICATIONS, "Permission needed. Go to android setting and allow permission")
                )
            } else {
                arrayListOf()
            }
        }

        fun getContactPermission(): ArrayList<Pair<String, String>> {
            return arrayListOf(Pair(Manifest.permission.READ_CONTACTS, "Read contact permission needed. Go to android setting and allow read contact access."))
        }

        fun getAllPermission(): ArrayList<Pair<String, String>> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayListOf(
                    Pair(Manifest.permission.READ_EXTERNAL_STORAGE, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.READ_EXTERNAL_STORAGE, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.CAMERA, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.ACCESS_COARSE_LOCATION, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.ACCESS_FINE_LOCATION, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.RECORD_AUDIO, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.READ_CONTACTS, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.POST_NOTIFICATIONS, "Permission needed. Go to android setting and allow permission")
                )
            } else {
                arrayListOf(
                    Pair(Manifest.permission.READ_EXTERNAL_STORAGE, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.READ_EXTERNAL_STORAGE, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.CAMERA, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.ACCESS_COARSE_LOCATION, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.ACCESS_FINE_LOCATION, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.RECORD_AUDIO, "Permission needed. Go to android setting and allow permission"),
                    Pair(Manifest.permission.READ_CONTACTS, "Permission needed. Go to android setting and allow permission"),
                )
            }
        }
        //End: -- Permissions --

        fun checkPermissionIsGranted(context: Context, permission: ArrayList<Pair<String, String>>): Boolean {
            var isAllGranted: Boolean
            permission.forEach {
                isAllGranted = context.checkCallingOrSelfPermission(it.first) == PackageManager.PERMISSION_GRANTED
                if (isAllGranted.not()) {
                    return false
                }
            }
            return true
        }
    }

    private var permission: ArrayList<Pair<String, String>>? = null
    private var needToManageRationale = true
    var onAllPermissionGrantedCallback: (() -> Unit)? = null
    var onPermissionDeniedCallback: (() -> Unit)? = null

    fun requestPermission(permission: ArrayList<Pair<String, String>>): PermissionInHand {
        this.permission = permission
        return this
    }

    fun requestGrantCallback(onAllPermissionGranted: (() -> Unit)?): PermissionInHand {
        this.onAllPermissionGrantedCallback = onAllPermissionGranted
        return this
    }

    fun requestDeniedCallback(onAllPermissionGranted: (() -> Unit)?): PermissionInHand {
        this.onPermissionDeniedCallback = onAllPermissionGranted
        return this
    }

    fun notToManageRationale(): PermissionInHand {
        this.needToManageRationale = false
        return this
    }

    fun check() {
        val arguments = Bundle()
        arguments.putString("permission", Gson().toJson(permission))
        arguments.putBoolean("rationale", needToManageRationale)
        val fragment = PermissionManager()
        fragment.arguments = arguments
        activity.supportFragmentManager.beginTransaction()
            .add(fragment, "PermissionManager")
            .commitNowAllowingStateLoss()
    }
}
