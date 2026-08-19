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
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import java.io.File
import java.io.FileOutputStream

class VideoEditorActivity : AppCompatActivity() {

    private val videoUris = mutableListOf<Uri>()
    private var currentVideoIndex = 0
    private lateinit var playerView: PlayerView
    private lateinit var player: ExoPlayer
    private lateinit var llTimeline: LinearLayout
    private var transitionType = "Sin transición"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_editor)

        playerView = findViewById(R.id.playerView)
        llTimeline = findViewById(R.id.llTimeline)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        setupToolbar()
        setupTabs()
        importVideo()
    }

    private fun setupToolbar() {
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAddClip).setOnClickListener { importVideo() }
        findViewById<Button>(R.id.btnTransition).setOnClickListener { showTransitionDialog() }
        findViewById<Button>(R.id.btnSpeed).setOnClickListener { showSpeedDialog() }
        findViewById<Button>(R.id.btnMute).setOnClickListener { toggleMute() }
        findViewById<Button>(R.id.btnExportTop).setOnClickListener { exportVideo() }
    }

    private fun setupTabs() {
        findViewById<Button>(R.id.btnTabCanvas).setOnClickListener { showCanvasPanel() }
        findViewById<Button>(R.id.btnTabMusic).setOnClickListener { showMusicPanel() }
        findViewById<Button>(R.id.btnTabText).setOnClickListener { showTextPanel() }
        findViewById<Button>(R.id.btnTabStickers).setOnClickListener { showStickersPanel() }
        findViewById<Button>(R.id.btnTabEffects).setOnClickListener { showEffectsPanel() }
        findViewById<Button>(R.id.btnTabFilters).setOnClickListener { showFiltersPanel() }
        findViewById<Button>(R.id.btnTabAdjust).setOnClickListener { showAdjustPanel() }
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
                updateTimeline()
                Toast.makeText(this, "Clip ${videoUris.size} añadido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playVideoAt(index: Int) {
        if (index in videoUris.indices) {
            val mediaItem = MediaItem.fromUri(videoUris[index])
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    private fun updateTimeline() {
        llTimeline.removeAllViews()
        videoUris.forEachIndexed { index, uri ->
            val thumbnail = getVideoThumbnail(uri)
            val imageView = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(80, 60).apply { marginEnd = 8 }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(thumbnail)
                setOnClickListener {
                    currentVideoIndex = index
                    playVideoAt(index)
                }
            }
            llTimeline.addView(imageView)
        }
    }

    private fun getVideoThumbnail(uri: Uri): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)
            val bitmap = retriever.getFrameAtTime(0)
            retriever.release()
            bitmap
        } catch (e: Exception) { null }
    }

    // ==================== PANELES ====================

    private fun showCanvasPanel() {
        val options = arrayOf("9:16", "1:1", "4:5", "16:9", "3:4")
        AlertDialog.Builder(this)
            .setTitle("Canvas (Relación de aspecto)")
            .setItems(options) { _, which ->
                Toast.makeText(this, "Canvas: ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showMusicPanel() {
        val options = arrayOf("Sin música", "Lo-fi Chill", "Corporativo", "Cinemático", "Urbano", "Acústico")
        AlertDialog.Builder(this)
            .setTitle("Música de fondo")
            .setItems(options) { _, which ->
                Toast.makeText(this, "Música: ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showTextPanel() {
        val input = EditText(this).apply { hint = "Escribe tu texto" }
        AlertDialog.Builder(this)
            .setTitle("Añadir texto")
            .setView(input)
            .setPositiveButton("Añadir") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    Toast.makeText(this, "Texto añadido: $text", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showStickersPanel() {
        val stickers = arrayOf("😀", "😂", "🎉", "💯", "🔥", "⭐", "❤️", "👍")
        AlertDialog.Builder(this)
            .setTitle("Stickers")
            .setItems(stickers) { _, which ->
                Toast.makeText(this, "Sticker: ${stickers[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEffectsPanel() {
        val effects = arrayOf("Sin efecto", "Glitch", "VHS", "Cine", "Desenfoque", "Saturación")
        AlertDialog.Builder(this)
            .setTitle("Efectos de video")
            .setItems(effects) { _, which ->
                Toast.makeText(this, "Efecto: ${effects[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showFiltersPanel() {
        val filters = arrayOf("Normal", "Vintage", "Blanco y negro", "Cálido", "Frío", "Sepia")
        AlertDialog.Builder(this)
            .setTitle("Filtros")
            .setItems(filters) { _, which ->
                Toast.makeText(this, "Filtro: ${filters[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAdjustPanel() {
        AlertDialog.Builder(this)
            .setTitle("Ajustes")
            .setMessage("Brillo, contraste, saturación, temperatura, nitidez")
            .setPositiveButton("OK", null)
            .show()
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

    private fun showSpeedDialog() {
        val speeds = arrayOf("0.5x", "1x", "1.5x", "2x")
        AlertDialog.Builder(this)
            .setTitle("Velocidad del clip")
            .setItems(speeds) { _, which ->
                Toast.makeText(this, "Velocidad: ${speeds[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toggleMute() {
        player.volume = if (player.volume > 0) 0f else 1f
        val btn = findViewById<Button>(R.id.btnMute)
        btn.text = if (player.volume == 0f) "🔇" else "🔊"
    }

    private fun exportVideo() {
        if (videoUris.isEmpty()) {
            Toast.makeText(this, "Añade al menos un video", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Exportando...")
            .setMessage("Uniendo ${videoUris.size} clips")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val outputFile = File(downloadsDir, "Creador360_${System.currentTimeMillis()}.mp4")

                FileOutputStream(outputFile).use { out ->
                    videoUris.forEach { uri ->
                        contentResolver.openInputStream(uri)?.use { input ->
                            input.copyTo(out)
                        }
                    }
                }

                runOnUiThread {
                    progressDialog.dismiss()
                    AlertDialog.Builder(this)
                        .setTitle("Video exportado")
                        .setMessage("${videoUris.size} clips\nTransición: $transitionType")
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

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
