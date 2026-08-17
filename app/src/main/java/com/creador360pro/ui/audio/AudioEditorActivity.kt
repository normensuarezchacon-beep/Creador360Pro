package com.creador360pro.ui.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.creador360pro.R
import com.creador360pro.data.db.AppDatabase
import com.creador360pro.data.model.AudioRecordItem
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioEditorActivity : AppCompatActivity() {

    private var audioRecord: AudioRecord? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var audioFile: File? = null
    private var currentRecord: AudioRecordItem? = null
    private var isPaused = false
    private var trimStartSec = 0
    private var trimEndSec = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_editor)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnRecord).setOnClickListener { toggleRecording() }
        findViewById<Button>(R.id.btnPause).setOnClickListener { pauseResumeRecording() }
        findViewById<Button>(R.id.btnPlay).setOnClickListener { playAudio() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopAudio() }
        findViewById<Button>(R.id.btnNormalize).setOnClickListener { normalizeAudio() }
        findViewById<Button>(R.id.btnFade).setOnClickListener { applyFade() }
        findViewById<Button>(R.id.btnTrimStart).setOnClickListener { trimStartSec = getCurrentPositionSec(); Toast.makeText(this, "Inicio: $trimStartSec s", Toast.LENGTH_SHORT).show() }
        findViewById<Button>(R.id.btnTrimEnd).setOnClickListener { trimEndSec = getCurrentPositionSec(); Toast.makeText(this, "Fin: $trimEndSec s", Toast.LENGTH_SHORT).show() }
        findViewById<Button>(R.id.btnApplyTrim).setOnClickListener { applyTrim() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportAudio() }
        findViewById<Button>(R.id.btnList).setOnClickListener { showRecordList() }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 300)
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT)

        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)

        audioFile = File(externalCacheDir, "audio_${System.currentTimeMillis()}.wav")

        audioRecord?.startRecording()
        isRecording = true
        isPaused = false
        findViewById<Button>(R.id.btnRecord).text = "⏹ Detener"
        Toast.makeText(this, "Grabando...", Toast.LENGTH_SHORT).show()

        Thread {
            val data = ByteArray(bufferSize)
            val pcmData = mutableListOf<Byte>()
            while (isRecording) {
                if (!isPaused) {
                    val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                    if (read > 0) {
                        pcmData.addAll(data.take(read))
                    }
                } else {
                    Thread.sleep(100)
                }
            }
            // Guardar WAV
            try {
                val pcmBytes = pcmData.toByteArray()
                writeWavFile(audioFile!!, pcmBytes, sampleRate)
                runOnUiThread {
                    findViewById<Button>(R.id.btnRecord).text = "🎤 Grabar"
                    Toast.makeText(this, "Grabación guardada", Toast.LENGTH_SHORT).show()
                    guardarEnBaseDeDatos(audioFile!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun pauseResumeRecording() {
        if (isRecording) {
            isPaused = !isPaused
            findViewById<Button>(R.id.btnPause).text = if (isPaused) "▶" else "⏸"
            Toast.makeText(this, if (isPaused) "Grabación pausada" else "Grabación reanudada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        findViewById<Button>(R.id.btnRecord).text = "🎤 Grabar"
    }

    private fun guardarEnBaseDeDatos(file: File) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@AudioEditorActivity)
            val record = AudioRecordItem(
                nombre = "Grabación ${System.currentTimeMillis()}",
                filePath = file.absolutePath,
                duracion = file.length() / 1000,
                fechaCreacion = System.currentTimeMillis()
            )
            val id = db.audioDao().insertRecord(record)
            currentRecord = record.copy(id = id)
        }
    }

    private fun writeWavFile(file: File, pcmData: ByteArray, sampleRate: Int) {
        FileOutputStream(file).use { fos ->
            val buffer = ByteBuffer.allocate(44 + pcmData.size).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put("RIFF".toByteArray())
                putInt(36 + pcmData.size)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16)
                putShort(1)
                putShort(1)
                putInt(sampleRate)
                putInt(sampleRate * 2)
                putShort(2)
                putShort(16)
                put("data".toByteArray())
                putInt(pcmData.size)
                put(pcmData)
            }
            fos.write(buffer.array())
        }
    }

    private fun readWavFile(file: File): Pair<Int, ShortArray>? {
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                fis.read(header)
                val sampleRate = ByteBuffer.wrap(header, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
                val dataSize = fis.available()
                val data = ByteArray(dataSize)
                fis.read(data)
                val shorts = ShortArray(dataSize / 2)
                ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
                Pair(sampleRate, shorts)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeWavFile(file: File, sampleRate: Int, samples: ShortArray) {
        val pcmData = ByteArray(samples.size * 2)
        ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(samples)
        writeWavFile(file, pcmData, sampleRate)
    }

    private fun normalizeAudio() {
        val file = audioFile ?: currentRecord?.let { File(it.filePath) }
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Primero graba o selecciona un audio", Toast.LENGTH_SHORT).show()
            return
        }
        val result = readWavFile(file) ?: return
        val (sampleRate, samples) = result
        var maxPeak = 0
        for (s in samples) {
            val abs = Math.abs(s.toInt())
            if (abs > maxPeak) maxPeak = abs
        }
        if (maxPeak == 0) return
        val scale = 32767f / maxPeak
        val normalized = ShortArray(samples.size)
        for (i in samples.indices) {
            normalized[i] = (samples[i] * scale).toInt().toShort()
        }
        val outputFile = File(externalCacheDir, "normalized_${System.currentTimeMillis()}.wav")
        writeWavFile(outputFile, sampleRate, normalized)
        audioFile = outputFile
        Toast.makeText(this, "Audio normalizado", Toast.LENGTH_SHORT).show()
    }

    private fun applyFade() {
        val file = audioFile ?: currentRecord?.let { File(it.filePath) }
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Primero graba o selecciona un audio", Toast.LENGTH_SHORT).show()
            return
        }
        val result = readWavFile(file) ?: return
        val (sampleRate, samples) = result
        val fadeSamples = (sampleRate * 1.0).toInt() // 1 segundo de fade
        for (i in 0 until fadeSamples) {
            val fadeIn = i.toFloat() / fadeSamples
            val fadeOut = 1f - (i.toFloat() / fadeSamples)
            if (i < samples.size) {
                samples[i] = (samples[i] * fadeIn).toInt().toShort()
            }
            val j = samples.size - 1 - i
            if (j >= 0) {
                samples[j] = (samples[j] * fadeOut).toInt().toShort()
            }
        }
        val outputFile = File(externalCacheDir, "fade_${System.currentTimeMillis()}.wav")
        writeWavFile(outputFile, sampleRate, samples)
        audioFile = outputFile
        Toast.makeText(this, "Fade aplicado", Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentPositionSec(): Int {
        return if (mediaPlayer?.isPlaying == true) {
            mediaPlayer!!.currentPosition / 1000
        } else {
            0
        }
    }

    private fun applyTrim() {
        val file = audioFile ?: currentRecord?.let { File(it.filePath) }
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Primero graba o selecciona un audio", Toast.LENGTH_SHORT).show()
            return
        }
        if (trimEndSec <= trimStartSec) {
            Toast.makeText(this, "Marca inicio y fin correctamente", Toast.LENGTH_SHORT).show()
            return
        }
        val result = readWavFile(file) ?: return
        val (sampleRate, samples) = result
        val startSample = trimStartSec * sampleRate
        val endSample = trimEndSec * sampleRate
        if (endSample > samples.size || startSample < 0) {
            Toast.makeText(this, "Rango inválido", Toast.LENGTH_SHORT).show()
            return
        }
        val trimmed = samples.copyOfRange(startSample, endSample)
        val outputFile = File(externalCacheDir, "trimmed_${System.currentTimeMillis()}.wav")
        writeWavFile(outputFile, sampleRate, trimmed)
        audioFile = outputFile
        Toast.makeText(this, "Recorte aplicado", Toast.LENGTH_SHORT).show()
    }

    private fun playAudio() {
        val file = audioFile ?: currentRecord?.let { File(it.filePath) }
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Selecciona una grabación primero", Toast.LENGTH_SHORT).show()
            return
        }
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                start()
                Toast.makeText(this@AudioEditorActivity, "Reproduciendo...", Toast.LENGTH_SHORT).show()
                setOnCompletionListener {
                    Toast.makeText(this@AudioEditorActivity, "Reproducción terminada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@AudioEditorActivity, "Error al reproducir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                reset()
            }
        }
    }

    private fun exportAudio() {
        val file = audioFile ?: currentRecord?.let { File(it.filePath) }
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Selecciona una grabación primero", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportFile = File(downloadsDir, "Creador360_${file.name}")
            FileInputStream(file).use { input ->
                FileOutputStream(exportFile).use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(this, "Audio exportado a:\n${exportFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRecordList() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@AudioEditorActivity)
            db.audioDao().getAllRecords().collect { records ->
                if (records.isEmpty()) {
                    AlertDialog.Builder(this@AudioEditorActivity)
                        .setTitle("Grabaciones guardadas")
                        .setMessage("No hay grabaciones guardadas.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@collect
                }
                val nombres = records.map { it.nombre }.toTypedArray()
                AlertDialog.Builder(this@AudioEditorActivity)
                    .setTitle("Grabaciones guardadas (${records.size})")
                    .setItems(nombres) { _, which ->
                        currentRecord = records[which]
                        audioFile = File(records[which].filePath)
                        Toast.makeText(this@AudioEditorActivity, "Cargada: ${records[which].nombre}", Toast.LENGTH_SHORT).show()
                    }
                    .setPositiveButton("Eliminar todas") { _, _ ->
                        lifecycleScope.launch {
                            records.forEach { db.audioDao().deleteRecord(it) }
                            Toast.makeText(this@AudioEditorActivity, "Todas las grabaciones eliminadas", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }
}
