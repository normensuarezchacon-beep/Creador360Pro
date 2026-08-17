package com.creador360pro.ui.ganancias

import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.creador360pro.R
import com.creador360pro.data.model.IncomeRecord
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class GananciasFragment : Fragment() {

    private val viewModel: IncomeViewModel by viewModel()
    private lateinit var tvResumen: TextView
    private lateinit var llIngresos: LinearLayout
    private lateinit var barChart: BarChart
    private lateinit var pieChart: PieChart
    private lateinit var tvMes: TextView
    private lateinit var tvMeta: TextView

    private var tasaUSD = 270.0
    private var tasaMLC = 260.0
    private var tasaEUR = 300.0
    private var currentMonthOffset = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ganancias, container, false)

        tvResumen = view.findViewById(R.id.tvResumen)
        llIngresos = view.findViewById(R.id.llIngresos)
        barChart = view.findViewById(R.id.barChart)
        pieChart = view.findViewById(R.id.pieChart)
        tvMes = view.findViewById(R.id.tvMes)
        tvMeta = view.findViewById(R.id.tvMeta)

        // Cargar tasas
        val prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        tasaUSD = prefs.getFloat("tasaUSD", 270.0f).toDouble()
        tasaMLC = prefs.getFloat("tasaMLC", 260.0f).toDouble()
        tasaEUR = prefs.getFloat("tasaEUR", 300.0f).toDouble()

        view.findViewById<Button>(R.id.btnAgregarIngreso).setOnClickListener { agregarIngreso() }
        view.findViewById<Button>(R.id.btnTasaCambio).setOnClickListener { configurarTasa() }
        view.findViewById<Button>(R.id.btnExportarCSV).setOnClickListener { exportarCSV() }
        view.findViewById<Button>(R.id.btnAnterior).setOnClickListener { cambiarMes(-1) }
        view.findViewById<Button>(R.id.btnSiguiente).setOnClickListener { cambiarMes(1) }
        view.findViewById<Button>(R.id.btnMeta).setOnClickListener { configurarMeta() }

        actualizarMes()
        lifecycleScope.launch {
            viewModel.allIncomes.collect { ingresos ->
                actualizarUI(ingresos)
            }
        }

        return view
    }

    private fun actualizarMes() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, currentMonthOffset)
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        tvMes.text = sdf.format(cal.time).capitalize()
    }

    private fun cambiarMes(delta: Int) {
        currentMonthOffset += delta
        actualizarMes()
    }

    private fun configurarMeta() {
        val input = EditText(requireContext())
        input.hint = "Meta mensual (CUP)"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER

        AlertDialog.Builder(requireContext())
            .setTitle("Meta de ingresos")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val meta = input.text.toString().toDoubleOrNull() ?: 0.0
                val prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                prefs.edit().putFloat("metaMensual", meta.toFloat()).apply()
                tvMeta.text = "Meta: $meta cup"
                Toast.makeText(requireContext(), "Meta guardada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarUI(ingresos: List<IncomeRecord>) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, currentMonthOffset)
        val mesActual = cal.get(Calendar.MONTH)
        val añoActual = cal.get(Calendar.YEAR)

        val ingresosDelMes = ingresos.filter { income ->
            val incomeCal = Calendar.getInstance()
            incomeCal.timeInMillis = income.fecha
            incomeCal.get(Calendar.MONTH) == mesActual && incomeCal.get(Calendar.YEAR) == añoActual
        }

        val totalCUP = ingresosDelMes.sumOf { convertirACUP(it.monto, it.moneda) }
        val prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        val meta = prefs.getFloat("metaMensual", 0f).toDouble()

        tvResumen.text = buildString {
            append("Total: $${String.format("%.2f", totalCUP)} cup\n")
            append("USD: ${ingresosDelMes.count { it.moneda == "USD" }} • MLC: ${ingresosDelMes.count { it.moneda == "MLC" }} • EUR: ${ingresosDelMes.count { it.moneda == "EUR" }}")
        }

        tvMeta.text = if (meta > 0) "Meta: $meta cup (${((totalCUP / meta) * 100).toInt()}%)" else "Sin meta establecida"

        // Lista de ingresos
        llIngresos.removeAllViews()
        if (ingresosDelMes.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "No hay ingresos este mes"
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 14f
                setPadding(16, 32, 16, 32)
                gravity = android.view.Gravity.CENTER
            }
            llIngresos.addView(tv)
        } else {
            val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            ingresosDelMes.sortedByDescending { it.fecha }.forEach { income ->
                val tv = TextView(requireContext()).apply {
                    text = "${sdf.format(Date(income.fecha))} - ${income.fuente}: ${income.monto} ${income.moneda}"
                    setTextColor(Color.parseColor("#333333"))
                    textSize = 13f
                    setPadding(16, 12, 16, 12)
                    setBackgroundColor(Color.WHITE)

                    setOnLongClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Eliminar ingreso")
                            .setMessage("¿Eliminar este ingreso?\n\n${income.fuente}: ${income.monto} ${income.moneda}")
                            .setPositiveButton("Eliminar") { _, _ ->
                                viewModel.deleteIncome(income)
                                Toast.makeText(requireContext(), "Ingreso eliminado", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                        true
                    }
                }
                llIngresos.addView(tv)
            }
        }

        // Gráfico de barras por fuente
        actualizarGraficoBarras(ingresosDelMes)
        // Gráfico de torta por moneda
        actualizarGraficoTorta(ingresosDelMes)
    }

    private fun actualizarGraficoBarras(ingresos: List<IncomeRecord>) {
        val fuentes = ingresos.map { it.fuente }.distinct()
        val entries = mutableListOf<BarEntry>()
        fuentes.forEachIndexed { index, fuente ->
            val total = ingresos.filter { it.fuente == fuente }.sumOf { convertirACUP(it.monto, it.moneda) }
            entries.add(BarEntry(index.toFloat(), total.toFloat()))
        }

        if (entries.isEmpty()) {
            barChart.clear()
            return
        }

        val dataSet = BarDataSet(entries, "Ingresos por fuente")
        dataSet.setColors(Color.parseColor("#8B5CF6"), Color.parseColor("#EC4899"), Color.parseColor("#FF6D00"), Color.parseColor("#4CAF50"), Color.parseColor("#2196F3"))

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        barChart.data = barData
        barChart.description.isEnabled = false
        barChart.axisLeft.axisMinimum = 0f
        barChart.xAxis.setDrawLabels(false)
        barChart.legend.isEnabled = false
        barChart.invalidate()
    }

    private fun actualizarGraficoTorta(ingresos: List<IncomeRecord>) {
        val totales = mutableMapOf<String, Double>()
        ingresos.forEach { income ->
            val valor = convertirACUP(income.monto, income.moneda)
            totales[income.moneda] = (totales[income.moneda] ?: 0.0) + valor
        }

        val entries = mutableListOf<PieEntry>()
        totales.forEach { (moneda, total) ->
            entries.add(PieEntry(total.toFloat(), moneda))
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            return
        }

        val dataSet = PieDataSet(entries, "Distribución por moneda")
        dataSet.setColors(Color.parseColor("#8B5CF6"), Color.parseColor("#EC4899"), Color.parseColor("#FF6D00"), Color.parseColor("#4CAF50"))
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val pieData = PieData(dataSet)
        pieChart.data = pieData
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 30f
        pieChart.transparentCircleRadius = 35f
        pieChart.legend.isEnabled = true
        pieChart.invalidate()
    }

    private fun agregarIngreso() {
        val inputMonto = EditText(requireContext())
        inputMonto.hint = "Monto"
        inputMonto.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val monedas = arrayOf("CUP (cup)", "USD ($)", "MLC ($)", "EUR (€)")
        val fuentes = arrayOf("YouTube", "TikTok", "Facebook", "Transfermóvil", "Enzona", "Efectivo", "Patrocinio", "Otro")

        val spinnerMoneda = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, monedas)
        }
        val spinnerFuente = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, fuentes)
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(TextView(requireContext()).apply { text = "Monto:"; setTextColor(Color.BLACK); setPadding(0, 8, 0, 4) })
            addView(inputMonto)
            addView(TextView(requireContext()).apply { text = "Moneda:"; setTextColor(Color.BLACK); setPadding(0, 16, 0, 4) })
            addView(spinnerMoneda)
            addView(TextView(requireContext()).apply { text = "Fuente:"; setTextColor(Color.BLACK); setPadding(0, 16, 0, 4) })
            addView(spinnerFuente)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Agregar ingreso")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val monto = inputMonto.text.toString().toDoubleOrNull()
                if (monto != null && monto > 0) {
                    val moneda = monedas[spinnerMoneda.selectedItemPosition].split(" ")[0]
                    val fuente = fuentes[spinnerFuente.selectedItemPosition]
                    viewModel.addIncome(monto, moneda, fuente)
                    Toast.makeText(requireContext(), "Ingreso guardado: $monto $moneda", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configurarTasa() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
        }

        val inputUSD = EditText(requireContext()).apply { setText(tasaUSD.toString()); hint = "USD ($) → CUP" }
        val inputMLC = EditText(requireContext()).apply { setText(tasaMLC.toString()); hint = "MLC ($) → CUP" }
        val inputEUR = EditText(requireContext()).apply { setText(tasaEUR.toString()); hint = "EUR (€) → CUP" }

        layout.addView(TextView(requireContext()).apply { text = "USD ($) → CUP:"; setTextColor(Color.BLACK) })
        layout.addView(inputUSD)
        layout.addView(TextView(requireContext()).apply { text = "MLC ($) → CUP:"; setTextColor(Color.BLACK); setPadding(0, 16, 0, 0) })
        layout.addView(inputMLC)
        layout.addView(TextView(requireContext()).apply { text = "EUR (€) → CUP:"; setTextColor(Color.BLACK); setPadding(0, 16, 0, 0) })
        layout.addView(inputEUR)

        AlertDialog.Builder(requireContext())
            .setTitle("Tasas de cambio")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                tasaUSD = inputUSD.text.toString().toDoubleOrNull() ?: tasaUSD
                tasaMLC = inputMLC.text.toString().toDoubleOrNull() ?: tasaMLC
                tasaEUR = inputEUR.text.toString().toDoubleOrNull() ?: tasaEUR
                val prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putFloat("tasaUSD", tasaUSD.toFloat())
                    .putFloat("tasaMLC", tasaMLC.toFloat())
                    .putFloat("tasaEUR", tasaEUR.toFloat())
                    .apply()
                Toast.makeText(requireContext(), "Tasas guardadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportarCSV() {
        lifecycleScope.launch {
            var lista = listOf<IncomeRecord>()
            viewModel.allIncomes.collect { lista = it; return@collect }

            if (lista.isEmpty()) {
                Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val csv = buildString {
                append("Fecha,Monto,Moneda,Fuente\n")
                lista.forEach { income ->
                    append("${sdf.format(Date(income.fecha))},${income.monto},${income.moneda},${income.fuente}\n")
                }
            }

            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, "creador360_ingresos_${System.currentTimeMillis()}.csv")
                FileWriter(file).use { it.write(csv) }
                Toast.makeText(requireContext(), "CSV guardado en:\n${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertirACUP(monto: Double, moneda: String): Double {
        return when (moneda) {
            "USD" -> monto * tasaUSD
            "MLC" -> monto * tasaMLC
            "EUR" -> monto * tasaEUR
            else -> monto
        }
    }
}
