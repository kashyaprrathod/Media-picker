package com.media.picker.builders

import com.media.picker.model.MediaBean


open class MediaBuilderBase {
    internal var selectionCount = 1
    internal var capture: Boolean = false

    internal var onComplete: ((MediaBean) -> Unit)? = null
    internal var onCompleteMultiple: ((ArrayList<MediaBean>) -> Unit)? = null
    internal var onFail: ((String) -> Unit)? = null

    fun canPickMultiple() = selectionCount != 1

    fun getSelectionCounts() = selectionCount

    fun getPickSuccessListener() = onComplete

    fun getMultiplePickSuccessListener() = onCompleteMultiple

    fun getPickFailedListener() = onFail
}