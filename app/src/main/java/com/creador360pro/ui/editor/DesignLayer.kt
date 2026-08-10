package com.creador360pro.ui.editor

import android.graphics.Bitmap

data class DesignLayer(
    val type: LayerType,
    var text: String? = null,
    var bitmap: Bitmap? = null,
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 200f,
    var height: Float = 100f,
    var color: String = "#000000",
    var textSize: Float = 40f,
    var fontName: String? = null,
    var imagePath: String? = null
)

enum class LayerType {
    BACKGROUND, TEXT, IMAGE, CIRCLE, RECTANGLE
}
