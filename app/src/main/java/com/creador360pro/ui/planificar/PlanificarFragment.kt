package com.creador360pro.ui.planificar

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
import com.creador360pro.data.model.CalendarEvent
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*

class PlanificarFragment : Fragment() {

    private val viewModel: CalendarViewModel by viewModel()
    private lateinit var tvSemana: TextView
    private lateinit var llDias: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_planificar, container, false)

        tvSemana = view.findViewById(R.id.tvSemana)
        llDias = view.findViewById(R.id.llDias)

        view.findViewById<Button>(R.id.btnAnterior).setOnClickListener {
            viewModel.goToPreviousWeek()
            actualizarUI()
        }

        view.findViewById<Button>(R.id.btnSiguiente).setOnClickListener {
            viewModel.goToNextWeek()
            actualizarUI()
        }

        actualizarUI()

        return view
    }

    private fun actualizarUI() {
        tvSemana.text = viewModel.getCurrentWeekLabel()

        lifecycleScope.launch {
            viewModel.getEventsForCurrentWeek().collect { eventos ->
                llDias.removeAllViews()

                for (diaOffset in 0..6) {
                    val diaView = layoutInflater.inflate(R.layout.item_dia, llDias, false)
                    val tvDia = diaView.findViewById<TextView>(R.id.tvDia)
                    val tvEventos = diaView.findViewById<TextView>(R.id.tvEventos)
                    val btnAgregar = diaView.findViewById<Button>(R.id.btnAgregarEvento)

                    tvDia.text = viewModel.getDayLabel(diaOffset)

                    // Filtrar eventos de este día
                    val eventosDelDia = eventos.filter { event ->
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = event.fechaHora
                        val eventDay = cal.get(Calendar.DAY_OF_YEAR)
                        val calDia = Calendar.getInstance()
                        calDia.timeInMillis = getDateForOffset(diaOffset)
                        eventDay == calDia.get(Calendar.DAY_OF_YEAR)
                    }

                    if (eventosDelDia.isEmpty()) {
                        tvEventos.text = "Sin eventos"
                        tvEventos.setTextColor(Color.parseColor("#AAAAAA"))
                    } else {
                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        tvEventos.text = eventosDelDia.joinToString("\n") { event ->
                            val hora = sdf.format(Date(event.fechaHora))
                            "• $hora - ${event.titulo}"
                        }
                        tvEventos.setTextColor(Color.parseColor("#333333"))
                    }

                    // Toque largo para ver/eliminar eventos
                    diaView.setOnLongClickListener {
                        if (eventosDelDia.isNotEmpty()) {
                            mostrarEventosDelDia(eventosDelDia)
                        }
                        true
                    }

                    btnAgregar.setOnClickListener {
                        agregarEvento(diaOffset)
                    }

                    llDias.addView(diaView)
                }
            }
        }
    }

    private fun getDateForOffset(diaOffset: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, diaOffset)
        return cal.timeInMillis
    }

    private fun agregarEvento(diaOffset: Int) {
        val input = EditText(requireContext())
        input.hint = "Título del evento"

        AlertDialog.Builder(requireContext())
            .setTitle("Nuevo evento - ${viewModel.getDayLabel(diaOffset)}")
            .setView(input)
            .setPositiveButton("Agregar") { _, _ ->
                val titulo = input.text.toString()
                if (titulo.isNotEmpty()) {
                    viewModel.addEvent(titulo, diaOffset)
                    Toast.makeText(requireContext(), "Evento agregado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarEventosDelDia(eventos: List<CalendarEvent>) {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val titulos = eventos.map { "${sdf.format(Date(it.fechaHora))} - ${it.titulo}" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Eventos del día")
            .setItems(titulos) { _, which ->
                val evento = eventos[which]
                AlertDialog.Builder(requireContext())
                    .setTitle("Evento")
                    .setMessage("${evento.titulo}\n\n¿Deseas eliminar este evento?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        viewModel.deleteEvent(evento)
                        Toast.makeText(requireContext(), "Evento eliminado", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setPositiveButton("Cerrar", null)
            .show()
    }
}
