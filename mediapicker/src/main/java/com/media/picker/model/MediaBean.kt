package com.media.picker.model

import android.net.Uri

data class MediaBean(
    var path: Uri? = null,

    var width: Int = 1,
    var height: Int = 1,

    /**
     * For video operations
     * **/
    var isVideo: Boolean = false,
    var duration: Long = 0,

    /**
     * Audio
     * **/
    var isAudio: Boolean = false
)