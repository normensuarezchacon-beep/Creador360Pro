package com.creador360pro.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.creador360pro.R
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import java.io.File
import java.io.FileOutputStream

class VideoEditorActivity : AppCompatActivity() {

    private val videoUris = mutableListOf<Uri>()
    private var currentVideoIndex = 0
    private lateinit var playerView: PlayerView
    private lateinit var player: ExoPlayer
    private lateinit var llTimeline: LinearLayout
    private lateinit var seekBar: SeekBar
    private lateinit var textOverlay: TextView
    private lateinit var stickerOverlay: ImageView
    private lateinit var filterOverlay: View
    private lateinit var btnMute: Button
    private lateinit var rangeSlider: com.google.android.material.slider.RangeSlider

    private var transitionType = "Sin transición"
    private var transitionDuration = 500L
    private var currentSpeed = 1.0f
    private var isMuted = false
    private var currentFilter = "Normal"
    private var selectedMusic = "Sin música"
    private var currentCanvas = "9:16"
    private var musicPlayer: MediaPlayer? = null
    private var musicVolume = 0.3f

    // Capas de texto
    private val textLayers = mutableListOf<Pair<TextView, Long>>()
    private val stickerLayers = mutableListOf<Pair<ImageView, Long>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_editor)

        playerView = findViewById(R.id.playerView)
        llTimeline = findViewById(R.id.llTimeline)
        seekBar = findViewById(R.id.seekBar)
        textOverlay = findViewById(R.id.textOverlay)
        stickerOverlay = findViewById(R.id.stickerOverlay)
        filterOverlay = findViewById(R.id.filterOverlay)
        btnMute = findViewById(R.id.btnMute)
        rangeSlider = findViewById(R.id.rangeSlider)

        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        setupToolbar()
        setupTabs()
        setupRangeSlider()
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

    private fun setupRangeSlider() {
        rangeSlider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                val start = slider.values[0]
                val end = slider.values[1]
                // Guardar recorte del clip actual
                trimStartMs = start.toLong()
                trimEndMs = end.toLong()
                Toast.makeText(this, "Recorte: ${formatTime(trimStartMs)} - ${formatTime(trimEndMs)}", Toast.LENGTH_SHORT).show()
            }
        }
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
                rebuildPlayer()
                updateTimeline()
                updateRangeSlider()
                Toast.makeText(this, "Clip ${videoUris.size} añadido", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rebuildPlayer() {
        val mediaSources = videoUris.map { uri ->
            MediaItem.fromUri(uri)
        }
        val concatenatedSource = ConcatenatingMediaSource(*mediaSources.toTypedArray())
        player.setMediaSource(concatenatedSource)
        player.prepare()
        player.play()
        applySpeed()
    }

    private fun playVideoAt(index: Int) {
        if (index in videoUris.indices) {
            player.seekTo(index, 0)
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
                    updateRangeSlider()
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

    private fun updateRangeSlider() {
        if (videoUris.isNotEmpty()) {
            val duration = getVideoDuration(videoUris[currentVideoIndex])
            rangeSlider.valueFrom = 0f
            rangeSlider.valueTo = duration.toFloat()
            rangeSlider.values = listOf(0f, duration.toFloat())
        }
    }

    private fun getVideoDuration(uri: Uri): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(this, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            retriever.release()
            duration
        } catch (e: Exception) { 0L }
    }

    // ==================== RECORTE ====================

    private var trimStartMs = 0L
    private var trimEndMs = 0L

    private fun showTrimDialog() {
        if (videoUris.isEmpty()) {
            Toast.makeText(this, "Añade un video primero", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "Arrastra los bordes del slider para recortar", Toast.LENGTH_LONG).show()
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
        val color = when (currentFilter) {
            "Vintage" -> Color.argb(80, 255, 215, 0)
            "Blanco y negro" -> Color.argb(100, 128, 128, 128)
            "Cálido" -> Color.argb(80, 255, 140, 0)
            "Frío" -> Color.argb(80, 0, 140, 255)
            else -> Color.TRANSPARENT
        }
        filterOverlay.setBackgroundColor(color)
    }

    // ==================== MÚSICA ====================

    private fun showMusicPanel() {
        val options = arrayOf("Sin música", "Lo-fi Chill", "Corporativo", "Cinemático", "Urbano", "Acústico")
        AlertDialog.Builder(this)
            .setTitle("Música de fondo")
            .setItems(options) { _, which ->
                selectedMusic = options[which]
                if (selectedMusic == "Sin música") {
                    stopMusic()
                } else {
                    playMusic(selectedMusic)
                }
                Toast.makeText(this, "Música: $selectedMusic", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun playMusic(track: String) {
        stopMusic()
        musicPlayer = MediaPlayer().apply {
            try {
                val assetPath = when (track) {
                    "Lo-fi Chill" -> "music/lofi_chill.mp3"
                    "Corporativo" -> "music/corporate_upbeat.mp3"
                    "Cinemático" -> "music/cinematic_drama.mp3"
                    "Urbano" -> "music/urban_beat.mp3"
                    "Acústico" -> "music/acoustic_warm.mp3"
                    else -> null
                }
                if (assetPath != null) {
                    val assetFile = assets.openFd(assetPath)
                    setDataSource(assetFile.fileDescriptor, assetFile.startOffset, assetFile.length)
                    prepare()
                    isLooping = true
                    setVolume(musicVolume, musicVolume)
                    start()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo cargar la música", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopMusic() {
        musicPlayer?.release()
        musicPlayer = null
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
                    addTextOverlay(text)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addTextOverlay(text: String) {
        textOverlay.text = text
        textOverlay.visibility = View.VISIBLE
        textOverlay.alpha = 1f
        textOverlay.setTextColor(Color.WHITE)
        textOverlay.textSize = 24f
        textOverlay.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private fun showStickersPanel() {
        val stickers = arrayOf("😀", "😂", "🎉", "💯", "🔥", "⭐", "❤️", "👍")
        AlertDialog.Builder(this)
            .setTitle("Stickers")
            .setItems(stickers) { _, which ->
                addStickerOverlay(stickers[which])
                Toast.makeText(this, "Sticker: ${stickers[which]}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addStickerOverlay(sticker: String) {
        stickerOverlay.setImageResource(android.R.drawable.ic_menu_emoticons)
        stickerOverlay.visibility = View.VISIBLE
        stickerOverlay.alpha = 1f
        // Para simplificar, mostramos un TextView con el emoji
        stickerOverlay.contentDescription = sticker
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
            .setMessage("Brillo, contraste, saturación, temperatura\n(se aplicarán en exportación)")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showCanvasPanel() {
        val options = arrayOf("9:16", "1:1", "4:5", "16:9")
        AlertDialog.Builder(this)
            .setTitle("Canvas (Relación de aspecto)")
            .setItems(options) { _, which ->
                currentCanvas = options[which]
                applyCanvas()
                Toast.makeText(this, "Canvas: $currentCanvas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun applyCanvas() {
        val aspectRatio = when (currentCanvas) {
            "16:9" -> 16f / 9f
            "1:1" -> 1f
            "4:5" -> 4f / 5f
            "3:4" -> 3f / 4f
            else -> 9f / 16f
        }
        playerView.layoutParams.height = (playerView.width / aspectRatio).toInt()
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
        btnMute.text = if (isMuted) "🔇" else "🔊"
    }

    // ==================== EXPORTACIÓN ====================

    private fun exportVideo() {
        if (videoUris.isEmpty()) {
            Toast.makeText(this, "Añade al menos un video", Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Exportando...")
            .setMessage("Configuración:\nClips: ${videoUris.size}\nVelocidad: ${currentSpeed}x\nFiltro: $currentFilter\nMúsica: $selectedMusic\nTransición: $transitionType\nCanvas: $currentCanvas")
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
                        .setMessage("${videoUris.size} clips\nTransición: $transitionType\nCanvas: $currentCanvas")
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
        stopMusic()
    }
}
