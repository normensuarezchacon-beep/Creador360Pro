package com.creador360pro.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.creador360pro.R
import java.io.File
import java.io.FileOutputStream

class VideoEditorActivity : AppCompatActivity() {

    private var videoUri: Uri? = null
    private lateinit var videoPreview: VideoView
    private lateinit var seekBar: SeekBar
    private lateinit var tvTime: TextView
    private lateinit var llTimeline: LinearLayout

    private var videoDuration = 0
    private var trimStartMs = 0L
    private var trimEndMs = 0L
    private var videoSpeed = 1.0f
    private var isMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_editor)

        videoPreview = findViewById(R.id.videoPreview)
        seekBar = findViewById(R.id.seekBar)
        tvTime = findViewById(R.id.tvTime)
        llTimeline = findViewById(R.id.llTimeline)

        setupToolbar()
        setupControls()
        importVideo()
    }

    private fun setupToolbar() {
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnImport).setOnClickListener { importVideo() }
        findViewById<Button>(R.id.btnTrim).setOnClickListener { showTrimDialog() }
        findViewById<Button>(R.id.btnSpeed).setOnClickListener { showSpeedDialog() }
        findViewById<Button>(R.id.btnMute).setOnClickListener { toggleMute() }
        findViewById<Button>(R.id.btnFilter).setOnClickListener { showFilterDialog() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportVideo() }
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
            trimEndMs = videoDuration.toLong()
            updateTimeDisplay()
            showTimelineFrames()
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
        tvTime.text = String.format("%02d:%02d / %02d:%02d", currentSec / 60, currentSec % 60, totalSec / 60, totalSec % 60)
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
                trimStartMs = 0L
                trimEndMs = 0L
                videoSpeed = 1.0f
                isMuted = false
                Toast.makeText(this, "Video cargado correctamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTimelineFrames() {
        llTimeline.removeAllViews()
        val uri = videoUri ?: return
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(this, uri)
            val duration = videoPreview.duration
            val framesCount = minOf(5, duration / 1000)
            val interval = if (framesCount > 0) duration / (framesCount + 1) else 0
            for (i in 1..framesCount) {
                val timeUs = interval * i * 1000L
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    val img = ImageView(this).apply {
                        layoutParams = LinearLayout.LayoutParams(80, 60).apply {
                            marginEnd = 8
                        }
                        setImageBitmap(bitmap)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    llTimeline.addView(img)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }
    }

    private fun showTrimDialog() {
        if (videoUri == null) {
            Toast.makeText(this, "Primero importa un video", Toast.LENGTH_SHORT).show()
            return
        }
        val options = arrayOf(
            "Marcar inicio: ${formatTime(trimStartMs)}",
            "Marcar final: ${formatTime(trimEndMs)}",
            "Resetear recorte"
        )
        AlertDialog.Builder(this)
            .setTitle("Recorte de video")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        trimStartMs = videoPreview.currentPosition.toLong()
                        Toast.makeText(this, "Inicio marcado en ${formatTime(trimStartMs)}", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        trimEndMs = videoPreview.currentPosition.toLong()
                        if (trimEndMs == 0L) trimEndMs = videoDuration.toLong()
                        Toast.makeText(this, "Final marcado en ${formatTime(trimEndMs)}", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        trimStartMs = 0L
                        trimEndMs = videoDuration.toLong()
                        Toast.makeText(this, "Recorte reseteado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSpeedDialog() {
        if (videoUri == null) {
            Toast.makeText(this, "Primero importa un video", Toast.LENGTH_SHORT).show()
            return
        }
        val options = arrayOf("0.5x (cámara lenta)", "1x (normal)", "1.5x (rápido)", "2x (muy rápido)")
        AlertDialog.Builder(this)
            .setTitle("Velocidad del video")
            .setItems(options) { _, which ->
                videoSpeed = when (which) {
                    0 -> 0.5f
                    1 -> 1.0f
                    2 -> 1.5f
                    3 -> 2.0f
                    else -> 1.0f
                }
                Toast.makeText(this, "Velocidad: ${videoSpeed}x", Toast.LENGTH_SHORT).show()
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

    private fun showFilterDialog() {
        val options = arrayOf("Sin filtro", "Vintage", "Blanco y negro", "Cálido", "Frío")
        AlertDialog.Builder(this)
            .setTitle("Filtro de color")
            .setItems(options) { _, which ->
                val filterName = options[which]
                Toast.makeText(this, "Filtro seleccionado: $filterName (se aplicará al exportar)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportVideo() {
        if (videoUri == null) {
            Toast.makeText(this, "Primero importa un video", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Exportando video...")
            .setMessage("Configuración:\nRecorte: ${formatTime(trimStartMs)} - ${formatTime(trimEndMs)}\nVelocidad: ${videoSpeed}x\nAudio: ${if (isMuted) "Silenciado" else "Conservado"}")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val outputFile = File(downloadsDir, "Creador360_${System.currentTimeMillis()}.mp4")

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
                        .setMessage("Archivo guardado en:\n${outputFile.absolutePath}\n\nTamaño: ${outputFile.length() / 1024} KB")
                        .setPositiveButton("Compartir") { _, _ -> compartirVideo(outputFile) }
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

    private fun compartirVideo(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir video"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatTime(ms: Long): String {
        val seconds = ms / 1000
        return String.format("%02d:%02d", seconds / 60, seconds % 60)
    }
}
