package com.creador360pro.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

object TFLiteHelper {

    private var interpreter: Interpreter? = null
    private val inputSize = 257

    fun initialize(context: Context) {
        if (interpreter != null) return
        try {
            val modelFile = context.assets.openFd("models/deeplabv3_257.tflite")
            val inputStream = FileInputStream(modelFile.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = modelFile.startOffset
            val declaredLength = modelFile.declaredLength
            val mappedByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
            )
            interpreter = Interpreter(mappedByteBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeBackground(originalBitmap: Bitmap): Bitmap? {
        val interpreter = this.interpreter ?: return null

        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, inputSize, inputSize, true)

        val inputBuffer = ByteBuffer.allocateDirect(
            4 * inputSize * inputSize * 3
        ).apply {
            order(ByteOrder.nativeOrder())
        }

        val pixels = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
        }

        val outputBuffer = ByteBuffer.allocateDirect(
            4 * inputSize * inputSize * 2
        ).apply {
            order(ByteOrder.nativeOrder())
        }

        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        val maskBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val maskPixels = IntArray(inputSize * inputSize)

        for (i in maskPixels.indices) {
            val personProb = outputBuffer.float
            val backgroundProb = outputBuffer.float
            maskPixels[i] = if (personProb > backgroundProb) Color.WHITE else Color.BLACK
        }

        maskBitmap.setPixels(maskPixels, 0, inputSize, 0, 0, inputSize, inputSize)

        val scaledMask = Bitmap.createScaledBitmap(maskBitmap, originalBitmap.width, originalBitmap.height, true)

        val resultBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(scaledMask, 0f, 0f, paint)
        paint.xfermode = null

        return resultBitmap
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
