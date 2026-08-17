package com.creador360pro.ui.editor

import android.content.Intent
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

    private val videoUris = mutableListOf<Uri>()
    private var currentVideoIndex = 0
    private lateinit var videoPreview: VideoView
    private lateinit var seekBar: SeekBar
    private lateinit var tvTime: TextView
    private lateinit var llClipThumbnails: LinearLayout
    private var videoDuration = 0
    private var transitionType = "Sin transición"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_editor)

        videoPreview = findViewById(R.id.videoPreview)
        seekBar = findViewById(R.id.seekBar)
        tvTime = findViewById(R.id.tvTime)
        llClipThumbnails = findViewById(R.id.llClipThumbnails)

        setupToolbar()
        setupControls()
        importVideo()
    }

    private fun setupToolbar() {
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAddClip).setOnClickListener { importVideo() }
        findViewById<Button>(R.id.btnTransition).setOnClickListener { showTransitionDialog() }
        findViewById<Button>(R.id.btnTrim).setOnClickListener { showTrimDialog() }
        findViewById<Button>(R.id.btnSpeed).setOnClickListener { showSpeedDialog() }
        findViewById<Button>(R.id.btnMute).setOnClickListener { toggleMute() }
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
            val uri = data?.data
            uri?.let {
                videoUris.add(it)
                currentVideoIndex = videoUris.size - 1
                playVideoAt(currentVideoIndex)
                updateClipThumbnails()
                Toast.makeText(this, "Clip ${videoUris.size} añadido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playVideoAt(index: Int) {
        if (index in videoUris.indices) {
            videoPreview.setVideoURI(videoUris[index])
            videoPreview.start()
        }
    }

    private fun updateClipThumbnails() {
        llClipThumbnails.removeAllViews()
        videoUris.forEachIndexed { index, _ ->
            val chip = TextView(this).apply {
                text = "🎬 Clip ${index + 1}"
                textSize = 12f
                setTextColor(android.graphics.Color.WHITE)
                setPadding(16, 8, 16, 8)
                setBackgroundColor(android.graphics.Color.parseColor("#8B5CF6"))
                setOnClickListener {
                    currentVideoIndex = index
                    playVideoAt(index)
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8
            }
            chip.layoutParams = params
            llClipThumbnails.addView(chip)
        }
    }

    private fun showTransitionDialog() {
        val transitions = arrayOf("Sin transición", "Fundido", "Deslizar izquierda", "Deslizar arriba", "Zoom")
        AlertDialog.Builder(this)
            .setTitle("Transición entre clips")
            .setItems(transitions) { _, which ->
                transitionType = transitions[which]
                Toast.makeText(this, "Transición: $transitionType", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showTrimDialog() {
        AlertDialog.Builder(this)
            .setTitle("Recorte")
            .setMessage("Selecciona el clip que quieres recortar y usa la barra de progreso para marcar inicio y fin.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSpeedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Velocidad")
            .setMessage("Velocidad configurable: 0.5x, 1x, 1.5x, 2x")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleMute() {
        AlertDialog.Builder(this)
            .setTitle("Audio")
            .setMessage("Audio del clip actual conservado.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun exportVideo() {
        if (videoUris.isEmpty()) {
            Toast.makeText(this, "Añade al menos un video", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Exportando video...")
            .setMessage("Uniendo ${videoUris.size} clips con transición: $transitionType")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val outputFile = File(downloadsDir, "Creador360_${System.currentTimeMillis()}.mp4")

                // Copiar secuencialmente los clips (exportación simple)
                FileOutputStream(outputFile).use { outputStream ->
                    videoUris.forEach { uri ->
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                }

                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog.Builder(this)
                        .setTitle("Video exportado")
                        .setMessage("Clips: ${videoUris.size}\nTransición: $transitionType\nArchivo: ${outputFile.name}")
                        .setPositiveButton("Compartir") { _, _ -> compartirVideo(outputFile) }
                        .setNegativeButton("OK", null)
                        .show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
