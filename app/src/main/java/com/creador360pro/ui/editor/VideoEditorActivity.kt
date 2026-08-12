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

    private var videoUri: Uri? = null
    private lateinit var videoPreview: VideoView
    private lateinit var seekBar: SeekBar
    private lateinit var tvTime: TextView
    private var videoDuration = 0

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
        tvTime.text = String.format(
            "%02d:%02d / %02d:%02d",
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
                Toast.makeText(this, "Video cargado correctamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportVideo() {
        if (videoUri == null) {
            Toast.makeText(this, "Primero importa un video", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Exportando video...")
            .setMessage("Copiando video a la carpeta Descargas.")
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
                        .setPositiveButton("Compartir") { _, _ ->
                            compartirVideo(outputFile)
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

    private fun compartirVideo(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
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
}
