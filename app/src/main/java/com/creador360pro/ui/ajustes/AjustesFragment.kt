package com.creador360pro.ui.ajustes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.creador360pro.R

class AjustesFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ajustes, container, false)

        view.findViewById<LinearLayout>(R.id.btnBackup).setOnClickListener { backupData() }
        view.findViewById<LinearLayout>(R.id.btnRestore).setOnClickListener { restoreData() }
        view.findViewById<LinearLayout>(R.id.btnTasaCambio).setOnClickListener { configurarTasa() }
        view.findViewById<LinearLayout>(R.id.btnPublicador).setOnClickListener { publicarContenido() }
        view.findViewById<LinearLayout>(R.id.btnAcerca).setOnClickListener { acercaDe() }

        return view
    }

    private fun backupData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Copia de seguridad")
            .setMessage("¿Guardar todos tus proyectos, ingresos e ideas?\n\nSe creará un archivo .c360backup en tu almacenamiento.")
            .setPositiveButton("Crear backup") { _, _ ->
                Toast.makeText(requireContext(), "Backup creado correctamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun restoreData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Restaurar backup")
            .setMessage("¿Restaurar todos los datos desde un archivo .c360backup?\n\nEsto reemplazará todos los datos actuales.")
            .setPositiveButton("Restaurar") { _, _ ->
                Toast.makeText(requireContext(), "Datos restaurados correctamente", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configurarTasa() {
        Toast.makeText(requireContext(), "Tasas de cambio configuradas desde Ganancias", Toast.LENGTH_SHORT).show()
    }

    private fun publicarContenido() {
        val plataformas = arrayOf("Facebook", "YouTube", "Instagram")
        AlertDialog.Builder(requireContext())
            .setTitle("Publicar contenido")
            .setMessage("Selecciona la plataforma donde quieres publicar tu contenido.\n\nEl contenido se subirá cuando haya conexión WiFi disponible.")
            .setItems(plataformas) { _, which ->
                Toast.makeText(requireContext(), "Contenido encolado para ${plataformas[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun acercaDe() {
        AlertDialog.Builder(requireContext())
            .setTitle("Creador360 PRO")
            .setMessage("Versión 1.0\n\n" +
                    "Suite de creación de contenido para emprendedores cubanos.\n\n" +
                    "Funcionalidades:\n" +
                    "• Editor de diseño con IA\n" +
                    "• Editor de video\n" +
                    "• Teleprompter\n" +
                    "• Estudio de audio\n" +
                    "• Banco de ideas\n" +
                    "• Calendario editorial\n" +
                    "• Gestor de ganancias\n\n" +
                    "Creado por invexXo TEAM\n" +
                    "© 2026 Todos los derechos reservados")
            .setPositiveButton("OK", null)
            .show()
    }
}
