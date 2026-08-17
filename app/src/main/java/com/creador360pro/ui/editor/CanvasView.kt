package com.creador360pro.ui.editor

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.creador360pro.util.FontManager
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min

class CanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val layersList = mutableListOf<DesignLayer>()
    private var selectedLayerIndex = -1
    private val undoStack = mutableListOf<List<DesignLayer>>()
    private val redoStack = mutableListOf<List<DesignLayer>>()

    private var isDragging = false
    private var lastX = 0f
    private var lastY = 0f
    private var rotationStart = 0f

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (selectedLayerIndex >= 0 && selectedLayerIndex < layersList.size) {
                saveState()
                val layer = layersList[selectedLayerIndex]
                layer.width *= detector.scaleFactor
                layer.height *= detector.scaleFactor
                invalidate()
            }
            return true
        }
    })

    fun addLayer(layer: DesignLayer) {
        saveState()
        layersList.add(layer)
        selectedLayerIndex = layersList.size - 1
        invalidate()
    }

    fun getLayersList(): List<DesignLayer> = layersList.toList()

    fun getSelectedLayerIndex(): Int = selectedLayerIndex

    fun selectLayer(index: Int) {
        if (index in layersList.indices) {
            selectedLayerIndex = index
            invalidate()
        }
    }

    fun clearLayers() {
        saveState()
        layersList.clear()
        selectedLayerIndex = -1
        invalidate()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(layersList.map { it.copy() })
            layersList.clear()
            layersList.addAll(undoStack.removeAt(undoStack.size - 1))
            if (selectedLayerIndex >= layersList.size) selectedLayerIndex = layersList.size - 1
            invalidate()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(layersList.map { it.copy() })
            layersList.clear()
            layersList.addAll(redoStack.removeAt(redoStack.size - 1))
            if (selectedLayerIndex >= layersList.size) selectedLayerIndex = layersList.size - 1
            invalidate()
        }
    }

    fun duplicateLayer(index: Int) {
        if (index in layersList.indices) {
            saveState()
            val original = layersList[index]
            val copy = original.copy(x = original.x + 30f, y = original.y + 30f)
            layersList.add(index + 1, copy)
            selectedLayerIndex = index + 1
            invalidate()
        }
    }

    fun deleteLayer(index: Int) {
        if (index in layersList.indices) {
            saveState()
            layersList.removeAt(index)
            if (selectedLayerIndex >= layersList.size) selectedLayerIndex = layersList.size - 1
            invalidate()
        }
    }

    fun moveLayerUp(index: Int) {
        if (index in layersList.indices && index < layersList.size - 1) {
            saveState()
            val layer = layersList.removeAt(index)
            layersList.add(index + 1, layer)
            selectedLayerIndex = index + 1
            invalidate()
        }
    }

    fun moveLayerDown(index: Int) {
        if (index in layersList.indices && index > 0) {
            saveState()
            val layer = layersList.removeAt(index)
            layersList.add(index - 1, layer)
            selectedLayerIndex = index - 1
            invalidate()
        }
    }

    fun alignLayer(index: Int, alignment: String) {
        if (index in layersList.indices) {
            saveState()
            val layer = layersList[index]
            when (alignment) {
                "CENTER_H" -> layer.x = (width - layer.width) / 2
                "CENTER_V" -> layer.y = (height - layer.height) / 2
                "LEFT" -> layer.x = 0f
                "RIGHT" -> layer.x = width - layer.width
                "TOP" -> layer.y = 0f
                "BOTTOM" -> layer.y = height - layer.height
            }
            invalidate()
        }
    }

    fun setLayerRotation(index: Int, rotation: Float) {
        if (index in layersList.indices) {
            saveState()
            layersList[index].rotation = rotation
            invalidate()
        }
    }

    fun exportToBitmap(canvasWidth: Int, canvasHeight: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        drawLayers(canvas, canvasWidth, canvasHeight)
        return bitmap
    }

    fun exportToBitmap(): Bitmap = exportToBitmap(width, height)

    fun toJson(): String {
        val jsonArray = JSONArray()
        layersList.forEach { layer ->
            val json = JSONObject()
            json.put("type", layer.type.name)
            json.put("x", layer.x.toDouble())
            json.put("y", layer.y.toDouble())
            json.put("width", layer.width.toDouble())
            json.put("height", layer.height.toDouble())
            json.put("color", layer.color)
            json.put("textSize", layer.textSize.toDouble())
            json.put("rotation", layer.rotation.toDouble())
            layer.text?.let { json.put("text", it) }
            layer.fontName?.let { json.put("fontName", it) }
            layer.imagePath?.let { json.put("imagePath", it) }
            jsonArray.put(json)
        }
        return jsonArray.toString()
    }

    fun fromJson(jsonString: String) {
        layersList.clear()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val json = jsonArray.getJSONObject(i)
            val type = LayerType.valueOf(json.getString("type"))
            val layer = DesignLayer(
                type = type,
                x = json.getDouble("x").toFloat(),
                y = json.getDouble("y").toFloat(),
                width = json.getDouble("width").toFloat(),
                height = json.getDouble("height").toFloat(),
                color = json.optString("color", "#000000"),
                textSize = json.optDouble("textSize", 40.0).toFloat(),
                text = json.optString("text", null),
                fontName = json.optString("fontName", null),
                imagePath = json.optString("imagePath", null),
                rotation = json.optDouble("rotation", 0.0).toFloat()
            )
            layer.imagePath?.let { path ->
                val file = java.io.File(path)
                if (file.exists()) {
                    layer.bitmap = BitmapFactory.decodeFile(path)
                }
            }
            layersList.add(layer)
        }
        selectedLayerIndex = -1
        invalidate()
    }

    fun saveState() {
        undoStack.add(layersList.map { it.copy() })
        redoStack.clear()
    }

    fun recycleBitmaps() {
    layersList.forEach { layer ->
        layer.bitmap?.recycle()
        layer.bitmap = null
    }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#F0F0F0"))
        drawGrid(canvas)
        drawLayers(canvas, width, height)
    }

    private fun drawGrid(canvas: Canvas) {
        val paint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }
        for (i in 0..width step 50) canvas.drawLine(i.toFloat(), 0f, i.toFloat(), height.toFloat(), paint)
        for (i in 0..height step 50) canvas.drawLine(0f, i.toFloat(), width.toFloat(), i.toFloat(), paint)
    }

    private fun drawLayers(canvas: Canvas, canvasWidth: Int, canvasHeight: Int) {
        val scaleX = canvasWidth.toFloat() / width.toFloat()
        val scaleY = canvasHeight.toFloat() / height.toFloat()

        layersList.forEachIndexed { index, layer ->
            val scaledLayer = layer.copy(
                x = layer.x * scaleX,
                y = layer.y * scaleY,
                width = layer.width * scaleX,
                height = layer.height * scaleY,
                textSize = layer.textSize * scaleX
            )

            canvas.save()
            canvas.rotate(
                scaledLayer.rotation,
                scaledLayer.x + scaledLayer.width / 2,
                scaledLayer.y + scaledLayer.height / 2
            )

            when (scaledLayer.type) {
                LayerType.BACKGROUND -> {
                    val paint = Paint().apply { color = Color.parseColor(scaledLayer.color); style = Paint.Style.FILL }
                    canvas.drawRect(0f, 0f, canvasWidth.toFloat(), canvasHeight.toFloat(), paint)
                }
                LayerType.TEXT -> {
                    val paint = Paint().apply {
                        color = Color.parseColor(scaledLayer.color)
                        textSize = scaledLayer.textSize
                        isAntiAlias = true
                        if (scaledLayer.fontName != null) typeface = FontManager.getTypeface(context, scaledLayer.fontName!!)
                    }
                    canvas.drawText(scaledLayer.text ?: "", scaledLayer.x, scaledLayer.y + scaledLayer.textSize, paint)
                }
                LayerType.IMAGE -> {
                    scaledLayer.bitmap?.let { bmp ->
                        canvas.drawBitmap(bmp, null, RectF(scaledLayer.x, scaledLayer.y, scaledLayer.x + scaledLayer.width, scaledLayer.y + scaledLayer.height), null)
                    }
                }
                LayerType.CIRCLE -> {
                    val paint = Paint().apply { color = Color.parseColor(scaledLayer.color); style = Paint.Style.FILL; isAntiAlias = true }
                    val radius = min(scaledLayer.width, scaledLayer.height) / 2
                    canvas.drawCircle(scaledLayer.x + scaledLayer.width / 2, scaledLayer.y + scaledLayer.height / 2, radius, paint)
                }
                LayerType.RECTANGLE -> {
                    val paint = Paint().apply { color = Color.parseColor(scaledLayer.color); style = Paint.Style.FILL }
                    canvas.drawRect(scaledLayer.x, scaledLayer.y, scaledLayer.x + scaledLayer.width, scaledLayer.y + scaledLayer.height, paint)
                }
            }

            canvas.restore()

            if (index == selectedLayerIndex) {
                val paint = Paint().apply {
                    color = Color.parseColor("#8B5CF6")
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                canvas.drawRect(scaledLayer.x - 5, scaledLayer.y - 5, scaledLayer.x + scaledLayer.width + 5, scaledLayer.y + scaledLayer.height + 5, paint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                for (i in layersList.size - 1 downTo 0) {
                    val layer = layersList[i]
                    if (event.x >= layer.x && event.x <= layer.x + layer.width &&
                        event.y >= layer.y && event.y <= layer.y + layer.height) {
                        selectedLayerIndex = i
                        isDragging = true
                        lastX = event.x
                        lastY = event.y
                        rotationStart = Math.toDegrees(Math.atan2(event.y - (layer.y + layer.height/2).toDouble(), event.x - (layer.x + layer.width/2).toDouble())).toFloat()
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
                    val layer = layersList[selectedLayerIndex]
                    layer.x += dx
                    layer.y += dy
                    lastX = event.x
                    lastY = event.y
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> { isDragging = false }
        }
        return true
    }
}
