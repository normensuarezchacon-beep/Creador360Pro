package com.creador360pro.util

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.creador360pro.data.db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private const val BACKUP_VERSION = "1.0"

    fun createBackup(context: Context): File? {
        return try {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val backupName = "Creador360_${sdf.format(Date())}.c360backup"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupFile = File(downloadsDir, backupName)

            ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                // 1. Metadata
                val metadata = JSONObject().apply {
                    put("version", BACKUP_VERSION)
                    put("date", System.currentTimeMillis())
                    put("appVersion", "1.0")
                }
                zipOut.putNextEntry(ZipEntry("metadata.json"))
                zipOut.write(metadata.toString(2).toByteArray())
                zipOut.closeEntry()

                // 2. Base de datos
                val db = AppDatabase.getInstance(context)
                runBlocking {
                    // Ingresos
                    val incomes = db.incomeDao().let { dao ->
                        var list = listOf<com.creador360pro.data.model.IncomeRecord>()
                        dao.getAllIncomes().collect { list = it; return@collect }
                        list
                    }
                    val incomesJson = JSONArray()
                    incomes.forEach { income ->
                        incomesJson.put(JSONObject().apply {
                            put("monto", income.monto)
                            put("moneda", income.moneda)
                            put("fuente", income.fuente)
                            put("fecha", income.fecha)
                        })
                    }
                    zipOut.putNextEntry(ZipEntry("incomes.json"))
                    zipOut.write(incomesJson.toString(2).toByteArray())
                    zipOut.closeEntry()

                    // Ideas
                    val ideas = db.ideaDao().let { dao ->
                        var list = listOf<com.creador360pro.data.model.IdeaItem>()
                        dao.getAllIdeas().collect { list = it; return@collect }
                        list
                    }
                    val ideasJson = JSONArray()
                    ideas.forEach { idea ->
                        ideasJson.put(JSONObject().apply {
                            put("titulo", idea.titulo)
                            put("descripcion", idea.descripcion)
                            put("gancho", idea.gancho)
                            put("hashtags", idea.hashtags)
                            put("categoria", idea.categoria)
                            put("estadoUso", idea.estadoUso)
                        })
                    }
                    zipOut.putNextEntry(ZipEntry("ideas.json"))
                    zipOut.write(ideasJson.toString(2).toByteArray())
                    zipOut.closeEntry()

                    // Calendario
                    val events = db.calendarDao().let { dao ->
                        var list = listOf<com.creador360pro.data.model.CalendarEvent>()
                        dao.getAllEvents().collect { list = it; return@collect }
                        list
                    }
                    val eventsJson = JSONArray()
                    events.forEach { event ->
                        eventsJson.put(JSONObject().apply {
                            put("titulo", event.titulo)
                            put("plataforma", event.plataforma)
                            put("tipo", event.tipo)
                            put("estado", event.estado)
                            put("prioridad", event.prioridad)
                            put("fechaHora", event.fechaHora)
                        })
                    }
                    zipOut.putNextEntry(ZipEntry("calendar.json"))
                    zipOut.write(eventsJson.toString(2).toByteArray())
                    zipOut.closeEntry()

                    // Guiones
                    val scripts = db.scriptDao().let { dao ->
                        var list = listOf<com.creador360pro.data.model.ScriptItem>()
                        dao.getAllScripts().collect { list = it; return@collect }
                        list
                    }
                    val scriptsJson = JSONArray()
                    scripts.forEach { script ->
                        scriptsJson.put(JSONObject().apply {
                            put("titulo", script.titulo)
                            put("contenido", script.contenido)
                            put("fechaCreacion", script.fechaCreacion)
                            put("fechaModificacion", script.fechaModificacion)
                        })
                    }
                    zipOut.putNextEntry(ZipEntry("scripts.json"))
                    zipOut.write(scriptsJson.toString(2).toByteArray())
                    zipOut.closeEntry()
                }

                // 3. Archivos de audio
                val audioDir = File(context.externalCacheDir, "")
                val audioFiles = audioDir.listFiles { file -> file.name.startsWith("audio_") }
                audioFiles?.forEach { audioFile ->
                    zipOut.putNextEntry(ZipEntry("audio/${audioFile.name}"))
                    FileInputStream(audioFile).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }

            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun restoreBackup(context: Context, backupFile: File): Boolean {
        return try {
            val db = AppDatabase.getInstance(context)

            ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    val content = zipIn.bufferedReader().readText()
                    zipIn.closeEntry()

                    when (entry.name) {
                        "incomes.json" -> {
                            val jsonArray = JSONArray(content)
                            runBlocking {
                                for (i in 0 until jsonArray.length()) {
                                    val obj = jsonArray.getJSONObject(i)
                                    val income = com.creador360pro.data.model.IncomeRecord(
                                        monto = obj.getDouble("monto"),
                                        moneda = obj.getString("moneda"),
                                        fuente = obj.getString("fuente"),
                                        fecha = obj.getLong("fecha")
                                    )
                                    db.incomeDao().insertIncome(income)
                                }
                            }
                        }
                        "ideas.json" -> {
                            val jsonArray = JSONArray(content)
                            runBlocking {
                                for (i in 0 until jsonArray.length()) {
                                    val obj = jsonArray.getJSONObject(i)
                                    val idea = com.creador360pro.data.model.IdeaItem(
                                        titulo = obj.getString("titulo"),
                                        descripcion = obj.getString("descripcion"),
                                        gancho = obj.getString("gancho"),
                                        hashtags = obj.getString("hashtags"),
                                        categoria = obj.getString("categoria"),
                                        estadoUso = obj.getString("estadoUso")
                                    )
                                    db.ideaDao().insertIdea(idea)
                                }
                            }
                        }
                        "calendar.json" -> {
                            val jsonArray = JSONArray(content)
                            runBlocking {
                                for (i in 0 until jsonArray.length()) {
                                    val obj = jsonArray.getJSONObject(i)
                                    val event = com.creador360pro.data.model.CalendarEvent(
                                        titulo = obj.getString("titulo"),
                                        plataforma = obj.getString("plataforma"),
                                        tipo = obj.getString("tipo"),
                                        estado = obj.getString("estado"),
                                        prioridad = obj.getString("prioridad"),
                                        fechaHora = obj.getLong("fechaHora")
                                    )
                                    db.calendarDao().insertEvent(event)
                                }
                            }
                        }
                        "scripts.json" -> {
                            val jsonArray = JSONArray(content)
                            runBlocking {
                                for (i in 0 until jsonArray.length()) {
                                    val obj = jsonArray.getJSONObject(i)
                                    val script = com.creador360pro.data.model.ScriptItem(
                                        titulo = obj.getString("titulo"),
                                        contenido = obj.getString("contenido"),
                                        fechaCreacion = obj.getLong("fechaCreacion"),
                                        fechaModificacion = obj.getLong("fechaModificacion")
                                    )
                                    db.scriptDao().insertScript(script)
                                }
                            }
                        }
                    }

                    entry = zipIn.nextEntry
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getBackupFiles(): List<File> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return downloadsDir.listFiles { file -> file.name.endsWith(".c360backup") }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
