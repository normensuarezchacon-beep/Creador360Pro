package com.creador360pro.ui.ideas

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.creador360pro.R
import com.creador360pro.data.model.IdeaItem
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class IdeasFragment : Fragment() {

    private val viewModel: IdeasViewModel by viewModel()
    private lateinit var llIdeas: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ideas, container, false)
        llIdeas = view.findViewById(R.id.llIdeas)

        // Cargar ideas precargadas si es la primera vez
        viewModel.cargarIdeasPrecargadas()

        // Observar todas las ideas
        lifecycleScope.launch {
            viewModel.allIdeas.collect { ideas ->
                actualizarLista(ideas)
            }
        }

        // Botón flotante para añadir idea personalizada
        view.findViewById<Button>(R.id.btnAgregarIdea).setOnClickListener {
            agregarIdeaPersonalizada()
        }

        return view
    }

    private fun actualizarLista(ideas: List<IdeaItem>) {
        llIdeas.removeAllViews()

        if (ideas.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "No hay ideas guardadas"
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 14f
                setPadding(16, 32, 16, 32)
                gravity = android.view.Gravity.CENTER
            }
            llIdeas.addView(tv)
            return
        }

        ideas.forEach { idea ->
            val card = layoutInflater.inflate(R.layout.item_idea, llIdeas, false)

            card.findViewById<TextView>(R.id.tvTitulo).text = idea.titulo
            card.findViewById<TextView>(R.id.tvDescripcion).text = idea.descripcion
            card.findViewById<TextView>(R.id.tvCategoria).text = idea.categoria
            card.findViewById<TextView>(R.id.tvHashtags).text = idea.hashtags

            val btnUsar = card.findViewById<Button>(R.id.btnUsar)
            val btnFavorito = card.findViewById<Button>(R.id.btnFavorito)

            // Botón favorito
            if (idea.estadoUso == "favorita") {
                btnFavorito.text = "★"
                btnFavorito.setBackgroundColor(Color.parseColor("#FFD700"))
            } else {
                btnFavorito.text = "☆"
                btnFavorito.setBackgroundColor(Color.parseColor("#FF9800"))
            }

            btnFavorito.setOnClickListener {
                viewModel.toggleFavorite(idea.id, idea.estadoUso)
            }

            // Botón usar (agregar al calendario)
            btnUsar.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Usar idea")
                    .setMessage("¿Agregar esta idea al calendario editorial?")
                    .setPositiveButton("Sí") { _, _ ->
                        Toast.makeText(requireContext(), "Idea agregada al calendario", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("No", null)
                    .show()
            }

            // Toque largo para eliminar
            card.setOnLongClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar idea")
                    .setMessage("¿Eliminar \"${idea.titulo}\"?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.deleteIdea(idea)
                        Toast.makeText(requireContext(), "Idea eliminada", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                true
            }

            llIdeas.addView(card)
        }
    }

    private fun agregarIdeaPersonalizada() {
        val inputTitulo = EditText(requireContext()).apply { hint = "Título de la idea" }
        val inputDescripcion = EditText(requireContext()).apply { hint = "Descripción o guión breve" }
        val inputHashtags = EditText(requireContext()).apply { hint = "#hashtag1 #hashtag2" }

        val categorias = arrayOf("Educación", "Entretenimiento", "Tecnología", "Diseño", "Negocios", "Cocina", "Motivación", "Celebraciones", "Otro")
        val spinnerCategoria = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, categorias)
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(TextView(requireContext()).apply { text = "Título:"; setTextColor(Color.BLACK) })
            addView(inputTitulo)
            addView(TextView(requireContext()).apply { text = "Descripción:"; setTextColor(Color.BLACK); setPadding(0, 12, 0, 0) })
            addView(inputDescripcion)
            addView(TextView(requireContext()).apply { text = "Hashtags:"; setTextColor(Color.BLACK); setPadding(0, 12, 0, 0) })
            addView(inputHashtags)
            addView(TextView(requireContext()).apply { text = "Categoría:"; setTextColor(Color.BLACK); setPadding(0, 12, 0, 0) })
            addView(spinnerCategoria)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Nueva idea")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = inputTitulo.text.toString()
                if (titulo.isNotEmpty()) {
                    val idea = IdeaItem(
                        titulo = titulo,
                        descripcion = inputDescripcion.text.toString(),
                        gancho = titulo,
                        hashtags = inputHashtags.text.toString(),
                        categoria = categorias[spinnerCategoria.selectedItemPosition]
                    )
                    viewModel.insertIdea(idea)
                    Toast.makeText(requireContext(), "Idea guardada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "El título es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
