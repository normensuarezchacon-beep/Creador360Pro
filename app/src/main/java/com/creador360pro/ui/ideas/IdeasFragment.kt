package com.creador360pro.ui.ideas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.creador360pro.R

class IdeasFragment : Fragment() {

    private val ideasPrecargadas = listOf(
        "Tutorial rápido de diseño" to "Enseña a tus seguidores cómo crear un flyer en 5 pasos. Usa Creador360 PRO.",
        "Detrás de cámaras" to "Muestra tu espacio de trabajo y cómo creas contenido desde Cuba.",
        "Top 5 herramientas" to "Las 5 apps que todo emprendedor cubano debe tener en su celular.",
        "Antes y después" to "Transforma un diseño básico en algo profesional usando filtros y fuentes.",
        "Día del padre cubano" to "Idea especial para el tercer domingo de junio. Felicita con un diseño único.",
        "Oferta relámpago" to "Crea un flyer de oferta por tiempo limitado para tu negocio.",
        "Receta cubana" to "Comparte una receta tradicional con un toque moderno. Usa texto elegante.",
        "Motivación lunes" to "Frases motivacionales con diseño minimalista para empezar la semana.",
        "Comparativa" to "Compara dos productos, servicios o lugares. Usa formato de tabla visual.",
        "Historia de emprendimiento" to "Cuenta cómo empezaste tu negocio en Cuba. Inspira a otros."
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ideas, container, false)
        val llIdeas = view.findViewById<LinearLayout>(R.id.llIdeas)

        ideasPrecargadas.forEach { (titulo, descripcion) ->
            val card = layoutInflater.inflate(R.layout.item_idea, llIdeas, false)
            card.findViewById<TextView>(R.id.tvTitulo).text = titulo
            card.findViewById<TextView>(R.id.tvDescripcion).text = descripcion

            card.findViewById<Button>(R.id.btnUsar).setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Usar idea")
                    .setMessage("¿Agregar esta idea al calendario editorial?")
                    .setPositiveButton("Sí") { _, _ ->
                        Toast.makeText(requireContext(), "Idea agregada al calendario", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }

            card.findViewById<Button>(R.id.btnFavorito).setOnClickListener {
                Toast.makeText(requireContext(), "Marcada como favorita", Toast.LENGTH_SHORT).show()
            }

            llIdeas.addView(card)
        }

        return view
    }
}
