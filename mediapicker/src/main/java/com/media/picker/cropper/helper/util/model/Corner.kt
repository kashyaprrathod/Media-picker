package com.media.picker.cropper.helper.util.model

import com.media.picker.cropper.helper.util.model.Corner.*

enum class Corner {
    NONE,
    TOP_RIGHT,
    TOP_LEFT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT
}

fun Corner.opposite() {
    when (this) {
        TOP_RIGHT -> BOTTOM_LEFT
        TOP_LEFT -> BOTTOM_RIGHT
        BOTTOM_RIGHT -> TOP_LEFT
        BOTTOM_LEFT -> TOP_RIGHT
        NONE -> NONE
    }
}