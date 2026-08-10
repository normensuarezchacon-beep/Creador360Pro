package com.creador360pro.ui.ajustes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.creador360pro.R
import com.creador360pro.util.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
            .setMessage("¿Crear un archivo .c360backup con todos tus datos?\n\n" +
                    "Incluye:\n" +
                    "• Ingresos\n" +
                    "• Ideas\n" +
                    "• Calendario\n" +
                    "• Guiones\n" +
                    "• Grabaciones de audio\n\n" +
                    "Se guardará en la carpeta Descargas.")
            .setPositiveButton("Crear backup") { _, _ ->
                val progressDialog = AlertDialog.Builder(requireContext())
                    .setTitle("Creando backup...")
                    .setMessage("Por favor espera mientras se crea la copia de seguridad.")
                    .setCancelable(false)
                    .create()
                progressDialog.show()

                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        BackupManager.createBackup(requireContext())
                    }
                    progressDialog.dismiss()

                    if (result != null) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Backup creado")
                            .setMessage("Archivo guardado en:\n${result.absolutePath}\n\n" +
                                    "Tamaño: ${result.length() / 1024} KB")
                            .setPositiveButton("Compartir") { _, _ ->
                                compartirArchivo(result)
                            }
                            .setNegativeButton("OK", null)
                            .show()
                    } else {
                        Toast.makeText(requireContext(), "Error al crear el backup", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun restoreData() {
        val backupFiles = BackupManager.getBackupFiles()

        if (backupFiles.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Restaurar backup")
                .setMessage("No se encontraron archivos .c360backup en la carpeta Descargas.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val nombres = backupFiles.map { file ->
            "${file.name} (${sdf.format(Date(file.lastModified()))}) - ${file.length() / 1024} KB"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Restaurar backup")
            .setItems(nombres) { _, which ->
                val selectedFile = backupFiles[which]
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar restauración")
                    .setMessage("¿Restaurar desde:\n${selectedFile.name}?\n\n" +
                            "ATENCIÓN: Esto reemplazará todos tus datos actuales.")
                    .setPositiveButton("Restaurar") { _, _ ->
                        val progressDialog = AlertDialog.Builder(requireContext())
                            .setTitle("Restaurando...")
                            .setMessage("Recuperando tus datos. Espera por favor.")
                            .setCancelable(false)
                            .create()
                        progressDialog.show()

                        lifecycleScope.launch {
                            val success = withContext(Dispatchers.IO) {
                                BackupManager.restoreBackup(requireContext(), selectedFile)
                            }
                            progressDialog.dismiss()

                            if (success) {
                                AlertDialog.Builder(requireContext())
                                    .setTitle("Restauración completada")
                                    .setMessage("Tus datos han sido restaurados correctamente.\n\nRecomendamos reiniciar la app.")
                                    .setPositiveButton("OK", null)
                                    .show()
                            } else {
                                Toast.makeText(requireContext(), "Error al restaurar el backup", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setPositiveButton("Cancelar", null)
            .show()
    }

    private fun configurarTasa() {
        Toast.makeText(requireContext(), "Las tasas se configuran en la sección de Ganancias", Toast.LENGTH_SHORT).show()
    }

    private fun publicarContenido() {
        val opciones = arrayOf(
            "📸 Compartir imagen de galería",
            "🎬 Compartir video de galería",
            "📄 Compartir archivo de Descargas",
            "🔗 Compartir texto"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Compartir contenido")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> compartirDesdeGaleria("image/*", "Selecciona una imagen")
                    1 -> compartirDesdeGaleria("video/*", "Selecciona un video")
                    2 -> compartirDesdeDescargas()
                    3 -> compartirTexto()
                }
            }
            .show()
    }

    private fun compartirDesdeGaleria(tipo: String, titulo: String) {
        try {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = tipo
                putExtra(Intent.EXTRA_TITLE, titulo)
            }
            startActivityForResult(Intent.createChooser(intent, titulo), 500)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No se pudo abrir la galería", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compartirDesdeDescargas() {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val files = downloadsDir.listFiles { file ->
            file.isFile && (file.name.endsWith(".mp4") || file.name.endsWith(".jpg") ||
                    file.name.endsWith(".png") || file.name.endsWith(".mp3") ||
                    file.name.endsWith(".m4a") || file.name.endsWith(".c360backup") ||
                    file.name.endsWith(".csv"))
        }?.sortedByDescending { it.lastModified() }

        if (files.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No hay archivos para compartir en Descargas", Toast.LENGTH_SHORT).show()
            return
        }

        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val nombres = files.map { "${it.name} (${sdf.format(Date(it.lastModified()))})" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Compartir archivo")
            .setItems(nombres) { _, which ->
                compartirArchivo(files[which])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun compartirArchivo(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = when {
                    file.name.endsWith(".mp4") -> "video/mp4"
                    file.name.endsWith(".jpg") || file.name.endsWith(".png") -> "image/*"
                    file.name.endsWith(".mp3") || file.name.endsWith(".m4a") -> "audio/*"
                    file.name.endsWith(".csv") -> "text/csv"
                    else -> "*/*"
                }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir con..."))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al compartir: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compartirTexto() {
        val input = EditText(requireContext()).apply {
            hint = "Escribe el texto a compartir"
            minLines = 3
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Compartir texto")
            .setView(input)
            .setPositiveButton("Compartir") { _, _ ->
                val texto = input.text.toString()
                if (texto.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, texto)
                    }
                    startActivity(Intent.createChooser(intent, "Compartir texto"))
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 500 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = requireContext().contentResolver.getType(uri)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Compartir con..."))
            }
        }
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
                    "• Gestor de ganancias\n" +
                    "• Backup y restauración\n" +
                    "• Compartir contenido\n\n" +
                    "Creado por invexXo TEAM\n" +
                    "© 2026 Todos los derechos reservados")
            .setPositiveButton("OK", null)
            .show()
    }
}
