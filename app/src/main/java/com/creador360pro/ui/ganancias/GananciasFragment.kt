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

    private var tasaUSD = 270.0
    private var tasaMLC = 260.0
    private var tasaEUR = 300.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ganancias, container, false)

        tvResumen = view.findViewById(R.id.tvResumen)
        llIngresos = view.findViewById(R.id.llIngresos)

        view.findViewById<Button>(R.id.btnAgregarIngreso).setOnClickListener { agregarIngreso() }
        view.findViewById<Button>(R.id.btnTasaCambio).setOnClickListener { configurarTasa() }
        view.findViewById<Button>(R.id.btnVerGrafico).setOnClickListener { verGrafico() }
        view.findViewById<Button>(R.id.btnExportarCSV).setOnClickListener { exportarCSV() }

        // Observar cambios en la base de datos
        lifecycleScope.launch {
            viewModel.allIncomes.collect { ingresos ->
                actualizarUI(ingresos)
            }
        }

        return view
    }

    private fun actualizarUI(ingresos: List<com.creador360pro.data.model.IncomeRecord>) {
        val totalCUP = calcularTotalCUP(ingresos)
        val countUSD = ingresos.count { it.moneda == "USD" }
        val countMLC = ingresos.count { it.moneda == "MLC" }
        val countCUP = ingresos.count { it.moneda == "CUP" }
        val countEUR = ingresos.count { it.moneda == "EUR" }

        tvResumen.text = buildString {
            append("Total: $${String.format("%.2f", totalCUP)} CUP\n")
            append("Ingresos: $countUSD USD | $countMLC MLC | $countCUP CUP | $countEUR EUR\n")
            append("Tasas: USD=$tasaUSD | MLC=$tasaMLC | EUR=$tasaEUR")
        }

        llIngresos.removeAllViews()
        if (ingresos.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "No hay ingresos registrados"
                setTextColor(Color.parseColor("#AAAAAA"))
                textSize = 14f
                setPadding(16, 16, 16, 16)
                gravity = android.view.Gravity.CENTER
            }
            llIngresos.addView(tv)
        } else {
            val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            ingresos.takeLast(20).reversed().forEach { income ->
                val fechaStr = sdf.format(Date(income.fecha))
                val tv = TextView(requireContext()).apply {
                    text = "$fechaStr - ${income.fuente}: ${income.monto} ${income.moneda}"
                    setTextColor(Color.parseColor("#333333"))
                    textSize = 13f
                    setPadding(16, 8, 16, 8)
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
                    val moneda = monedas[spinnerMoneda.selectedItemPosition]
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

        val inputUSD = EditText(requireContext()).apply { setText(tasaUSD.toString()); hint = "USD a CUP" }
        val inputMLC = EditText(requireContext()).apply { setText(tasaMLC.toString()); hint = "MLC a CUP" }
        val inputEUR = EditText(requireContext()).apply { setText(tasaEUR.toString()); hint = "EUR a CUP" }

        layout.addView(TextView(requireContext()).apply { text = "USD → CUP:"; setTextColor(Color.BLACK) })
        layout.addView(inputUSD)
        layout.addView(TextView(requireContext()).apply { text = "MLC → CUP:"; setTextColor(Color.BLACK); setPadding(0, 16, 0, 0) })
        layout.addView(inputMLC)
        layout.addView(TextView(requireContext()).apply { text = "EUR → CUP:"; setTextColor(Color.BLACK); setPadding(0, 16, 0, 0) })
        layout.addView(inputEUR)

        AlertDialog.Builder(requireContext())
            .setTitle("Tasas de cambio")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                tasaUSD = inputUSD.text.toString().toDoubleOrNull() ?: tasaUSD
                tasaMLC = inputMLC.text.toString().toDoubleOrNull() ?: tasaMLC
                tasaEUR = inputEUR.text.toString().toDoubleOrNull() ?: tasaEUR
                Toast.makeText(requireContext(), "Tasas actualizadas", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun verGrafico() {
        lifecycleScope.launch {
            val ingresos = viewModel.allIncomes.let { flow ->
                var list = listOf<com.creador360pro.data.model.IncomeRecord>()
                flow.collect { list = it; return@collect }
            }
            // Recolectar una vez
            var lista = listOf<com.creador360pro.data.model.IncomeRecord>()
            viewModel.allIncomes.collect { lista = it; return@collect }

            if (lista.isEmpty()) {
                Toast.makeText(requireContext(), "No hay ingresos para mostrar", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val porFuente = lista.groupBy { it.fuente }.mapValues { (_, list) ->
                list.sumOf { convertirACUP(it.monto, it.moneda) }
            }

            val mensaje = buildString {
                append("📊 GRÁFICO DE INGRESOS\n\n")
                append("Total: $${String.format("%.2f", calcularTotalCUP(lista))} CUP\n\n")
                append("Por fuente:\n")
                porFuente.forEach { (fuente, total) ->
                    val barra = "█".repeat((total / calcularTotalCUP(lista) * 20).toInt().coerceAtMost(20))
                    append("$fuente: $barra $${String.format("%.2f", total)}\n")
                }
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Gráfico de ingresos")
                .setMessage(mensaje)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun exportarCSV() {
        lifecycleScope.launch {
            var lista = listOf<com.creador360pro.data.model.IncomeRecord>()
            viewModel.allIncomes.collect { lista = it; return@collect }

            if (lista.isEmpty()) {
                Toast.makeText(requireContext(), "No hay datos para exportar", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val csv = buildString {
                append("Fecha,Monto,Moneda,Fuente\n")
                lista.forEach { income ->
                    val fecha = sdf.format(Date(income.fecha))
                    append("$fecha,${income.monto},${income.moneda},${income.fuente}\n")
                }
            }

            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, "creador360_ingresos_${System.currentTimeMillis()}.csv")
                FileWriter(file).use { it.write(csv) }
                Toast.makeText(requireContext(), "CSV guardado en:\n${file.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al guardar CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calcularTotalCUP(ingresos: List<com.creador360pro.data.model.IncomeRecord>): Double {
        return ingresos.sumOf { convertirACUP(it.monto, it.moneda) }
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
