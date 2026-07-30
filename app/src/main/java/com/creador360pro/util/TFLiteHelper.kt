package com.creador360pro.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

enum class DeviceTier {
    LOW,    // Gama baja: RAM ≤ 3GB
    HIGH    // Gama media/alta: RAM > 3GB
}

object TFLiteHelper {

    private var mediapipeInterpreter: Interpreter? = null
    private var deeplabInterpreter: Interpreter? = null
    private var mediapipeLoaded = false
    private var deeplabLoaded = false
    private var deviceTier: DeviceTier = DeviceTier.LOW

    fun initialize(context: Context) {
        // Detectar gama del dispositivo
        deviceTier = detectDeviceTier(context)

        // Cargar MediaPipe (siempre, para gama baja)
        try {
            val modelFile = context.assets.openFd("models/mediapipe_selfie_segmentation.tflite")
            val inputStream = FileInputStream(modelFile.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = modelFile.startOffset
            val declaredLength = modelFile.declaredLength
            val mappedByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
            )
            mediapipeInterpreter = Interpreter(mappedByteBuffer)
            mediapipeLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
            mediapipeLoaded = false
        }

        // Cargar DeepLabV3 solo si es gama alta
        if (deviceTier == DeviceTier.HIGH) {
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
                deeplabInterpreter = Interpreter(mappedByteBuffer)
                deeplabLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
                deeplabLoaded = false
            }
        }
    }

    private fun detectDeviceTier(context: Context): DeviceTier {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        // Obtener RAM total en GB
        val totalRamGB = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)

        // Versión de Android
        val androidVersion = Build.VERSION.SDK_INT

        // Lógica de detección:
        // Gama baja: RAM ≤ 3GB O Android < 9 (API 28)
        // Gama alta: RAM > 3GB Y Android ≥ 9
        return if (totalRamGB > 3.0 && androidVersion >= Build.VERSION_CODES.P) {
            DeviceTier.HIGH
        } else {
            DeviceTier.LOW
        }
    }

    fun getDeviceTier(): DeviceTier = deviceTier
    fun isMediapipeLoaded(): Boolean = mediapipeLoaded
    fun isDeeplabLoaded(): Boolean = deeplabLoaded

    fun getActiveModelName(): String {
        return when {
            deviceTier == DeviceTier.HIGH && deeplabLoaded -> "DeepLabV3 (alta precisión)"
            mediapipeLoaded -> "MediaPipe (optimizado)"
            else -> "Ninguno"
        }
    }

    fun getDeviceInfo(context: Context): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamGB = String.format("%.1f", memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0))

        return "RAM: ${totalRamGB}GB | Android: ${Build.VERSION.SDK_INT} | " +
                "Gama: ${if (deviceTier == DeviceTier.HIGH) "Alta" else "Baja"} | " +
                "Modelo: ${getActiveModelName()}"
    }

    fun removeBackground(originalBitmap: Bitmap): Bitmap? {
        // Usar DeepLabV3 si está disponible (gama alta), sino MediaPipe
        return if (deviceTier == DeviceTier.HIGH && deeplabLoaded) {
            removeBackgroundDeeplab(originalBitmap)
        } else if (mediapipeLoaded) {
            removeBackgroundMediapipe(originalBitmap)
        } else {
            null
        }
    }

    private fun removeBackgroundMediapipe(originalBitmap: Bitmap): Bitmap? {
        val interpreter = this.mediapipeInterpreter ?: return null
        val inputSize = 256

        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, inputSize, inputSize, true)

        val pixels = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        val inputBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).apply {
            order(ByteOrder.nativeOrder())
        }

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        val outputBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 1).apply {
            order(ByteOrder.nativeOrder())
        }

        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        val maskBitmap = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val maskPixels = IntArray(inputSize * inputSize)

        for (i in maskPixels.indices) {
            val probability = outputBuffer.float
            maskPixels[i] = if (probability > 0.5f) Color.WHITE else Color.BLACK
        }

        maskBitmap.setPixels(maskPixels, 0, inputSize, 0, 0, inputSize, inputSize)
        val scaledMask = Bitmap.createScaledBitmap(maskBitmap, originalBitmap.width, originalBitmap.height, true)

        return applyMask(originalBitmap, scaledMask)
    }

    private fun removeBackgroundDeeplab(originalBitmap: Bitmap): Bitmap? {
        val interpreter = this.deeplabInterpreter ?: return null
        val inputSize = 257

        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, inputSize, inputSize, true)

        val inputBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).apply {
            order(ByteOrder.nativeOrder())
        }

        val pixels = IntArray(inputSize * inputSize)
        resizedBitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
        }

        val outputBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 2).apply {
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

        return applyMask(originalBitmap, scaledMask)
    }

    private fun applyMask(originalBitmap: Bitmap, maskBitmap: Bitmap): Bitmap {
        val resultBitmap = Bitmap.createBitmap(
            originalBitmap.width,
            originalBitmap.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(resultBitmap)
        val paint = Paint().apply { isAntiAlias = true }

        canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        canvas.drawBitmap(maskBitmap, 0f, 0f, paint)
        paint.xfermode = null

        return resultBitmap
    }

    fun close() {
        mediapipeInterpreter?.close()
        deeplabInterpreter?.close()
        mediapipeInterpreter = null
        deeplabInterpreter = null
    }
}
