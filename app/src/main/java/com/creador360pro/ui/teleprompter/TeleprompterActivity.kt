package com.creador360pro.ui.teleprompter

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.creador360pro.R
import com.creador360pro.data.db.AppDatabase
import com.creador360pro.data.model.ScriptItem
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TeleprompterActivity : AppCompatActivity() {

    private lateinit var tvScript: TextView
    private lateinit var seekBarSpeed: SeekBar
    private lateinit var tvCurrentScript: TextView
    private var isScrolling = false
    private var scrollSpeed = 2f
    private var currentScript: ScriptItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teleprompter)

        tvScript = findViewById(R.id.tvScript)
        seekBarSpeed = findViewById(R.id.seekBarSpeed)
        tvCurrentScript = findViewById(R.id.tvCurrentScript)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnLoadScript).setOnClickListener { showScriptList() }
        findViewById<Button>(R.id.btnNewScript).setOnClickListener { createNewScript() }
        findViewById<Button>(R.id.btnEditScript).setOnClickListener { editCurrentScript() }
        findViewById<Button>(R.id.btnStartStop).setOnClickListener { toggleScroll() }
        findViewById<Button>(R.id.btnSpeedUp).setOnClickListener { adjustSpeed(0.5f) }
        findViewById<Button>(R.id.btnSpeedDown).setOnClickListener { adjustSpeed(-0.5f) }

        seekBarSpeed.max = 10
        seekBarSpeed.progress = 4
        seekBarSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                scrollSpeed = (progress + 1).toFloat()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        tvScript.text = "Selecciona un guión o crea uno nuevo para empezar."
    }

    private fun showScriptList() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@TeleprompterActivity)
            db.scriptDao().getAllScripts().collect { scripts ->
                if (scripts.isEmpty()) {
                    AlertDialog.Builder(this@TeleprompterActivity)
                        .setTitle("Guiones guardados")
                        .setMessage("No tienes guiones guardados.\n\nCrea uno nuevo con el botón +")
                        .setPositiveButton("Crear nuevo") { _, _ -> createNewScript() }
                        .setNegativeButton("Cancelar", null)
                        .show()
                    return@collect
                }

                val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                val titulos = scripts.map {
                    "${it.titulo} (${sdf.format(Date(it.fechaModificacion))})"
                }.toTypedArray()

                AlertDialog.Builder(this@TeleprompterActivity)
                    .setTitle("Cargar guión")
                    .setItems(titulos) { _, which ->
                        currentScript = scripts[which]
                        tvScript.text = scripts[which].contenido
                        tvCurrentScript.text = scripts[which].titulo
                        tvCurrentScript.visibility = View.VISIBLE
                        Toast.makeText(this@TeleprompterActivity, "Guión cargado", Toast.LENGTH_SHORT).show()
                    }
                    .setPositiveButton("Nuevo") { _, _ -> createNewScript() }
                    .setNegativeButton("Cancelar", null)
                    .show()
                return@collect
            }
        }
    }

    private fun createNewScript() {
        val inputTitulo = EditText(this).apply { hint = "Título del guión" }
        val inputContenido = EditText(this).apply {
            hint = "Contenido del guión..."
            minLines = 8
            gravity = Gravity.TOP
            setText(tvScript.text)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(TextView(this@TeleprompterActivity).apply { text = "Título:"; setTextColor(Color.BLACK) })
            addView(inputTitulo)
            addView(TextView(this@TeleprompterActivity).apply { text = "Contenido:"; setTextColor(Color.BLACK); setPadding(0, 16, 0, 0) })
            addView(inputContenido)
        }

        AlertDialog.Builder(this)
            .setTitle("Nuevo guión")
            .setView(ScrollView(this).apply { addView(layout) })
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = inputTitulo.text.toString().ifEmpty { "Guión sin título" }
                val contenido = inputContenido.text.toString()
                if (contenido.isNotEmpty()) {
                    lifecycleScope.launch {
                        val db = AppDatabase.getInstance(this@TeleprompterActivity)
                        val script = ScriptItem(
                            titulo = titulo,
                            contenido = contenido
                        )
                        val id = db.scriptDao().insertScript(script)
                        currentScript = db.scriptDao().getScriptById(id)
                        tvScript.text = contenido
                        tvCurrentScript.text = titulo
                        tvCurrentScript.visibility = View.VISIBLE
                        Toast.makeText(this@TeleprompterActivity, "Guión guardado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editCurrentScript() {
        val script = currentScript
        if (script == null) {
            Toast.makeText(this, "Carga o crea un guión primero", Toast.LENGTH_SHORT).show()
            return
        }

        val inputContenido = EditText(this).apply {
            setText(script.contenido)
            minLines = 8
            gravity = Gravity.TOP
        }

        AlertDialog.Builder(this)
            .setTitle("Editar: ${script.titulo}")
            .setView(ScrollView(this).apply { addView(inputContenido) })
            .setPositiveButton("Guardar") { _, _ ->
                lifecycleScope.launch {
                    val db = AppDatabase.getInstance(this@TeleprompterActivity)
                    val updatedScript = script.copy(
                        contenido = inputContenido.text.toString(),
                        fechaModificacion = System.currentTimeMillis()
                    )
                    db.scriptDao().updateScript(updatedScript)
                    currentScript = updatedScript
                    tvScript.text = updatedScript.contenido
                    Toast.makeText(this@TeleprompterActivity, "Guión actualizado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toggleScroll() {
        isScrolling = !isScrolling
        val btn = findViewById<Button>(R.id.btnStartStop)
        btn.text = if (isScrolling) "⏸" else "▶"
        if (isScrolling) startScrolling()
    }

    private fun startScrolling() {
        Thread {
            while (isScrolling) {
                runOnUiThread {
                    tvScript.scrollBy(0, scrollSpeed.toInt())
                }
                Thread.sleep(50)
            }
        }.start()
    }

    private fun adjustSpeed(delta: Float) {
        scrollSpeed = (scrollSpeed + delta).coerceIn(1f, 11f)
        seekBarSpeed.progress = (scrollSpeed - 1).toInt()
        Toast.makeText(this, "Velocidad: ${scrollSpeed.toInt()}", Toast.LENGTH_SHORT).show()
    }
}
