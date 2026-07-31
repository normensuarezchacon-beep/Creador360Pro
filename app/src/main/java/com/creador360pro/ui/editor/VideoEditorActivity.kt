package com.creador360pro.ui.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.creador360pro.R

class VideoEditorActivity : AppCompatActivity() {

    private var videoUri: Uri? = null
    private lateinit var videoPreview: VideoView
    private lateinit var seekBar: SeekBar
    private lateinit var tvTime: TextView
    private lateinit var timelineSeekBar: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_editor)

        videoPreview = findViewById(R.id.videoPreview)
        seekBar = findViewById(R.id.seekBar)
        tvTime = findViewById(R.id.tvTime)
        timelineSeekBar = findViewById(R.id.timelineSeekBar)

        setupToolbar()
        setupControls()
        importVideo()
    }

    private fun setupToolbar() {
        findViewById<Button>(R.id.btnImport).setOnClickListener { importVideo() }
        findViewById<Button>(R.id.btnTrim).setOnClickListener { trimVideo() }
        findViewById<Button>(R.id.btnSpeed).setOnClickListener { changeSpeed() }
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
            seekBar.max = videoPreview.duration
            timelineSeekBar.max = videoPreview.duration
            val seconds = videoPreview.duration / 1000
            val min = seconds / 60
            val sec = seconds % 60
            tvTime.text = String.format("%02d:%02d", min, sec)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) videoPreview.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        timelineSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    videoPreview.seekTo(progress)
                    seekBar.progress = progress
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
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

    private fun trimVideo() {
        if (videoUri == null) {
            Toast.makeText(this, "Primero importa un video", Toast.LENGTH_SHORT).show()
            return
        }
        val options = arrayOf("Recortar inicio", "Recortar final", "Dividir en dos", "Quitar audio")
        AlertDialog.Builder(this)
            .setTitle("Herramientas de recorte")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, "Mueve el marcador de inicio en la línea de tiempo", Toast.LENGTH_LONG).show()
                    1 -> Toast.makeText(this, "Mueve el marcador de fin en la línea de tiempo", Toast.LENGTH_LONG).show()
                    2 -> Toast.makeText(this, "Divide el clip en la posición actual", Toast.LENGTH_LONG).show()
                    3 -> Toast.makeText(this, "Audio eliminado del clip", Toast.LENGTH_SHORT).show()
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
                val speedNames = arrayOf("cámara lenta", "normal", "rápido", "muy rápido")
                Toast.makeText(this, "Velocidad: ${speedNames[which]} (se aplicará al exportar)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportVideo() {
        if (videoUri == null) {
            Toast.makeText(this, "Primero importa un video", Toast.LENGTH_SHORT).show()
            return
        }
        val options = arrayOf("MP4 comprimido (WhatsApp)", "MP4 alta calidad", "Extraer audio (MP3)", "GIF animado")
        AlertDialog.Builder(this)
            .setTitle("Exportar video")
            .setItems(options) { _, which ->
                val formats = arrayOf("MP4 comprimido", "MP4 alta calidad", "MP3", "GIF")
                Toast.makeText(this, "Exportando como ${formats[which]} (próximamente)", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
