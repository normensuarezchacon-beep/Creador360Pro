package com.creador360pro.ui.audio

import android.Manifest
import android.content.pm.PackageManager
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

class AudioEditorActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var audioFile: File? = null
    private var currentRecord: AudioRecordItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_editor)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnRecord).setOnClickListener { toggleRecording() }
        findViewById<Button>(R.id.btnPlay).setOnClickListener { playAudio() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopAudio() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportAudio() }
        findViewById<Button>(R.id.btnEffects).setOnClickListener { showEffects() }
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
        val fileName = "audio_${System.currentTimeMillis()}.m4a"
        audioFile = File(externalCacheDir?.absolutePath, fileName)

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioBitRate(128000)
            setOutputFile(audioFile?.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                findViewById<Button>(R.id.btnRecord).text = "⏹ Detener"
                findViewById<Button>(R.id.btnRecord).setBackgroundColor(resources.getColor(android.R.color.holo_red_dark, null))
                Toast.makeText(this, "Grabando...", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error al grabar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        findViewById<Button>(R.id.btnRecord).text = "🎤 Grabar"
        findViewById<Button>(R.id.btnRecord).setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))

        // Guardar en la base de datos
        audioFile?.let { file ->
            val nombre = "Grabación ${System.currentTimeMillis()}"
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@AudioEditorActivity)
                val record = AudioRecordItem(
                    nombre = nombre,
                    filePath = file.absolutePath,
                    fechaCreacion = System.currentTimeMillis()
                )
                val id = db.audioDao().insertRecord(record)
                currentRecord = record.copy(id = id)
                Toast.makeText(this@AudioEditorActivity, "Grabación guardada", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun showEffects() {
        val effects = arrayOf("Normalizar", "Compresor", "Reducción de ruido", "Fade in/out", "Cambiar velocidad")
        AlertDialog.Builder(this)
            .setTitle("Efectos de audio")
            .setItems(effects) { _, which ->
                val effectNames = arrayOf("normalizado", "comprimido", "con reducción de ruido", "con fade", "con velocidad ajustada")
                Toast.makeText(this, "Audio ${effectNames[which]} (próximamente)", Toast.LENGTH_SHORT).show()
            }
            .show()
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
                return@collect
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }
}
