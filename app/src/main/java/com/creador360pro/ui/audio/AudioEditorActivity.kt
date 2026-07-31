package com.creador360pro.ui.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.creador360pro.R
import java.io.File
import java.io.IOException

class AudioEditorActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var audioFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_editor)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnRecord).setOnClickListener { toggleRecording() }
        findViewById<Button>(R.id.btnPlay).setOnClickListener { playAudio() }
        findViewById<Button>(R.id.btnStop).setOnClickListener { stopAudio() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportAudio() }
        findViewById<Button>(R.id.btnEffects).setOnClickListener { showEffects() }

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
        audioFile = File(externalCacheDir?.absolutePath, "grabacion_${System.currentTimeMillis()}.mp3")

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFile?.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                findViewById<Button>(R.id.btnRecord).text = "⏹ Detener"
                Toast.makeText(this@AudioEditorActivity, "Grabando...", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@AudioEditorActivity, "Error al grabar", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, "Grabación guardada", Toast.LENGTH_SHORT).show()
    }

    private fun playAudio() {
        if (audioFile == null || !audioFile!!.exists()) {
            Toast.makeText(this, "Primero graba un audio", Toast.LENGTH_SHORT).show()
            return
        }
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioFile!!.absolutePath)
                prepare()
                start()
                Toast.makeText(this@AudioEditorActivity, "Reproduciendo...", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun stopAudio() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun exportAudio() {
        if (audioFile == null || !audioFile!!.exists()) {
            Toast.makeText(this, "Primero graba un audio", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Audio guardado en: ${audioFile?.absolutePath}", Toast.LENGTH_LONG).show()
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

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }
}
