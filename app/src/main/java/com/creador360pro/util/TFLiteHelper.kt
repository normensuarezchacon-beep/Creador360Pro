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
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

enum class DeviceTier {
    LOW,     // Gama baja: hardware limitado → MediaPipe
    MEDIUM,  // Gama media: balanceado → MediaPipe por seguridad
    HIGH     // Gama alta: potente → DeepLabV3
}

object TFLiteHelper {

    private var mediapipeInterpreter: Interpreter? = null
    private var deeplabInterpreter: Interpreter? = null
    private var mediapipeLoaded = false
    private var deeplabLoaded = false
    private var deviceTier: DeviceTier = DeviceTier.LOW

    fun initialize(context: Context) {
        // Detectar gama del dispositivo con análisis completo
        deviceTier = detectDeviceTier(context)

        // Cargar MediaPipe (SIEMPRE)
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

        // Cargar DeepLabV3 SOLO si es gama ALTA
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
        } else {
            deeplabLoaded = false
        }
    }

    private fun detectDeviceTier(context: Context): DeviceTier {
        var score = 0

        // 1. ANÁLISIS DE RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamGB = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)

        score += when {
            totalRamGB <= 3.0 -> 0   // Gama baja
            totalRamGB <= 6.0 -> 1   // Gama media
            else -> 2                 // Gama alta
        }

        // 2. ANÁLISIS DE VERSIÓN DE ANDROID
        val androidVersion = Build.VERSION.SDK_INT
        score += when {
            androidVersion < Build.VERSION_CODES.Q -> 0      // Android < 10
            androidVersion < Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> 1  // Android 10-13
            else -> 2                                          // Android 14+
        }

        // 3. ANÁLISIS DE PROCESADOR (CPU)
        val cpuInfo = getCPUInfo()
        score += when {
            cpuInfo.contains("Helio G2") || cpuInfo.contains("Helio P2") ||
            cpuInfo.contains("Snapdragon 4") || cpuInfo.contains("Unisoc") ||
            cpuInfo.contains("Spreadtrum") -> 0  // Gama baja

            cpuInfo.contains("Helio G3") || cpuInfo.contains("Helio G4") ||
            cpuInfo.contains("Helio P3") || cpuInfo.contains("Helio P4") ||
            cpuInfo.contains("Helio G8") || cpuInfo.contains("Helio P6") ||
            cpuInfo.contains("Snapdragon 6") -> 1  // Gama media

            cpuInfo.contains("Helio G9") || cpuInfo.contains("Dimensity") ||
            cpuInfo.contains("Snapdragon 7") || cpuInfo.contains("Snapdragon 8") ||
            cpuInfo.contains("Exynos 9") || cpuInfo.contains("Exynos 2") -> 2  // Gama alta

            else -> 1  // Desconocido, asumir media
        }

        // 4. ANÁLISIS DE GPU
        val gpuInfo = getGPUInfo()
        score += when {
            gpuInfo.contains("Mali-G31") || gpuInfo.contains("Mali-G52") ||
            gpuInfo.contains("PowerVR") || gpuInfo.contains("Adreno 3") ||
            gpuInfo.contains("Adreno 5") || gpuInfo.contains("Mali-400") ||
            gpuInfo.contains("Mali-450") -> 0  // Gama baja

            gpuInfo.contains("Mali-G57") || gpuInfo.contains("Mali-G68") ||
            gpuInfo.contains("Mali-G72") || gpuInfo.contains("Mali-G76") ||
            gpuInfo.contains("Adreno 6") -> 1  // Gama media

            gpuInfo.contains("Mali-G77") || gpuInfo.contains("Mali-G78") ||
            gpuInfo.contains("Mali-G710") || gpuInfo.contains("Adreno 7") ||
            gpuInfo.contains("Immortalis") -> 2  // Gama alta

            else -> 1  // Desconocido, asumir media
        }

        // 5. PUNTUACIÓN TOTAL
        return when {
            score <= 2 -> DeviceTier.LOW
            score <= 5 -> DeviceTier.MEDIUM
            else -> DeviceTier.HIGH
        }
    }

    private fun getCPUInfo(): String {
        return try {
            val process = Runtime.getRuntime().exec("cat /proc/cpuinfo")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val cpuInfo = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains("Hardware") || line!!.contains("model name")) {
                    cpuInfo.append(line).append(" ")
                }
            }
            reader.close()
            cpuInfo.toString().ifEmpty { Build.HARDWARE ?: "Desconocido" }
        } catch (e: Exception) {
            Build.HARDWARE ?: "Desconocido"
        }
    }

    private fun getGPUInfo(): String {
        return try {
            val process = Runtime.getRuntime().exec("dumpsys | grep GLES")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val gpuInfo = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains("GLES") || line!!.contains("GL_") || line!!.contains("OpenGL")) {
                    gpuInfo.append(line).append(" ")
                }
            }
            reader.close()

            if (gpuInfo.isEmpty()) {
                // Intentar otro método
                val process2 = Runtime.getRuntime().exec("cat /proc/gpu_info")
                val reader2 = BufferedReader(InputStreamReader(process2.inputStream))
                var line2: String?
                while (reader2.readLine().also { line2 = it } != null) {
                    gpuInfo.append(line2).append(" ")
                }
                reader2.close()
            }

            gpuInfo.toString().ifEmpty { "GPU no detectada" }
        } catch (e: Exception) {
            "GPU no disponible"
        }
    }

    fun getDeviceTier(): DeviceTier = deviceTier
    fun isMediapipeLoaded(): Boolean = mediapipeLoaded
    fun isDeeplabLoaded(): Boolean = deeplabLoaded

    fun getActiveModelName(): String {
        return when {
            mediapipeLoaded && (deviceTier == DeviceTier.LOW || deviceTier == DeviceTier.MEDIUM) -> 
                "MediaPipe (optimizado para tu dispositivo)"
            deeplabLoaded && deviceTier == DeviceTier.HIGH -> 
                "DeepLabV3 (alta precisión)"
            mediapipeLoaded -> 
                "MediaPipe (modo seguro)"
            else -> "Ninguno"
        }
    }

    fun getDeviceInfo(context: Context): String {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamGB = String.format("%.1f", memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0))

        val cpuInfo = getCPUInfo().take(40)
        val gpuInfo = getGPUInfo().take(30)

        val tierName = when (deviceTier) {
            DeviceTier.LOW -> "Baja"
            DeviceTier.MEDIUM -> "Media"
            DeviceTier.HIGH -> "Alta"
        }

        return "RAM: ${totalRamGB}GB | Android: ${Build.VERSION.SDK_INT}\n" +
                "CPU: $cpuInfo\n" +
                "GPU: $gpuInfo\n" +
                "Clasificación: Gama $tierName\n" +
                "Modelo IA: ${getActiveModelName()}"
    }

    fun removeBackground(originalBitmap: Bitmap): Bitmap? {
        return when {
            // Solo usar DeepLabV3 en gama ALTA
            deviceTier == DeviceTier.HIGH && deeplabLoaded -> removeBackgroundDeeplab(originalBitmap)
            // Para gama baja y media, usar MediaPipe
            mediapipeLoaded -> removeBackgroundMediapipe(originalBitmap)
            else -> null
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
