package com.creador360pro.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.creador360pro.R
import com.creador360pro.ui.MainActivity

class OnboardingActivity : AppCompatActivity() {

    private var currentPage = 0
    private lateinit var tvTitulo: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var btnSiguiente: Button
    private lateinit var btnAtras: Button

    private val pages = listOf(
        Triple(
            "Bienvenido a Creador360",
            "La suite de creación de contenido que funciona hasta en el apagón.",
            "🎨"
        ),
        Triple(
            "Todo en uno",
            "Diseña, edita videos, graba podcasts, planifica tu contenido y lleva tus finanzas desde un mismo lugar.",
            "🚀"
        ),
        Triple(
            "Hecho para Cuba",
            "100% offline, con soporte para CUP, USD, MLC y EUR. Creado por invexXo TEAM.",
            "🇨🇺"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        tvTitulo = findViewById(R.id.tvTitulo)
        tvDescripcion = findViewById(R.id.tvDescripcion)
        btnSiguiente = findViewById(R.id.btnSiguiente)
        btnAtras = findViewById(R.id.btnAtras)

        btnAtras.visibility = View.GONE
        actualizarPagina()

        btnSiguiente.setOnClickListener {
            if (currentPage < pages.size - 1) {
                currentPage++
                actualizarPagina()
            } else {
                val prefs = getSharedPreferences("settings", MODE_PRIVATE)
                prefs.edit().putBoolean("onboarding_visto", true).apply()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        btnAtras.setOnClickListener {
            if (currentPage > 0) {
                currentPage--
                actualizarPagina()
            }
        }
    }

    private fun actualizarPagina() {
        val page = pages[currentPage]
        tvTitulo.text = page.first
        tvDescripcion.text = page.second
        findViewById<TextView>(R.id.tvEmoji).text = page.third

        btnAtras.visibility = if (currentPage == 0) View.GONE else View.VISIBLE
        btnSiguiente.text = if (currentPage == pages.size - 1) "Comenzar" else "Siguiente"
    }
}
