package com.creador360pro.ui.planificar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.creador360pro.R
import java.util.*

class PlanificarFragment : Fragment() {

    private lateinit var tvSemana: TextView
    private val eventosSemana = mutableListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_planificar, container, false)

        tvSemana = view.findViewById(R.id.tvSemana)
        actualizarSemana()

        view.findViewById<Button>(R.id.btnAgregar).setOnClickListener { agregarEvento() }
        view.findViewById<Button>(R.id.btnAnterior).setOnClickListener { cambiarSemana(-7) }
        view.findViewById<Button>(R.id.btnSiguiente).setOnClickListener { cambiarSemana(7) }

        val llEventos = view.findViewById<LinearLayout>(R.id.llEventos)

        // Días de la semana
        val dias = arrayOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        dias.forEach { dia ->
            val diaView = inflater.inflate(R.layout.item_dia, llEventos, false)
            diaView.findViewById<TextView>(R.id.tvDia).text = dia
            llEventos.addView(diaView)
        }

        return view
    }

    private fun actualizarSemana() {
        tvSemana.text = "Semana: ${Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)}"
    }

    private fun cambiarSemana(dias: Int) {
        Toast.makeText(requireContext(), "Navegando ${if (dias > 0) "siguiente" else "anterior"} semana", Toast.LENGTH_SHORT).show()
        actualizarSemana()
    }

    private fun agregarEvento() {
        val input = EditText(requireContext())
        input.hint = "Título del evento"
        AlertDialog.Builder(requireContext())
            .setTitle("Nuevo evento")
            .setView(input)
            .setPositiveButton("Agregar") { _, _ ->
                val titulo = input.text.toString()
                if (titulo.isNotEmpty()) {
                    eventosSemana.add(titulo)
                    Toast.makeText(requireContext(), "Evento agregado: $titulo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
