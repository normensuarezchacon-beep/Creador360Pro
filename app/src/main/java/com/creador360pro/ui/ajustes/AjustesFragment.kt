package com.creador360pro.ui.ajustes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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

    private var selectedImageUri: Uri? = null

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
            .setMessage("¿Crear un archivo .c360backup con todos tus datos?\n\nIncluye:\n• Ingresos\n• Ideas\n• Calendario\n• Guiones\n• Grabaciones de audio")
            .setPositiveButton("Crear backup") { _, _ ->
                val progressDialog = AlertDialog.Builder(requireContext())
                    .setTitle("Creando backup...")
                    .setMessage("Espera por favor.")
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
                            .setMessage("Archivo: ${result.name}\nTamaño: ${result.length() / 1024} KB")
                            .setPositiveButton("Compartir") { _, _ -> compartirArchivo(result) }
                            .setNegativeButton("OK", null)
                            .show()
                    } else {
                        Toast.makeText(requireContext(), "Error al crear backup", Toast.LENGTH_SHORT).show()
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
                .setMessage("No se encontraron archivos .c360backup.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val nombres = backupFiles.map { "${it.name} (${sdf.format(Date(it.lastModified))})" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Restaurar backup")
            .setItems(nombres) { _, which ->
                val selectedFile = backupFiles[which]
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar")
                    .setMessage("¿Restaurar desde ${selectedFile.name}?")
                    .setPositiveButton("Restaurar") { _, _ ->
                        lifecycleScope.launch {
                            val success = withContext(Dispatchers.IO) {
                                BackupManager.restoreBackup(requireContext(), selectedFile)
                            }
                            if (success) {
                                Toast.makeText(requireContext(), "Datos restaurados", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Error al restaurar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configurarTasa() {
        Toast.makeText(requireContext(), "Las tasas se configuran en Ganancias", Toast.LENGTH_SHORT).show()
    }

    private fun publicarContenido() {
        val opciones = arrayOf(
            "📸 Publicar en Instagram",
            "📘 Publicar en Facebook",
            "💬 Compartir en WhatsApp",
            "📤 Compartir con cualquier app"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Publicar contenido")
            .setItems(opciones) { _, which ->
                seleccionarImagen(which)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun seleccionarImagen(plataforma: Int) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, plataforma + 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == android.app.Activity.RESULT_OK && data != null) {
            val uri = data.data
            if (uri != null) {
                selectedImageUri = uri
                when (requestCode) {
                    100 -> publicarEnInstagram(uri)
                    101 -> publicarEnFacebook(uri)
                    102 -> compartirEnWhatsApp(uri)
                    103 -> compartirConCualquierApp(uri)
                }
            }
        }
    }

    private fun publicarEnInstagram(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Instagram no está instalado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun publicarEnFacebook(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.facebook.katana")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Facebook no está instalado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compartirEnWhatsApp(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp no está instalado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compartirConCualquierApp(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir con..."))
    }

    private fun compartirArchivo(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir backup"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al compartir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun acercaDe() {
        AlertDialog.Builder(requireContext())
            .setTitle("Creador360 PRO")
            .setMessage("Versión 1.0\n\n" +
                    "Suite de creación de contenido para emprendedores cubanos.\n\n" +
                    "Funcionalidades:\n" +
                    "• Editor de diseño con IA\n" +
                    "• Editor de video con transiciones\n" +
                    "• Teleprompter Pro\n" +
                    "• Estudio de audio\n" +
                    "• Banco de ideas\n" +
                    "• Calendario editorial\n" +
                    "• Gestor de ganancias\n" +
                    "• Backup y restauración\n" +
                    "• Publicación en redes\n\n" +
                    "Creado por invexXo TEAM\n" +
                    "© 2026 Todos los derechos reservados")
            .setPositiveButton("OK", null)
            .show()
    }
}
