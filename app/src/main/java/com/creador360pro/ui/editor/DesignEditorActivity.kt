package com.creador360pro.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.creador360pro.R
import java.io.ByteArrayOutputStream

class DesignEditorActivity : AppCompatActivity() {

    private lateinit var canvasView: CanvasView
    private lateinit var llToolbar: LinearLayout
    private lateinit var llCapas: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_design_editor)

        canvasView = findViewById(R.id.canvasView)
        llToolbar = findViewById(R.id.llToolbar)
        llCapas = findViewById(R.id.llCapas)

        setupToolbar()
        setupCanvas()
    }

    private fun setupToolbar() {
        // Botón Añadir
        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            showAddMenu()
        }

        // Botón Capas
        findViewById<Button>(R.id.btnLayers).setOnClickListener {
            if (llCapas.visibility == View.VISIBLE) {
                llCapas.visibility = View.GONE
            } else {
                llCapas.visibility = View.VISIBLE
                updateLayersList()
            }
        }

        // Botón Guardar
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveProject()
        }

        // Botón Exportar
        findViewById<Button>(R.id.btnExport).setOnClickListener {
            exportImage()
        }

        // Botón Deshacer
        findViewById<Button>(R.id.btnUndo).setOnClickListener {
            canvasView.undo()
            updateLayersList()
        }

        // Botón Rehacer
        findViewById<Button>(R.id.btnRedo).setOnClickListener {
            canvasView.redo()
            updateLayersList()
        }
    }

    private fun setupCanvas() {
        // Añadir una capa inicial de fondo blanco
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
                            textSize = 40f
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
        val intent = android.content.Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
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
                        y = 150f
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
        llCapas.removeAllViews()

        canvasView.getLayers().forEachIndexed { index, layer ->
            val layerView = TextView(this).apply {
                text = when (layer.type) {
                    LayerType.TEXT -> "Texto: ${layer.text?.take(15) ?: ""}"
                    LayerType.IMAGE -> "Imagen"
                    LayerType.CIRCLE -> "Círculo"
                    LayerType.RECTANGLE -> "Rectángulo"
                    LayerType.BACKGROUND -> "Fondo"
                }
                setTextColor(Color.WHITE)
                setPadding(16)
                setBackgroundColor(if (index == canvasView.getSelectedLayerIndex()) Color.parseColor("#8B5CF6") else Color.DKGRAY)
                gravity = Gravity.CENTER_VERTICAL

                setOnClickListener {
                    canvasView.selectLayer(index)
                    updateLayersList()
                }
            }
            llCapas.addView(layerView)
        }
    }

    private fun saveProject() {
        Toast.makeText(this, "Proyecto guardado (próximamente)", Toast.LENGTH_SHORT).show()
    }

    private fun exportImage() {
        val bitmap = canvasView.exportToBitmap()
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)

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
}
