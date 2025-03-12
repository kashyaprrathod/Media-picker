package com.media.picker.builders

import com.media.picker.model.MediaBean

class AudioMediaBuilder : MediaBuilderBase() {
    fun setSelectionCounts(count: Int): AudioMediaBuilder {
        this.selectionCount = count
        return this
    }

    fun pickSuccessStatusListener(onComplete: ((MediaBean) -> Unit)): AudioMediaBuilder {
        this.onComplete = onComplete
        return this
    }

    fun pickMultipleSuccessStatusListener(onComplete: ((ArrayList<MediaBean>) -> Unit)): AudioMediaBuilder {
        this.onCompleteMultiple = onComplete
        return this
    }

    fun pickFailStatusListener(onFail: ((String) -> Unit)): AudioMediaBuilder {
        this.onFail = onFail
        return this
    }
}