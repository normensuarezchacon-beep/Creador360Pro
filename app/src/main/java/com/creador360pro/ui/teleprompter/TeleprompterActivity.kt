package com.creador360pro.ui.teleprompter

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.creador360pro.R

class TeleprompterActivity : AppCompatActivity() {

    private lateinit var tvScript: TextView
    private lateinit var seekBarSpeed: SeekBar
    private var isScrolling = false
    private var scrollSpeed = 2f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teleprompter)

        tvScript = findViewById(R.id.tvScript)
        seekBarSpeed = findViewById(R.id.seekBarSpeed)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnLoadScript).setOnClickListener { loadScript() }
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

        tvScript.text = "Escribe o carga tu guión aquí...\n\nToca el botón ▶ para empezar a desplazar el texto.\n\nAjusta la velocidad con los botones + y -"
    }

    private fun loadScript() {
        val input = EditText(this)
        input.setText(tvScript.text)
        input.minLines = 10
        input.gravity = android.view.Gravity.TOP

        AlertDialog.Builder(this)
            .setTitle("Cargar guión")
            .setView(ScrollView(this).apply { addView(input) })
            .setPositiveButton("Cargar") { _, _ ->
                tvScript.text = input.text.toString()
                Toast.makeText(this, "Guión cargado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toggleScroll() {
        isScrolling = !isScrolling
        val btn = findViewById<Button>(R.id.btnStartStop)
        btn.text = if (isScrolling) "⏸" else "▶"

        if (isScrolling) {
            startScrolling()
        }
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
