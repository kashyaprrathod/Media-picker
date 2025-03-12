package com.media.picker.cropper.helper.util.model

import com.media.picker.cropper.helper.util.model.Edge.*

enum class Edge {
    NONE,
    LEFT,
    TOP,
    RIGHT,
    BOTTOM
}

fun Edge.opposite() {
    when (this) {
        LEFT -> RIGHT
        TOP -> BOTTOM
        RIGHT -> LEFT
        BOTTOM -> TOP
        NONE -> NONE
    }
}