package com.media.picker.builders

import com.media.picker.model.MediaBean


class VideoMediaBuilder : MediaBuilderBase() {
    private var maxVideoDuration: Long = 0
    private var needToTrim: Boolean = false

    fun recordVideo(): VideoMediaBuilder {
        capture = true
        return this
    }

    fun pickVideo(): VideoMediaBuilder {
        capture = false
        return this
    }

    fun trimVideo(): VideoMediaBuilder {
        needToTrim = true
        return this
    }

    fun isTrimEnabled(): Boolean {
        return needToTrim
    }

    fun getMaxVideoDurationInMillis(): Long {
        return maxVideoDuration
    }

    fun getMaxVideoDurationInSecond(): Long {
        return maxVideoDuration.div(1000)
    }

    fun setMaxVideoDurationInSeconds(duration: Long): VideoMediaBuilder {
        this.maxVideoDuration = duration * 1000
        return this
    }

    fun setSelectionCounts(count: Int): VideoMediaBuilder {
        this.selectionCount = count
        return this
    }

    fun pickSuccessStatusListener(onComplete: ((MediaBean) -> Unit)): VideoMediaBuilder {
        this.onComplete = onComplete
        return this
    }

    fun pickMultipleSuccessStatusListener(onComplete: ((ArrayList<MediaBean>) -> Unit)): VideoMediaBuilder {
        this.onCompleteMultiple = onComplete
        return this
    }

    fun pickFailStatusListener(onFail: ((String) -> Unit)): VideoMediaBuilder {
        this.onFail = onFail
        return this
    }
}