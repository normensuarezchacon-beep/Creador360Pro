package com.creador360pro.ui.ideas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creador360pro.data.dao.IdeaDao
import com.creador360pro.data.model.IdeaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class IdeasViewModel(private val ideaDao: IdeaDao) : ViewModel() {

    val allIdeas: Flow<List<IdeaItem>> = ideaDao.getAllIdeas()
    val favorites: Flow<List<IdeaItem>> = ideaDao.getFavorites()

    private val _ideasPrecargadas = MutableStateFlow(false)
    val ideasPrecargadas: StateFlow<Boolean> = _ideasPrecargadas

    fun insertIdea(idea: IdeaItem) {
        viewModelScope.launch {
            ideaDao.insertIdea(idea)
        }
    }

    fun toggleFavorite(id: Long, currentState: String) {
        viewModelScope.launch {
            val newState = if (currentState == "favorita") "disponible" else "favorita"
            ideaDao.updateEstado(id, newState)
        }
    }

    fun deleteIdea(idea: IdeaItem) {
        viewModelScope.launch {
            ideaDao.deleteIdea(idea)
        }
    }

    fun cargarIdeasPrecargadas() {
        viewModelScope.launch {
            if (!_ideasPrecargadas.value) {
                val ideas = listOf(
                    IdeaItem(
                        titulo = "Tutorial rápido de diseño",
                        descripcion = "Enseña a tus seguidores cómo crear un flyer en 5 pasos. Usa Creador360 PRO.",
                        gancho = "Aprende diseño en minutos",
                        hashtags = "#tutorial #diseño #creador360",
                        categoria = "Educación"
                    ),
                    IdeaItem(
                        titulo = "Detrás de cámaras",
                        descripcion = "Muestra tu espacio de trabajo y cómo creas contenido desde Cuba.",
                        gancho = "Así trabajo yo",
                        hashtags = "#detrasdecamaras #creador #cuba",
                        categoria = "Entretenimiento"
                    ),
                    IdeaItem(
                        titulo = "Top 5 herramientas",
                        descripcion = "Las 5 apps que todo emprendedor cubano debe tener en su celular.",
                        gancho = "Herramientas que necesitas",
                        hashtags = "#top5 #apps #emprendedor",
                        categoria = "Tecnología"
                    ),
                    IdeaItem(
                        titulo = "Antes y después",
                        descripcion = "Transforma un diseño básico en algo profesional usando filtros y fuentes.",
                        gancho = "Increíble transformación",
                        hashtags = "#antesydespues #diseño #transformacion",
                        categoria = "Diseño"
                    ),
                    IdeaItem(
                        titulo = "Oferta relámpago",
                        descripcion = "Crea un flyer de oferta por tiempo limitado para tu negocio.",
                        gancho = "Solo por hoy",
                        hashtags = "#oferta #negocio #flyer",
                        categoria = "Negocios"
                    ),
                    IdeaItem(
                        titulo = "Receta cubana",
                        descripcion = "Comparte una receta tradicional con un toque moderno. Usa texto elegante.",
                        gancho = "Cocina con estilo",
                        hashtags = "#receta #cocina #cuba",
                        categoria = "Cocina"
                    ),
                    IdeaItem(
                        titulo = "Motivación lunes",
                        descripcion = "Frases motivacionales con diseño minimalista para empezar la semana.",
                        gancho = "Empieza con energía",
                        hashtags = "#motivacion #lunes #frases",
                        categoria = "Motivación"
                    ),
                    IdeaItem(
                        titulo = "Comparativa",
                        descripcion = "Compara dos productos, servicios o lugares. Usa formato de tabla visual.",
                        gancho = "¿Cuál es mejor?",
                        hashtags = "#comparativa #review #productos",
                        categoria = "Tecnología"
                    ),
                    IdeaItem(
                        titulo = "Historia de emprendimiento",
                        descripcion = "Cuenta cómo empezaste tu negocio en Cuba. Inspira a otros.",
                        gancho = "Mi historia te inspirará",
                        hashtags = "#emprendimiento #historia #cuba",
                        categoria = "Motivación"
                    ),
                    IdeaItem(
                        titulo = "Día del padre cubano",
                        descripcion = "Idea especial para el tercer domingo de junio. Felicita con un diseño único.",
                        gancho = "El mejor regalo",
                        hashtags = "#diadelpadre #familia #cuba",
                        categoria = "Celebraciones"
                    )
                )
                ideas.forEach { ideaDao.insertIdea(it) }
                _ideasPrecargadas.value = true
            }
        }
    }
}
