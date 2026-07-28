package com.creador360pro.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object ImageFilterUtil {

    fun applyFilter(bitmap: Bitmap, filterType: FilterType): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint()

        when (filterType) {
            FilterType.BRIGHTNESS -> {
                val cm = ColorMatrix().apply {
                    setScale(1.3f, 1.3f, 1.3f, 1f)
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(result, 0f, 0f, paint)
            }
            FilterType.CONTRAST -> {
                val cm = ColorMatrix().apply {
                    val contrast = 1.5f
                    val translate = (-.5f * contrast + .5f) * 255f
                    set(floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(result, 0f, 0f, paint)
            }
            FilterType.SATURATION -> {
                val cm = ColorMatrix().apply {
                    setSaturation(1.8f)
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(result, 0f, 0f, paint)
            }
            FilterType.VINTAGE -> {
                val cm = ColorMatrix().apply {
                    set(floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(result, 0f, 0f, paint)
            }
            FilterType.GRAYSCALE -> {
                val cm = ColorMatrix().apply {
                    setSaturation(0f)
                }
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(result, 0f, 0f, paint)
            }
        }

        return result
    }
}

enum class FilterType {
    BRIGHTNESS, CONTRAST, SATURATION, VINTAGE, GRAYSCALE
}
