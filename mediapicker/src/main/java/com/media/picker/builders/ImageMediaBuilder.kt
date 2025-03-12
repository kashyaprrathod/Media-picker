package com.media.picker.builders

import com.media.picker.model.MediaBean

class ImageMediaBuilder : MediaBuilderBase() {
    private var needToCrop: Boolean = false

    fun cropImage(): ImageMediaBuilder {
        needToCrop = true
        return this
    }

    fun canCropImage(): Boolean {
        return needToCrop
    }

    fun captureImage(): ImageMediaBuilder {
        capture = true
        return this
    }

    fun pickImage(): ImageMediaBuilder {
        capture = false
        return this
    }

    fun setSelectionCounts(count: Int): ImageMediaBuilder {
        this.selectionCount = count
        return this
    }

    fun pickSuccessStatusListener(onComplete: ((MediaBean) -> Unit)): ImageMediaBuilder {
        this.onComplete = onComplete
        return this
    }

    fun pickMultipleSuccessStatusListener(onComplete: ((ArrayList<MediaBean>) -> Unit)): ImageMediaBuilder {
        this.onCompleteMultiple = onComplete
        return this
    }

    fun pickFailStatusListener(onFail: ((String) -> Unit)): ImageMediaBuilder {
        this.onFail = onFail
        return this
    }
}