package com.creador360pro.util

import android.content.Context
import android.graphics.Typeface
import java.util.LinkedHashMap

object FontManager {

    private val fontCache = LinkedHashMap<String, Typeface>(10, 0.75f, true)
    private var initialized = false

    private val googleFonts = mapOf(
        "Montserrat" to "fonts/montserrat_regular.ttf",
        "Roboto" to "fonts/roboto_regular.ttf",
        "Open Sans" to "fonts/open_sans_regular.ttf",
        "Lato" to "fonts/lato_regular.ttf",
        "Bebas Neue" to "fonts/bebas_neue_regular.ttf",
        "Oswald" to "fonts/oswald_regular.ttf",
        "Playfair Display" to "fonts/playfair_display_regular.ttf",
        "Poppins" to "fonts/poppins_regular.ttf",
        "Raleway" to "fonts/raleway_regular.ttf",
        "Merriweather" to "fonts/merriweather_regular.ttf"
    )

    fun getAvailableFonts(): List<String> = googleFonts.keys.toList()

    fun getTypeface(context: Context, fontName: String, isBold: Boolean = false): Typeface {
        if (fontCache.containsKey(fontName)) {
            val cached = fontCache[fontName]
            if (cached != null) return cached
        }

        val assetPath = googleFonts[fontName]
        return if (assetPath != null) {
            try {
                val typeface = Typeface.createFromAsset(context.assets, assetPath)
                fontCache[fontName] = typeface
                typeface
            } catch (e: Exception) {
                getDefaultTypeface(isBold)
            }
        } else {
            getDefaultTypeface(isBold)
        }
    }

    private fun getDefaultTypeface(isBold: Boolean): Typeface {
        return if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}
