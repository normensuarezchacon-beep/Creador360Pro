package com.creador360pro.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.creador360pro.R
import com.creador360pro.data.db.AppDatabase
import com.creador360pro.data.model.DesignProject
import com.creador360pro.util.FilterType
import com.creador360pro.util.FontManager
import com.creador360pro.util.ImageFilterUtil
import com.creador360pro.util.TFLiteHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DesignEditorActivity : AppCompatActivity() {

    private lateinit var canvasView: CanvasView
    private lateinit var llCapas: LinearLayout
    private var selectedLayerIndex = -1
    private var currentProjectId: Long? = null
    private val imageFiles = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_design_editor)
        canvasView = findViewById(R.id.canvasView)
        llCapas = findViewById(R.id.llCapas)
        TFLiteHelper.initialize(this)
        setupToolbar()
        setupCanvas()

        currentProjectId = intent.getLongExtra("project_id", -1).takeIf { it != -1L }
        if (currentProjectId != null) {
            loadProject(currentProjectId!!)
        }
    }

    private fun setupToolbar() {
        findViewById<Button>(R.id.btnAdd).setOnClickListener { showAddMenu() }
        findViewById<Button>(R.id.btnLayers).setOnClickListener {
            if (llCapas.visibility == View.VISIBLE) {
                llCapas.visibility = View.GONE
            } else {
                llCapas.visibility = View.VISIBLE
                updateLayersList()
            }
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveProject() }
        findViewById<Button>(R.id.btnExport).setOnClickListener { exportImage() }
        findViewById<Button>(R.id.btnUndo).setOnClickListener {
            canvasView.undo()
            updateLayersList()
        }
        findViewById<Button>(R.id.btnRedo).setOnClickListener {
            canvasView.redo()
            updateLayersList()
        }
        findViewById<Button>(R.id.btnProperties).setOnClickListener {
            val index = canvasView.getSelectedLayerIndex()
            if (index >= 0 && index < canvasView.getLayersList().size) {
                showLayerProperties(canvasView.getLayersList()[index])
            } else {
                Toast.makeText(this, "Selecciona una capa primero", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnTemplates).setOnClickListener { showTemplates() }
        findViewById<Button>(R.id.btnLoadProject).setOnClickListener { showProjectList() }
    }

    private fun setupCanvas() {
        canvasView.addLayer(DesignLayer(type = LayerType.BACKGROUND, color = "#FFFFFF"))
        updateLayersList()
    }

    private fun showAddMenu() {
        val options = arrayOf("Texto", "Imagen desde galería", "Forma (círculo)", "Forma (rectángulo)")
        AlertDialog.Builder(this)
            .setTitle("Añadir elemento")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> addTextLayer()
                    1 -> addImageLayer()
                    2 -> addShapeLayer("circle")
                    3 -> addShapeLayer("rectangle")
                }
            }
            .show()
    }

    private fun addTextLayer() {
        val input = EditText(this)
        input.hint = "Escribe tu texto"
        input.setTextColor(Color.BLACK)
        AlertDialog.Builder(this)
            .setTitle("Añadir texto")
            .setView(input)
            .setPositiveButton("Añadir") { _, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    canvasView.saveState()
                    canvasView.addLayer(
                        DesignLayer(
                            type = LayerType.TEXT,
                            text = text,
                            x = 100f,
                            y = 200f,
                            color = "#000000",
                            textSize = 40f,
                            fontName = "Montserrat"
                        )
                    )
                    updateLayersList()
                    canvasView.invalidate()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun addImageLayer() {
        startActivityForResult(
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            100
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                try {
                    val bitmap = loadSampledBitmap(uri, 1200, 1200)
                    val fileName = "img_${System.currentTimeMillis()}.png"
                    val file = File(filesDir, fileName)
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
                    }
                    imageFiles.add(file)
                    canvasView.saveState()
                    canvasView.addLayer(
                        DesignLayer(
                            type = LayerType.IMAGE,
                            bitmap = bitmap,
                            x = 150f,
                            y = 150f,
                            width = bitmap.width.toFloat(),
                            height = bitmap.height.toFloat(),
                            imagePath = file.absolutePath
                        )
                    )
                    updateLayersList()
                    canvasView.invalidate()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadSampledBitmap(uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream, null, options)
    }
    var sampleSize = 1
    while (options.outWidth / sampleSize > maxWidth || options.outHeight / sampleSize > maxHeight) {
        sampleSize *= 2
    }
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    contentResolver.openInputStream(uri)?.use { inputStream ->
        val bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        if (bitmap != null) {
            return bitmap
        }
    }
    throw Exception("No se pudo cargar la imagen")
    }

    private fun addShapeLayer(shape: String) {
        canvasView.saveState()
        canvasView.addLayer(
            DesignLayer(
                type = if (shape == "circle") LayerType.CIRCLE else LayerType.RECTANGLE,
                x = 200f,
                y = 200f,
                width = 150f,
                height = if (shape == "circle") 150f else 100f,
                color = "#8B5CF6"
            )
        )
        updateLayersList()
        canvasView.invalidate()
    }

    private fun updateLayersList() {
        val container = findViewById<LinearLayout>(R.id.llCapasContainer)
        container.removeAllViews()
        canvasView.getLayersList().forEachIndexed { index, layer ->
            val layerView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8)
                val textView = TextView(context).apply {
                    text = when (layer.type) {
                        LayerType.TEXT -> "T: ${layer.text?.take(12) ?: ""}"
                        LayerType.IMAGE -> "Imagen"
                        LayerType.CIRCLE -> "Círculo"
                        LayerType.RECTANGLE -> "Rectángulo"
                        LayerType.BACKGROUND -> "Fondo"
                    }
                    setTextColor(Color.WHITE)
                    setPadding(16)
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val editButton = Button(context).apply {
                    text = "✏️"
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        canvasView.selectLayer(index)
                        selectedLayerIndex = index
                        showLayerProperties(layer)
                    }
                }
                setOnClickListener {
                    canvasView.selectLayer(index)
                    selectedLayerIndex = index
                    updateLayersList()
                }
                if (index == canvasView.getSelectedLayerIndex()) {
                    setBackgroundColor(Color.parseColor("#8B5CF6"))
                } else {
                    setBackgroundColor(Color.DKGRAY)
                }
                addView(textView)
                addView(editButton)
            }
            container.addView(layerView)
        }
    }

    private fun showLayerProperties(layer: DesignLayer) {
        when (layer.type) {
            LayerType.TEXT -> showTextProperties(layer)
            LayerType.IMAGE -> showImageProperties(layer)
            else -> showShapeProperties(layer)
        }
    }

    private fun showTextProperties(layer: DesignLayer) {
        AlertDialog.Builder(this)
            .setTitle("Propiedades de texto")
            .setItems(
                arrayOf("Cambiar texto", "Cambiar fuente", "Cambiar color", "Cambiar tamaño")
            ) { _, which ->
                when (which) {
                    0 -> changeText(layer)
                    1 -> changeFont(layer)
                    2 -> changeColor(layer)
                    3 -> changeSize(layer)
                }
            }
            .show()
    }

    private fun changeText(layer: DesignLayer) {
        val input = EditText(this)
        input.setText(layer.text)
        AlertDialog.Builder(this)
            .setTitle("Editar texto")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                canvasView.saveState()
                layer.text = input.text.toString()
                canvasView.invalidate()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun changeFont(layer: DesignLayer) {
        val fonts = FontManager.getAvailableFonts().toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Elegir fuente")
            .setItems(fonts) { _, which ->
                canvasView.saveState()
                layer.fontName = fonts[which]
                canvasView.invalidate()
            }
            .show()
    }

    private fun changeColor(layer: DesignLayer) {
        val hexCodes = arrayOf("#000000", "#FFFFFF", "#FF0000", "#0000FF", "#00FF00", "#FF9800", "#8B5CF6")
        AlertDialog.Builder(this)
            .setTitle("Color de texto")
            .setItems(arrayOf("Negro", "Blanco", "Rojo", "Azul", "Verde", "Naranja", "Púrpura")) { _, which ->
                canvasView.saveState()
                layer.color = hexCodes[which]
                canvasView.invalidate()
            }
            .show()
    }

    private fun changeSize(layer: DesignLayer) {
        val sizeValues = floatArrayOf(20f, 40f, 60f, 80f)
        AlertDialog.Builder(this)
            .setTitle("Tamaño de texto")
            .setItems(
                arrayOf("Pequeño (20)", "Mediano (40)", "Grande (60)", "Muy grande (80)")
            ) { _, which ->
                canvasView.saveState()
                layer.textSize = sizeValues[which]
                canvasView.invalidate()
            }
            .show()
    }

    private fun showImageProperties(layer: DesignLayer) {
        AlertDialog.Builder(this)
            .setTitle("Propiedades de imagen")
            .setItems(
                arrayOf("Aplicar filtro", "Eliminar fondo (IA)", "Ajustar brillo/contraste")
            ) { _, which ->
                when (which) {
                    0 -> applyFilter(layer)
                    1 -> removeBackground(layer)
                    2 -> adjustBrightness(layer)
                }
            }
            .show()
    }

    private fun applyFilter(layer: DesignLayer) {
        val filters = FilterType.values().map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Aplicar filtro")
            .setItems(filters) { _, which ->
                layer.bitmap?.let { bmp ->
                    canvasView.saveState()
                    val filtered = ImageFilterUtil.applyFilter(bmp, FilterType.values()[which])
                    layer.bitmap = filtered
                    layer.width = filtered.width.toFloat()
                    layer.height = filtered.height.toFloat()
                    canvasView.invalidate()
                }
            }
            .show()
    }

    private fun removeBackground(layer: DesignLayer) {
        if (!TFLiteHelper.isAvailable()) {
            AlertDialog.Builder(this)
                .setTitle("Función no disponible")
                .setMessage("No se encontró ningún modelo de IA.")
                .setPositiveButton("Entendido", null)
                .show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Eliminar fondo con IA")
            .setMessage("Tu dispositivo:\n${TFLiteHelper.getDeviceInfo(this)}\n\n¿Continuar?")
            .setPositiveButton("Sí") { _, _ -> executeBackgroundRemoval(layer) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun executeBackgroundRemoval(layer: DesignLayer) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Procesando...")
            .setMessage("Eliminando fondo con IA...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        Thread {
            layer.bitmap?.let { bmp ->
                val result = TFLiteHelper.removeBackground(bmp)
                runOnUiThread {
                    progressDialog.dismiss()
                    if (result != null) {
                        canvasView.saveState()
                        layer.bitmap = result
                        layer.width = result.width.toFloat()
                        layer.height = result.height.toFloat()
                        canvasView.invalidate()
                        Toast.makeText(this, "¡Fondo eliminado!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun adjustBrightness(layer: DesignLayer) {
        AlertDialog.Builder(this)
            .setTitle("Ajustar brillo")
            .setMessage("Próximamente")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showShapeProperties(layer: DesignLayer) {
        val hexCodes = arrayOf("#8B5CF6", "#FF0000", "#0000FF", "#00FF00", "#FF9800", "#000000")
        AlertDialog.Builder(this)
            .setTitle("Color de forma")
            .setItems(arrayOf("Púrpura", "Rojo", "Azul", "Verde", "Naranja", "Negro")) { _, which ->
                canvasView.saveState()
                layer.color = hexCodes[which]
                canvasView.invalidate()
            }
            .show()
    }

    private fun showTemplates() {
    val categorias = arrayOf("📱 Redes Sociales", "🎬 YouTube", "📢 Negocios", "🎉 Eventos", "🍽️ Restaurantes", "🎨 Diseño")
    AlertDialog.Builder(this)
        .setTitle("Cargar plantilla (30 diseños)")
        .setItems(categorias) { _, catIndex ->
            showTemplatesByCategory(catIndex)
        }
        .setNegativeButton("Cancelar", null)
        .show()
}

private fun showTemplatesByCategory(category: Int) {
    val templates = when (category) {
        0 -> arrayOf("Post Instagram 4:5", "Story Instagram 9:16", "Post Minimalista", "Post Colorido", "Quote del Día")
        1 -> arrayOf("Miniatura YouTube 16:9", "Miniatura Gaming", "Miniatura Vlog", "Miniatura Tutorial", "Miniatura Tech")
        2 -> arrayOf("Flyer Promocional", "Oferta Flash", "Banner Horizontal", "Tarjeta Presentación", "Anuncio Negocio")
        3 -> arrayOf("Invitación Fiesta", "Felicitación Cumpleaños", "Evento Especial", "Concierto", "Boda")
        4 -> arrayOf("Menú Restaurante", "Menú Cafetería", "Promo Comida", "Carta de Precios", "Especial del Día")
        5 -> arrayOf("Logo Circular", "Logo Moderno", "Diseño Abstracto", "Diseño Geométrico", "Diseño Degradado")
        else -> arrayOf("Post Instagram 4:5", "Story Instagram 9:16", "Miniatura YouTube 16:9", "Flyer Promocional", "Menú Restaurante")
    }

    AlertDialog.Builder(this)
        .setTitle("Plantillas disponibles")
        .setItems(templates) { _, templateIndex ->
            val templateId = category * 5 + templateIndex
            loadTemplate(templateId)
        }
        .setNegativeButton("Cancelar", null)
        .show()
}

private fun loadTemplate(index: Int) {
    canvasView.clearLayers()
    canvasView.addLayer(DesignLayer(type = LayerType.BACKGROUND, color = "#FFFFFF"))

    when (index) {
        0 -> loadPostInstagram()
        1 -> loadStoryInstagram()
        2 -> loadPostMinimalista()
        3 -> loadPostColorido()
        4 -> loadQuoteDelDia()
        5 -> loadMiniaturaYouTube()
        6 -> loadMiniaturaGaming()
        7 -> loadMiniaturaVlog()
        8 -> loadMiniaturaTutorial()
        9 -> loadMiniaturaTech()
        10 -> loadFlyerPromocional()
        11 -> loadOfertaFlash()
        12 -> loadBannerHorizontal()
        13 -> loadTarjetaPresentacion()
        14 -> loadAnuncioNegocio()
        15 -> loadInvitacionFiesta()
        16 -> loadFelicitacionCumpleanos()
        17 -> loadEventoEspecial()
        18 -> loadConcierto()
        19 -> loadBoda()
        20 -> loadMenuRestaurante()
        21 -> loadMenuCafeteria()
        22 -> loadPromoComida()
        23 -> loadCartaPrecios()
        24 -> loadEspecialDelDia()
        25 -> loadLogoCircular()
        26 -> loadLogoModerno()
        27 -> loadDisenoAbstracto()
        28 -> loadDisenoGeometrico()
        29 -> loadDisenoDegradado()
        else -> loadPostInstagram()
    }

    updateLayersList()
    canvasView.invalidate()
    Toast.makeText(this, "Plantilla cargada", Toast.LENGTH_SHORT).show()
}

private fun loadPostInstagram() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 40f, y = 40f, width = 1000f, height = 1270f, color = "#F5F5F5"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 80f, y = 80f, width = 920f, height = 600f, color = "#8B5CF6"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 700f, y = 500f, width = 200f, height = 200f, color = "#EC4899"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Título Impactante", x = 80f, y = 750f, textSize = 60f, color = "#333333", fontName = "Montserrat"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Descripción breve del contenido\nque quieres compartir hoy", x = 80f, y = 870f, textSize = 35f, color = "#888888", fontName = "Open Sans"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 80f, y = 1150f, width = 400f, height = 5f, color = "#8B5CF6"))
}

private fun loadStoryInstagram() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1080f, height = 1920f, color = "#1A1A1A"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 300f, y = 200f, width = 480f, height = 480f, color = "#8B5CF6"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 420f, y = 320f, width = 240f, height = 240f, color = "#EC4899"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "¡NUEVO!", x = 250f, y = 800f, textSize = 90f, color = "#FFFFFF", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Contenido exclusivo\npara tus seguidores", x = 200f, y = 1000f, textSize = 45f, color = "#CCCCCC", fontName = "Poppins"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 300f, y = 1400f, width = 480f, height = 8f, color = "#8B5CF6"))
}

private fun loadPostMinimalista() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 60f, y = 60f, width = 960f, height = 1230f, color = "#FFFFFF"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 300f, y = 300f, width = 480f, height = 480f, color = "#F3F4F6"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Minimalismo", x = 150f, y = 900f, textSize = 70f, color = "#1F2937", fontName = "Playfair Display"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Menos es más", x = 150f, y = 1000f, textSize = 40f, color = "#6B7280", fontName = "Poppins"))
}

private fun loadPostColorido() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 40f, y = 40f, width = 1000f, height = 1270f, color = "#FFF3E0"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 100f, y = 150f, width = 300f, height = 300f, color = "#FF6D00"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 600f, y = 300f, width = 250f, height = 250f, color = "#EC4899"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 150f, y = 600f, width = 780f, height = 5f, color = "#FF6D00"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "COLOR", x = 200f, y = 750f, textSize = 100f, color = "#FF6D00", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Diseño vibrante y creativo", x = 200f, y = 880f, textSize = 40f, color = "#333333", fontName = "Poppins"))
}

private fun loadQuoteDelDia() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 50f, y = 150f, width = 980f, height = 900f, color = "#F3E5F5"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "\u0022", x = 400f, y = 100f, textSize = 200f, color = "#8B5CF6", fontName = "Playfair Display"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "La creatividad\nno tiene límites", x = 150f, y = 450f, textSize = 60f, color = "#4A148C", fontName = "Playfair Display"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 350f, y = 900f, width = 380f, height = 5f, color = "#8B5CF6"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "- Autor desconocido", x = 350f, y = 950f, textSize = 30f, color = "#6B7280", fontName = "Poppins"))
}

private fun loadMiniaturaYouTube() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1280f, height = 720f, color = "#1A1A1A"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 30f, y = 30f, width = 1220f, height = 660f, color = "#2A2A2A"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 700f, y = 50f, width = 500f, height = 300f, color = "#8B5CF6"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 850f, y = 200f, width = 150f, height = 150f, color = "#EC4899"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "¡TÍTULO!", x = 80f, y = 300f, textSize = 90f, color = "#FFFFFF", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Subtítulo llamativo", x = 80f, y = 450f, textSize = 45f, color = "#CCCCCC", fontName = "Oswald"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 80f, y = 550f, width = 300f, height = 8f, color = "#FF0000"))
}

private fun loadMiniaturaGaming() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1280f, height = 720f, color = "#0D0D0D"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 40f, y = 40f, width = 1200f, height = 640f, color = "#1A1A1A"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 600f, y = 100f, width = 600f, height = 400f, color = "#8B5CF6"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "GAMEPLAY", x = 80f, y = 250f, textSize = 100f, color = "#FFFFFF", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "EPISODIO #1", x = 80f, y = 400f, textSize = 50f, color = "#8B5CF6", fontName = "Oswald"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 1100f, y = 50f, width = 120f, height = 120f, color = "#FF0000"))
}

private fun loadMiniaturaVlog() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1280f, height = 720f, color = "#FFF3E0"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 700f, y = 100f, width = 400f, height = 400f, color = "#FF9800"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "VLOG", x = 80f, y = 250f, textSize = 100f, color = "#1A1A1A", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Un día conmigo", x = 80f, y = 400f, textSize = 50f, color = "#FF6D00", fontName = "Poppins"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 80f, y = 550f, width = 400f, height = 6f, color = "#FF9800"))
}

private fun loadMiniaturaTutorial() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1280f, height = 720f, color = "#E8F5E9"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 700f, y = 80f, width = 500f, height = 350f, color = "#4CAF50"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "TUTORIAL", x = 80f, y = 250f, textSize = 90f, color = "#1A1A1A", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Paso a paso", x = 80f, y = 400f, textSize = 50f, color = "#2E7D32", fontName = "Poppins"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 80f, y = 550f, width = 500f, height = 6f, color = "#4CAF50"))
}

private fun loadMiniaturaTech() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1280f, height = 720f, color = "#0D1B2A"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 700f, y = 100f, width = 500f, height = 300f, color = "#1B4965"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "TECH", x = 80f, y = 250f, textSize = 100f, color = "#FFFFFF", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Review completo", x = 80f, y = 400f, textSize = 45f, color = "#5FA8D3", fontName = "Oswald"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 1100f, y = 500f, width = 130f, height = 130f, color = "#5FA8D3"))
}

private fun loadFlyerPromocional() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 40f, y = 40f, width = 1000f, height = 1270f, color = "#FFF3E0"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 100f, y = 100f, width = 880f, height = 400f, color = "#FF6D00"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "OFERTA", x = 200f, y = 300f, textSize = 100f, color = "#FFFFFF", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "50%", x = 350f, y = 550f, textSize = 120f, color = "#FF6D00", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Solo por tiempo limitado", x = 150f, y = 800f, textSize = 40f, color = "#333333", fontName = "Poppins"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 200f, y = 950f, width = 680f, height = 80f, color = "#1A1A1A"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "APROVECHA YA", x = 350f, y = 990f, textSize = 40f, color = "#FFFFFF", fontName = "Oswald"))
}

private fun loadOfertaFlash() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 50f, y = 50f, width = 980f, height = 500f, color = "#FFEB3B"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "¡FLASH!", x = 250f, y = 200f, textSize = 90f, color = "#000000", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Solo por 24 horas", x = 250f, y = 350f, textSize = 40f, color = "#333333", fontName = "Poppins"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 800f, y = 100f, width = 150f, height = 150f, color = "#FF0000"))
}

private fun loadBannerHorizontal() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1280f, height = 720f, color = "#263238"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 600f, y = 100f, width = 600f, height = 400f, color = "#8B5CF6"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "TU MARCA", x = 100f, y = 250f, textSize = 90f, color = "#FFFFFF", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Eslogan impactante", x = 100f, y = 400f, textSize = 40f, color = "#B0BEC5", fontName = "Poppins"))
}

private fun loadTarjetaPresentacion() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 50f, y = 50f, width = 1180f, height = 620f, color = "#FFFFFF"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 50f, y = 50f, width = 1180f, height = 8f, color = "#8B5CF6"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Nombre Apellido", x = 100f, y = 250f, textSize = 60f, color = "#333333", fontName = "Poppins"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Diseñador Gráfico", x = 100f, y = 350f, textSize = 35f, color = "#6B7280", fontName = "Open Sans"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "📱 +53 5XXXXXXX\n✉️ correo@email.com", x = 700f, y = 250f, textSize = 35f, color = "#333333", fontName = "Poppins"))
}

private fun loadAnuncioNegocio() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 40f, y = 40f, width = 1000f, height = 1270f, color = "#E8F5E9"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 100f, y = 100f, width = 880f, height = 350f, color = "#2E7D32"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "TU NEGOCIO", x = 150f, y = 280f, textSize = 70f, color = "#FFFFFF", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Productos de calidad", x = 150f, y = 600f, textSize = 45f, color = "#1A1A1A", fontName = "Poppins"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 150f, y = 800f, width = 780f, height = 200f, color = "#C8E6C9"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Precios especiales", x = 250f, y = 900f, textSize = 50f, color = "#2E7D32", fontName = "Oswald"))
}

private fun loadInvitacionFiesta() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 40f, y = 40f, width = 1000f, height = 1270f, color = "#FCE4EC"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 300f, y = 150f, width = 480f, height = 480f, color = "#EC4899"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "¡Te invitamos!", x = 200f, y = 750f, textSize = 70f, color = "#880E4F", fontName = "Playfair Display"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Sábado 15 • 8 PM", x = 250f, y = 900f, textSize = 45f, color = "#AD1457", fontName = "Poppins"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 300f, y = 1100f, width = 480f, height = 6f, color = "#EC4899"))
}

private fun loadFelicitacionCumpleanos() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1080f, height = 1080f, color = "#E8F5E9"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 200f, y = 150f, width = 680f, height = 680f, color = "#FFD54F"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "¡Feliz\nCumpleaños!", x = 150f, y = 400f, textSize = 80f, color = "#2E7D32", fontName = "Playfair Display"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "🎉🎂🎈", x = 350f, y = 750f, textSize = 60f, color = "#1A1A1A", fontName = "Poppins"))
}

private fun loadEventoEspecial() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 50f, y = 50f, width = 980f, height = 1250f, color = "#E3F2FD"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 100f, y = 150f, width = 880f, height = 400f, color = "#1976D2"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "EVENTO\nESPECIAL", x = 150f, y = 300f, textSize = 80f, color = "#FFFFFF", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Fecha y lugar", x = 150f, y = 700f, textSize = 50f, color = "#1A1A1A", fontName = "Poppins"))
}

private fun loadConcierto() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1080f, height = 1350f, color = "#1A1A1A"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 250f, y = 150f, width = 580f, height = 580f, color = "#8B5CF6"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "CONCIERTO", x = 200f, y = 900f, textSize = 80f, color = "#FFFFFF", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "🎸🎤🎹", x = 350f, y = 1050f, textSize = 60f, color = "#FFFFFF", fontName = "Poppins"))
}

private fun loadBoda() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 40f, y = 40f, width = 1000f, height = 1270f, color = "#FFF8E1"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Boda", x = 300f, y = 350f, textSize = 100f, color = "#8B5CF6", fontName = "Grand Hotel"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "María & José", x = 200f, y = 600f, textSize = 50f, color = "#6D28D9", fontName = "Playfair Display"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 300f, y = 900f, width = 480f, height = 4f, color = "#8B5CF6"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "15 de Febrero", x = 300f, y = 1000f, textSize = 40f, color = "#1A1A1A", fontName = "Poppins"))
}

private fun loadMenuRestaurante() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1080f, height = 1350f, color = "#FFF8E1"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "MENÚ", x = 350f, y = 150f, textSize = 90f, color = "#3E2723", fontName = "Playfair Display"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 100f, y = 300f, width = 880f, height = 4f, color = "#3E2723"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Entrada ......... $5\nPlato fuerte ... $12\nPostre ......... $4", x = 150f, y = 450f, textSize = 45f, color = "#4E342E", fontName = "Montserrat"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 100f, y = 900f, width = 880f, height = 4f, color = "#3E2723"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Abierto todos los días", x = 250f, y = 1000f, textSize = 35f, color = "#6B7280", fontName = "Poppins"))
}

private fun loadMenuCafeteria() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1080f, height = 1350f, color = "#EFEBE9"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 300f, y = 150f, width = 480f, height = 480f, color = "#8D6E63"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "CAFÉ", x = 350f, y = 750f, textSize = 80f, color = "#4E342E", fontName = "Playfair Display"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Americano .... $2\nCappuccino .. $3\nLatte ......... $3.5", x = 150f, y = 950f, textSize = 40f, color = "#4E342E", fontName = "Montserrat"))
}

private fun loadPromoComida() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 30f, y = 30f, width = 1020f, height = 1290f, color = "#FFEBEE"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 100f, y = 100f, width = 880f, height = 350f, color = "#D32F2F"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "¡2x1!", x = 250f, y = 300f, textSize = 100f, color = "#FFFFFF", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "En todas las pizzas", x = 150f, y = 600f, textSize = 50f, color = "#1A1A1A", fontName = "Poppins"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "🍕🍕🍕", x = 350f, y = 800f, textSize = 80f, color = "#D32F2F", fontName = "Poppins"))
}

private fun loadCartaPrecios() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 50f, y = 50f, width = 980f, height = 1250f, color = "#FFFFFF"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "PRECIOS", x = 250f, y = 150f, textSize = 80f, color = "#1F2937", fontName = "Bebas Neue"))
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 100f, y = 280f, width = 880f, height = 3f, color = "#1F2937"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Servicio A ... $10\nServicio B ... $20\nServicio C ... $30", x = 150f, y = 400f, textSize = 45f, color = "#333333", fontName = "Montserrat"))
}

private fun loadEspecialDelDia() {
    canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 40f, y = 40f, width = 1000f, height = 1270f, color = "#FFF3E0"))
    canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 250f, y = 150f, width = 580f, height = 580f, color = "#FF9800"))
    canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "ESPECIAL", x = 200f, y = 850f, textSize = 70f, color = "#E65100", fontName = "Bebas Neue"))
    canvasView.addLayer(
   
        private fun saveProject() {
        val jsonCapas = canvasView.toJson()
        val projectName = "Diseño_${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())}"
        val project = DesignProject(
            nombre = projectName,
            fechaCreacion = System.currentTimeMillis(),
            fechaModificacion = System.currentTimeMillis(),
            jsonCapas = jsonCapas,
            anchoLienzo = 1080,
            altoLienzo = 1080
        )
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@DesignEditorActivity)
            withContext(Dispatchers.IO) {
                if (currentProjectId != null) {
                    val existing = db.designProjectDao().getProjectById(currentProjectId!!)
                    if (existing != null) {
                        db.designProjectDao().updateProject(
                            existing.copy(
                                nombre = projectName,
                                fechaModificacion = System.currentTimeMillis(),
                                jsonCapas = jsonCapas
                            )
                        )
                    }
                } else {
                    val id = db.designProjectDao().insertProject(project)
                    currentProjectId = id
                }
            }
            Toast.makeText(this@DesignEditorActivity, "¡Proyecto guardado!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadProject(projectId: Long) {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@DesignEditorActivity)
            val project = withContext(Dispatchers.IO) {
                db.designProjectDao().getProjectById(projectId)
            }
            project?.let {
                canvasView.fromJson(it.jsonCapas)
                updateLayersList()
                canvasView.invalidate()
                Toast.makeText(this@DesignEditorActivity, "Proyecto cargado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showProjectList() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val projects = withContext(Dispatchers.IO) {
                var list = listOf<DesignProject>()
                db.designProjectDao().getAllProjects().collect { list = it }
                list
            }

            if (projects.isEmpty()) {
                AlertDialog.Builder(this@DesignEditorActivity)
                    .setTitle("Proyectos guardados")
                    .setMessage("No hay proyectos guardados.")
                    .setPositiveButton("OK", null)
                    .show()
                return@launch
            }

            val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            val nombres = projects.map {
                "${it.nombre} (${sdf.format(Date(it.fechaModificacion))})"
            }.toTypedArray()

            AlertDialog.Builder(this@DesignEditorActivity)
                .setTitle("Cargar proyecto")
                .setItems(nombres) { _, which ->
                    currentProjectId = projects[which].id
                    loadProject(projects[which].id)
                }
                .setPositiveButton("Eliminar todos") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            projects.forEach { db.designProjectDao().deleteProject(it) }
                        }
                        Toast.makeText(this@DesignEditorActivity, "Proyectos eliminados", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun exportImage() {
        AlertDialog.Builder(this)
            .setTitle("Exportar diseño")
            .setItems(arrayOf("1080 x 1080 (Post)", "1080 x 1920 (Story)", "1280 x 720 (YouTube)")) { _, which ->
                exportWithResolution(which)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportWithResolution(resolutionIndex: Int) {
        AlertDialog.Builder(this)
            .setTitle("Formato")
            .setItems(arrayOf("JPG (comprimido)", "PNG (alta calidad)")) { _, formatIndex ->
                exportFinalImage(resolutionIndex, formatIndex)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportFinalImage(resolutionIndex: Int, formatIndex: Int) {
        try {
            val widths = intArrayOf(1080, 1080, 1280)
            val heights = intArrayOf(1080, 1920, 720)
            val bitmap = canvasView.exportToBitmap(widths[resolutionIndex], heights[resolutionIndex])
            val extension = if (formatIndex == 0) "jpg" else "png"
            val format = if (formatIndex == 0) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
            val quality = if (formatIndex == 0) 85 else 100
            val path = MediaStore.Images.Media.insertImage(
                contentResolver,
                bitmap,
                "Creador360_${System.currentTimeMillis()}.$extension",
                "Creado con Creador360 PRO"
            )
            if (path != null && path.isNotEmpty()) {
                Toast.makeText(this, "¡Guardado como $extension!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
    
