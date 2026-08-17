package com.creador360pro.ui.splash

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.creador360pro.R
import com.creador360pro.ui.MainActivity

class SplashScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Lista de letras de "Creador360"
        val letras = listOf(
            findViewById<TextView>(R.id.tvC),
            findViewById<TextView>(R.id.tvR),
            findViewById<TextView>(R.id.tvE),
            findViewById<TextView>(R.id.tvA),
            findViewById<TextView>(R.id.tvD),
            findViewById<TextView>(R.id.tvO),
            findViewById<TextView>(R.id.tvR2),
            findViewById<TextView>(R.id.tv3),
            findViewById<TextView>(R.id.tv6),
            findViewById<TextView>(R.id.tv0)
        )

        // Animar cada letra secuencialmente
        letras.forEachIndexed { index, textView ->
            textView.alpha = 0f
            textView.scaleX = 0.5f
            textView.scaleY = 0.5f
            textView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setStartDelay(index * 120L)
                .start()
        }

        // Configurar invexXo con la X en azul
        val tvInvexXo = findViewById<TextView>(R.id.tvInvexXo)
        val spannable = SpannableString("invexXo")
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#234CF9")),
            5,
            6,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvInvexXo.text = spannable

        // Animar la aparición de invexXo
        tvInvexXo.alpha = 0f
        tvInvexXo.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(letras.size * 120L + 300)
            .start()

        // Navegar a MainActivity después de 3 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 3000)
    }
}
