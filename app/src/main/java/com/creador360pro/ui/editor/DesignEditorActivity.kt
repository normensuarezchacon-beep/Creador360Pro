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
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        var sampleSize = 1
        while (options.outWidth / sampleSize > maxWidth || options.outHeight / sampleSize > maxHeight) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        contentResolver.openInputStream(uri)?.use {
            return BitmapFactory.decodeStream(it, null, decodeOptions)
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
        val templates = arrayOf(
            "Post Instagram", "Story Instagram", "Miniatura YouTube", "Flyer promocional",
            "Felicitación", "Cita motivacional", "Logo circular", "Banner horizontal",
            "Menú restaurante", "Invitación evento", "Oferta flash", "Tarjeta presentación"
        )
        AlertDialog.Builder(this)
            .setTitle("Cargar plantilla")
            .setItems(templates) { _, which -> loadTemplate(which) }
            .show()
    }

    private fun loadTemplate(index: Int) {
        canvasView.clearLayers()
        canvasView.addLayer(DesignLayer(type = LayerType.BACKGROUND, color = "#FFFFFF"))
        when (index) {
            0 -> {
                canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 50f, y = 50f, width = 980f, height = 980f, color = "#F5F5F5"))
                canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Tu título aquí", x = 100f, y = 200f, textSize = 60f, color = "#333333", fontName = "Montserrat"))
                canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Subtítulo", x = 100f, y = 300f, textSize = 35f, color = "#888888", fontName = "Open Sans"))
            }
            1 -> {
                canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1080f, height = 1920f, color = "#8B5CF6"))
                canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 340f, y = 500f, width = 400f, height = 400f, color = "#FFFFFF"))
                canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "¡Nuevo!", x = 300f, y = 1100f, textSize = 70f, color = "#FFFFFF", fontName = "Bebas Neue"))
            }
            2 -> {
                canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 0f, y = 0f, width = 1280f, height = 720f, color = "#1A1A1A"))
                canvasView.addLayer(DesignLayer(type = LayerType.RECTANGLE, x = 40f, y = 40f, width = 1200f, height = 640f, color = "#2A2A2A"))
                canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "TÍTULO DEL VIDEO", x = 100f, y = 300f, textSize = 80f, color = "#FFFFFF", fontName = "Oswald"))
                canvasView.addLayer(DesignLayer(type = LayerType.CIRCLE, x = 1000f, y = 500f, width = 150f, height = 150f, color = "#FF0000"))
            }
            else -> {
                canvasView.addLayer(DesignLayer(type = LayerType.TEXT, text = "Nueva plantilla", x = 200f, y = 400f, textSize = 50f, color = "#333333", fontName = "Montserrat"))
            }
        }
        updateLayersList()
        canvasView.invalidate()
        Toast.makeText(this, "Plantilla cargada", Toast.LENGTH_SHORT).show()
    }

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
    
