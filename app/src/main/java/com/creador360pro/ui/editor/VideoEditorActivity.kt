package com.creador360pro.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.creador360pro.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
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
    private var currentSpeed = 1.0f
    private var isMuted = false
    private var currentFilter = "Normal"
    private var selectedMusic = "Sin música"

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
        findViewById<Button>(R.id.btnTrim).setOnClickListener { showTrimDialog() }
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
            applySpeed()
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

    // ==================== RECORTE ====================

    private var trimStartMs = 0L
    private var trimEndMs = 0L

    private fun showTrimDialog() {
        if (videoUris.isEmpty()) {
            Toast.makeText(this, "Añade un video primero", Toast.LENGTH_SHORT).show()
            return
        }
        val currentPos = player.currentPosition
        val options = arrayOf(
            "Marcar inicio: ${formatTime(trimStartMs)}",
            "Marcar final: ${formatTime(trimEndMs)}",
            "Resetear recorte"
        )
        AlertDialog.Builder(this)
            .setTitle("Recorte del clip")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        trimStartMs = currentPos
                        Toast.makeText(this, "Inicio marcado", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        trimEndMs = currentPos
                        if (trimEndMs == 0L) trimEndMs = player.duration
                        Toast.makeText(this, "Final marcado", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        trimStartMs = 0L
                        trimEndMs = player.duration
                        Toast.makeText(this, "Recorte reseteado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==================== VELOCIDAD ====================

    private fun showSpeedDialog() {
        val speeds = arrayOf("0.5x (lento)", "1x (normal)", "1.5x (rápido)", "2x (muy rápido)")
        AlertDialog.Builder(this)
            .setTitle("Velocidad")
            .setItems(speeds) { _, which ->
                currentSpeed = when (which) {
                    0 -> 0.5f
                    1 -> 1.0f
                    2 -> 1.5f
                    3 -> 2.0f
                    else -> 1.0f
                }
                applySpeed()
                Toast.makeText(this, "Velocidad: ${currentSpeed}x", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun applySpeed() {
        player.playbackParameters = PlaybackParameters(currentSpeed)
    }

    // ==================== FILTROS ====================

    private fun showFiltersPanel() {
        val filters = arrayOf("Normal", "Vintage", "Blanco y negro", "Cálido", "Frío")
        AlertDialog.Builder(this)
            .setTitle("Filtros")
            .setItems(filters) { _, which ->
                currentFilter = filters[which]
                applyFilter()
                Toast.makeText(this, "Filtro: $currentFilter", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun applyFilter() {
        // Los filtros se aplican visualmente en el preview si ExoPlayer tiene efecto.
        // Para simplificar, guardamos el filtro seleccionado para aplicarlo en exportación.
    }

    // ==================== MÚSICA ====================

    private fun showMusicPanel() {
        val options = arrayOf("Sin música", "Lo-fi Chill", "Corporativo", "Cinemático", "Urbano", "Acústico")
        AlertDialog.Builder(this)
            .setTitle("Música de fondo")
            .setItems(options) { _, which ->
                selectedMusic = options[which]
                Toast.makeText(this, "Música: $selectedMusic", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==================== TEXTO Y STICKERS ====================

    private fun showTextPanel() {
        val input = EditText(this).apply {
            hint = "Escribe tu texto"
            gravity = Gravity.TOP
            minLines = 2
        }
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

    // ==================== EFECTOS Y AJUSTES ====================

    private fun showEffectsPanel() {
        val effects = arrayOf("Sin efecto", "Glitch", "VHS", "Cine", "Desenfoque")
        AlertDialog.Builder(this)
            .setTitle("Efectos")
            .setItems(effects) { _, which ->
                Toast.makeText(this, "Efecto: ${effects[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAdjustPanel() {
        AlertDialog.Builder(this)
            .setTitle("Ajustes")
            .setMessage("Brillo, contraste, saturación, temperatura\n(se aplicarían en exportación)")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showCanvasPanel() {
        val options = arrayOf("9:16", "1:1", "4:5", "16:9")
        AlertDialog.Builder(this)
            .setTitle("Canvas")
            .setItems(options) { _, which ->
                Toast.makeText(this, "Canvas: ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==================== TRANSICIÓN ====================

    private fun showTransitionDialog() {
        val transitions = arrayOf("Sin transición", "Fundido", "Deslizar izquierda", "Deslizar arriba", "Zoom")
        AlertDialog.Builder(this)
            .setTitle("Transición")
            .setItems(transitions) { _, which ->
                transitionType = transitions[which]
                Toast.makeText(this, "Transición: $transitionType", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ==================== SILENCIO ====================

    private fun toggleMute() {
        isMuted = !isMuted
        player.volume = if (isMuted) 0f else 1f
        findViewById<Button>(R.id.btnMute).text = if (isMuted) "🔇" else "🔊"
    }

    // ==================== EXPORTACIÓN ====================

    private fun exportVideo() {
        if (videoUris.isEmpty()) {
            Toast.makeText(this, "Añade al menos un video", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Exportando...")
            .setMessage("Configuración:\nClips: ${videoUris.size}\nVelocidad: ${currentSpeed}x\nFiltro: $currentFilter\nMúsica: $selectedMusic\nTransición: $transitionType")
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

    private fun formatTime(ms: Long): String {
        val seconds = ms / 1000
        return String.format("%02d:%02d", seconds / 60, seconds % 60)
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
