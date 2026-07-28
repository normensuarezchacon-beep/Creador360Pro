package com.creador360pro.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.creador360pro.R
import com.creador360pro.util.FilterType
import com.creador360pro.util.FontManager
import com.creador360pro.util.ImageFilterUtil
import com.creador360pro.util.TFLiteHelper
import java.io.ByteArrayOutputStream

class DesignEditorActivity : AppCompatActivity() {

    private lateinit var canvasView: CanvasView
    private lateinit var llCapas: LinearLayout
    private var selectedLayerIndex = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_design_editor)

        canvasView = findViewById(R.id.canvasView)
        llCapas = findViewById(R.id.llCapas)

        TFLiteHelper.initialize(this)
        setupToolbar()
        setupCanvas()
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
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                canvasView.addLayer(
                    DesignLayer(
                        type = LayerType.IMAGE,
                        bitmap = bitmap,
                        x = 150f,
                        y = 150f,
                        width = bitmap.width.toFloat(),
                        height = bitmap.height.toFloat()
                    )
                )
                updateLayersList()
                canvasView.invalidate()
            }
        }
    }

    private fun addShapeLayer(shape: String) {
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

        canvasView.getLayers().forEachIndexed { index, layer ->
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
        val options = arrayOf("Cambiar texto", "Cambiar fuente", "Cambiar color", "Cambiar tamaño")
        AlertDialog.Builder(this)
            .setTitle("Propiedades de texto")
            .setItems(options) { _, which ->
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
                layer.fontName = fonts[which]
                canvasView.invalidate()
                Toast.makeText(this, "Fuente: ${fonts[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun changeColor(layer: DesignLayer) {
        val colors = arrayOf("Negro", "Blanco", "Rojo", "Azul", "Verde", "Naranja", "Púrpura")
        val hexCodes = arrayOf("#000000", "#FFFFFF", "#FF0000", "#0000FF", "#00FF00", "#FF9800", "#8B5CF6")
        AlertDialog.Builder(this)
            .setTitle("Color de texto")
            .setItems(colors) { _, which ->
                layer.color = hexCodes[which]
                canvasView.invalidate()
            }
            .show()
    }

    private fun changeSize(layer: DesignLayer) {
        val sizes = arrayOf("Pequeño (20)", "Mediano (40)", "Grande (60)", "Muy grande (80)")
        val sizeValues = floatArrayOf(20f, 40f, 60f, 80f)
        AlertDialog.Builder(this)
            .setTitle("Tamaño de texto")
            .setItems(sizes) { _, which ->
                layer.textSize = sizeValues[which]
                canvasView.invalidate()
            }
            .show()
    }

    private fun showImageProperties(layer: DesignLayer) {
        val options = arrayOf("Aplicar filtro", "Eliminar fondo (IA)", "Ajustar brillo/contraste")
        AlertDialog.Builder(this)
            .setTitle("Propiedades de imagen")
            .setItems(options) { _, which ->
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
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Procesando...")
            .setMessage("Eliminando fondo con IA. Esto puede tardar unos segundos.")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            layer.bitmap?.let { bmp ->
                val result = TFLiteHelper.removeBackground(bmp)
                runOnUiThread {
                    progressDialog.dismiss()
                    if (result != null) {
                        layer.bitmap = result
                        layer.width = result.width.toFloat()
                        layer.height = result.height.toFloat()
                        canvasView.invalidate()
                        Toast.makeText(this, "Fondo eliminado", Toast.LENGTH_SHORT).show()
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
            .setMessage("Próximamente: control deslizante de brillo y contraste")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showShapeProperties(layer: DesignLayer) {
        val colors = arrayOf("Púrpura", "Rojo", "Azul", "Verde", "Naranja", "Negro")
        val hexCodes = arrayOf("#8B5CF6", "#FF0000", "#0000FF", "#00FF00", "#FF9800", "#000000")
        AlertDialog.Builder(this)
            .setTitle("Color de forma")
            .setItems(colors) { _, which ->
                layer.color = hexCodes[which]
                canvasView.invalidate()
            }
            .show()
    }

    private fun saveProject() {
        Toast.makeText(this, "Proyecto guardado (próximamente)", Toast.LENGTH_SHORT).show()
    }

    private fun exportImage() {
        val bitmap = canvasView.exportToBitmap()
        val path = MediaStore.Images.Media.insertImage(
            contentResolver,
            bitmap,
            "Creador360_${System.currentTimeMillis()}",
            "Diseño creado con Creador360 PRO"
        )
        if (path != null) {
            Toast.makeText(this, "¡Imagen guardada en la galería!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TFLiteHelper.close()
    }
}
