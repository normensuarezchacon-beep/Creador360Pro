package com.creador360pro.ui.ganancias

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.creador360pro.R

class GananciasFragment : Fragment() {

    private val ingresos = mutableListOf<Triple<Double, String, String>>()
    private var tasaUSD = 270.0
    private var tasaMLC = 260.0
    private var tasaEUR = 300.0
    private lateinit var tvResumen: TextView
    private lateinit var llIngresos: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ganancias, container, false)

        tvResumen = view.findViewById(R.id.tvResumen)
        llIngresos = view.findViewById(R.id.llIngresos)

        view.findViewById<Button>(R.id.btnAgregarIngreso).setOnClickListener { agregarIngreso() }
        view.findViewById<Button>(R.id.btnTasaCambio).setOnClickListener { configurarTasa() }
        view.findViewById<Button>(R.id.btnVerGrafico).setOnClickListener { verGrafico() }
        view.findViewById<Button>(R.id.btnExportarCSV).setOnClickListener { exportarCSV() }

        actualizarResumen()

        return view
    }

    private fun agregarIngreso() {
        val inputMonto = EditText(requireContext())
        inputMonto.hint = "Monto"
        inputMonto.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val monedas = arrayOf("CUP", "USD", "MLC", "EUR")
        val fuentes = arrayOf("YouTube", "TikTok", "Facebook", "Transfermóvil", "Enzona", "Efectivo", "Patrocinio", "Otro")

        val spinnerMoneda = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, monedas)
        }
        val spinnerFuente = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, fuentes)
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(TextView(requireContext()).apply { text = "Monto:"; setTextColor(Color.BLACK) })
            addView(inputMonto)
            addView(TextView(requireContext()).apply { text = "Moneda:"; setTextColor(Color.BLACK) })
            addView(spinnerMoneda)
            addView(TextView(requireContext()).apply { text = "Fuente:"; setTextColor(Color.BLACK) })
            addView(spinnerFuente)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Agregar ingreso")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val monto = inputMonto.text.toString().toDoubleOrNull()
                if (monto != null && monto > 0) {
                    val moneda = monedas[spinnerMoneda.selectedItemPosition]
                    val fuente = fuentes[spinnerFuente.selectedItemPosition]
                    ingresos.add(Triple(monto, moneda, fuente))
                    actualizarResumen()
                    Toast.makeText(requireContext(), "Ingreso agregado: $monto $moneda", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configurarTasa() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val inputUSD = EditText(requireContext()).apply { setText(tasaUSD.toString()); hint = "USD a CUP" }
        val inputMLC = EditText(requireContext()).apply { setText(tasaMLC.toString()); hint = "MLC a CUP" }
        val inputEUR = EditText(requireContext()).apply { setText(tasaEUR.toString()); hint = "EUR a CUP" }

        layout.addView(TextView(requireContext()).apply { text = "Tasa USD → CUP:"; setTextColor(Color.BLACK) })
        layout.addView(inputUSD)
        layout.addView(TextView(requireContext()).apply { text = "Tasa MLC → CUP:"; setTextColor(Color.BLACK) })
        layout.addView(inputMLC)
        layout.addView(TextView(requireContext()).apply { text = "Tasa EUR → CUP:"; setTextColor(Color.BLACK) })
        layout.addView(inputEUR)

        AlertDialog.Builder(requireContext())
            .setTitle("Tasas de cambio")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                tasaUSD = inputUSD.text.toString().toDoubleOrNull() ?: tasaUSD
                tasaMLC = inputMLC.text.toString().toDoubleOrNull() ?: tasaMLC
                tasaEUR = inputEUR.text.toString().toDoubleOrNull() ?: tasaEUR
                actualizarResumen()
                Toast.makeText(requireContext(), "Tasas actualizadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun verGrafico() {
        if (ingresos.isEmpty()) {
            Toast.makeText(requireContext(), "No hay ingresos para mostrar", Toast.LENGTH_SHORT).show()
            return
        }
        val totalCUP = calcularTotalCUP()
        val mensaje = buildString {
            append("Resumen de ingresos:\n\n")
            append("Total en CUP: $$totalCUP\n\n")
            append("Por fuente:\n")
            val porFuente = ingresos.groupBy { it.third }.mapValues { (_, list) -> list.sumOf { convertirACUP(it.first, it.second) } }
            porFuente.forEach { (fuente, total) ->
                append("$fuente: $$total CUP\n")
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Gráfico de ingresos")
            .setMessage(mensaje)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun exportarCSV() {
        if (ingresos.isEmpty()) {
            Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }
        val csv = buildString {
            append("Monto,Moneda,Fuente\n")
            ingresos.forEach { (monto, moneda, fuente) ->
                append("$monto,$moneda,$fuente\n")
            }
        }
        Toast.makeText(requireContext(), "CSV generado (próximamente: guardar archivo)", Toast.LENGTH_LONG).show()
    }

    private fun calcularTotalCUP(): Double {
        return ingresos.sumOf { (monto, moneda, _) -> convertirACUP(monto, moneda) }
    }

    private fun convertirACUP(monto: Double, moneda: String): Double {
        return when (moneda) {
            "USD" -> monto * tasaUSD
            "MLC" -> monto * tasaMLC
            "EUR" -> monto * tasaEUR
            else -> monto
        }
    }

    private fun actualizarResumen() {
        val totalCUP = calcularTotalCUP()
        val countUSD = ingresos.count { it.second == "USD" }
        val countMLC = ingresos.count { it.second == "MLC" }
        val countCUP = ingresos.count { it.second == "CUP" }

        tvResumen.text = buildString {
            append("Total: $${String.format("%.2f", totalCUP)} CUP\n")
            append("Ingresos: $countUSD USD | $countMLC MLC | $countCUP CUP\n")
            append("Tasas: USD=$tasaUSD | MLC=$tasaMLC | EUR=$tasaEUR")
        }

        llIngresos.removeAllViews()
        ingresos.takeLast(5).forEach { (monto, moneda, fuente) ->
            val tv = TextView(requireContext()).apply {
                text = "$fuente: $monto $moneda"
                setTextColor(Color.parseColor("#333333"))
                textSize = 13f
                setPadding(8, 4, 8, 4)
            }
            llIngresos.addView(tv)
        }
    }
}
