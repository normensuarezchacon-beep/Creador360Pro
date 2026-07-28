package com.creador360pro.ui.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val layers = mutableListOf<DesignLayer>()
    private var selectedLayerIndex = -1
    private val undoStack = mutableListOf<MutableList<DesignLayer>>()
    private val redoStack = mutableListOf<MutableList<DesignLayer>>()

    private var isDragging = false
    private var lastX = 0f
    private var lastY = 0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (selectedLayerIndex >= 0 && selectedLayerIndex < layers.size) {
                val layer = layers[selectedLayerIndex]
                layer.width *= detector.scaleFactor
                layer.height *= detector.scaleFactor
                invalidate()
            }
            return true
        }
    })

    fun addLayer(layer: DesignLayer) {
        saveState()
        layers.add(layer)
        selectedLayerIndex = layers.size - 1
        invalidate()
    }

    fun getLayers(): List<DesignLayer> = layers.toList()

    fun getSelectedLayerIndex(): Int = selectedLayerIndex

    fun selectLayer(index: Int) {
        if (index in layers.indices) {
            selectedLayerIndex = index
            invalidate()
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(layers.toMutableList())
            layers.clear()
            layers.addAll(undoStack.removeAt(undoStack.size - 1))
            if (selectedLayerIndex >= layers.size) selectedLayerIndex = layers.size - 1
            invalidate()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(layers.toMutableList())
            layers.clear()
            layers.addAll(redoStack.removeAt(redoStack.size - 1))
            if (selectedLayerIndex >= layers.size) selectedLayerIndex = layers.size - 1
            invalidate()
        }
    }

    fun exportToBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        drawLayers(canvas)
        return bitmap
    }

    private fun saveState() {
        undoStack.add(layers.toMutableList())
        redoStack.clear()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#F0F0F0"))
        drawGrid(canvas)
        drawLayers(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }
        for (i in 0..width step 50) {
            canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
        }
        for (i in 0..height step 50) {
            canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
        }
    }

    private fun drawLayers(canvas: Canvas) {
        layers.forEachIndexed { index, layer ->
            when (layer.type) {
                LayerType.BACKGROUND -> {
                    val paint = Paint().apply {
                        color = Color.parseColor(layer.color)
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                }
                LayerType.TEXT -> {
                    val paint = Paint().apply {
                        color = Color.parseColor(layer.color)
                        textSize = layer.textSize
                        isAntiAlias = true
                    }
                    canvas.drawText(layer.text ?: "", layer.x, layer.y + layer.textSize, paint)
                }
                LayerType.IMAGE -> {
                    layer.bitmap?.let { bmp ->
                        canvas.drawBitmap(
                            bmp,
                            null,
                            RectF(layer.x, layer.y, layer.x + layer.width, layer.y + layer.height),
                            null
                        )
                    }
                }
                LayerType.CIRCLE -> {
                    val paint = Paint().apply {
                        color = Color.parseColor(layer.color)
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawCircle(
                        layer.x + layer.width / 2,
                        layer.y + layer.height / 2,
                        layer.width / 2,
                        paint
                    )
                }
                LayerType.RECTANGLE -> {
                    val paint = Paint().apply {
                        color = Color.parseColor(layer.color)
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(layer.x, layer.y, layer.x + layer.width, layer.y + layer.height, paint)
                }
            }

            // Dibujar borde de selección
            if (index == selectedLayerIndex) {
                val paint = Paint().apply {
                    color = Color.parseColor("#8B5CF6")
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawRect(
                    layer.x - 5, layer.y - 5,
                    layer.x + layer.width + 5, layer.y + layer.height + 5,
                    paint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Buscar capa tocada (en orden inverso para seleccionar la de arriba)
                for (i in layers.size - 1 downTo 0) {
                    val layer = layers[i]
                    if (event.x >= layer.x && event.x <= layer.x + layer.width &&
                        event.y >= layer.y && event.y <= layer.y + layer.height) {
                        selectedLayerIndex = i
                        isDragging = true
                        lastX = event.x
                        lastY = event.y
                        invalidate()
                        return true
                    }
                }
                selectedLayerIndex = -1
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && selectedLayerIndex >= 0) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    val layer = layers[selectedLayerIndex]
                    layer.x += dx
                    layer.y += dy
                    lastX = event.x
                    lastY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                isDragging = false
            }
        }
        return true
    }
}
