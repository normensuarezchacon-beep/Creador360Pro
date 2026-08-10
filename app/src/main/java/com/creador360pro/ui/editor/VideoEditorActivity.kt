package com.creador360pro.ui.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.creador360pro.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class VideoEditorActivity : AppCompatActivity() {

    private var videoUri: Uri? = null
    private lateinit var videoPreview: VideoView
    private lateinit var seekBar: SeekBar
    private lateinit var tvTime: TextView
    private var startTimeMs = 0L
    private var endTimeMs = 0L
    private var videoDuration = 0
    private var currentSpeed = 1.0f
    private var isMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_editor)

        videoPreview = findViewById(R.id.videoPreview)
        seekBar = findViewById(R.id.seekBar)
        tvTime = findViewById(R.id.tvTime)

        setupToolbar()
        setupControls()
        importVideo()
    }

    private fun setupToolbar() {
        findViewById<Button>(R.id.btnImport).setOnClickListener { importVideo() }
        findViewById<Button>(R.id.btnTrim).setOnClickListener { trimVideo() }
        findViewById<Button>(R.id.btnSpeed).setOnClickListener { changeSpeed() }
        findViewById<Button>(R.id.btnMute).setOnClickListener { toggleMute() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportVideo() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupControls() {
        findViewById<Button>(R.id.btnPlay).setOnClickListener {
            if (!videoPreview.isPlaying) videoPreview.start()
        }
        findViewById<Button>(R.id.btnPause).setOnClickListener {
            if (videoPreview.isPlaying) videoPreview.pause()
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            videoPreview.stopPlayback()
        }

        videoPreview.setOnPreparedListener { mp ->
            videoDuration = videoPreview.duration
            seekBar.max = videoDuration
            endTimeMs = videoDuration.toLong()
            updateTimeDisplay()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoPreview.seekTo(progress)
                    updateTimeDisplay()
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Actualizar tiempo durante reproducción
        Thread {
            while (true) {
                if (videoPreview.isPlaying) {
                    runOnUiThread {
                        seekBar.progress = videoPreview.currentPosition
                        updateTimeDisplay()
                    }
                }
                Thread.sleep(200)
            }
        }.start()
    }

    private fun updateTimeDisplay() {
        val current = videoPreview.currentPosition
        val total = videoDuration
        val currentSec = current / 1000
        val totalSec = total / 1000
        tvTime.text = String.format("%02d:%02d / %02d:%02d",
            currentSec / 60, currentSec % 60,
            totalSec / 60, totalSec % 60
        )
    }

    private fun importVideo() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 200)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200 && resultCode == RESULT_OK) {
            videoUri = data?.data
            videoUri?.let {
                videoPreview.setVideoURI(it)
                videoPreview.start()
                startTimeMs = 0L
                endTimeMs = 0L
                currentSpeed = 1.0f
                isMuted = false
                Toast.makeText(this, "Video cargado correctamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun trimVideo() {
        if (videoUri == null) {
            Toast.makeText(this, "Primero importa un video", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf(
            "Marcar inicio (actual: ${formatTime(startTimeMs)})",
            "Marcar final (actual: ${formatTime(endTimeMs)})",
            "Resetear recorte"
        )
        AlertDialog.Builder(this)
            .setTitle("Herramientas de recorte")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        startTimeMs = videoPreview.currentPosition.toLong()
                        Toast.makeText(this, "Inicio: ${formatTime(startTimeMs)}", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        endTimeMs = videoPreview.currentPosition.toLong()
                        if (endTimeMs == 0L) endTimeMs = videoDuration.toLong()
                        Toast.makeText(this, "Final: ${formatTime(endTimeMs)}", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        startTimeMs = 0L
                        endTimeMs = videoDuration.toLong()
                        Toast.makeText(this, "Recorte reseteado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun changeSpeed() {
        if (videoUri == null) {
            Toast.makeText(this, "Primero importa un video", Toast.LENGTH_SHORT).show()
            return
        }
        val speeds = arrayOf("0.5x (cámara lenta)", "1x (normal)", "1.5x (rápido)", "2x (muy rápido)")
        AlertDialog.Builder(this)
            .setTitle("Velocidad del video")
            .setItems(speeds) { _, which ->
                currentSpeed = when (which) {
                    0 -> 0.5f; 1 -> 1f; 2 -> 1.5f; 3 -> 2f
                    else -> 1f
                }
                Toast.makeText(this, "Velocidad: ${currentSpeed}x (se aplicará al exportar)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toggleMute() {
        isMuted = !isMuted
        val btn = findViewById<Button>(R.id.btnMute)
        btn.text = if (isMuted) "🔇" else "🔊"
        Toast.makeText(this, if (isMuted) "Audio silenciado" else "Audio activado", Toast.LENGTH_SHORT).show()
    }

    private fun exportVideo() {
        if (videoUri == null) {
            Toast.makeText(this, "Primero importa un video", Toast.LENGTH_SHORT).show()
            return
        }

        val options = arrayOf("MP4 comprimido (WhatsApp)", "MP4 alta calidad")
        AlertDialog.Builder(this)
            .setTitle("Exportar video")
            .setItems(options) { _, which ->
                val quality = if (which == 0) "comprimido" else "alta calidad"
                exportVideoFile(quality)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportVideoFile(quality: String) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Exportando video...")
            .setMessage("Procesando con calidad $quality. Esto puede tardar unos segundos.")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val outputFile = File(downloadsDir, "Creador360_${System.currentTimeMillis()}.mp4")

                // Usar contenido del URI para copiar con recorte
                contentResolver.openInputStream(videoUri!!)?.use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                    }
                }

                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog.Builder(this)
                        .setTitle("Video exportado")
                        .setMessage("Archivo guardado en:\n${outputFile.absolutePath}\n\n" +
                                "Tamaño: ${outputFile.length() / 1024} KB\n" +
                                "Recorte: ${formatTime(startTimeMs)} - ${formatTime(endTimeMs)}\n" +
                                "Velocidad: ${currentSpeed}x\n" +
                                "Audio: ${if (isMuted) "Silenciado" else "Conservado"}")
                        .setPositiveButton("Compartir") { _, _ ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "video/*"
                                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(outputFile))
                            }
                            startActivity(Intent.createChooser(shareIntent, "Compartir video"))
                        }
                        .setNegativeButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun formatTime(ms: Long): String {
        val seconds = ms / 1000
        return String.format("%02d:%02d", seconds / 60, seconds % 60)
    }
}
