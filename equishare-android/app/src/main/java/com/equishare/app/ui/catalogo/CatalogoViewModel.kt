package com.equishare.app.ui.catalogo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equishare.app.domain.model.Categoria
import com.equishare.app.domain.model.Coordenadas
import com.equishare.app.domain.model.Equipo
import com.equishare.app.domain.model.ErrorApp
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.repositorio.AjustesRepository
import com.equishare.app.domain.repositorio.EquipoRepository
import com.equishare.app.domain.repositorio.FavoritosRepository
import com.equishare.app.domain.repositorio.FiltroCatalogo
import com.equishare.app.domain.repositorio.MonitorRed
import com.equishare.app.domain.repositorio.ProveedorUbicacion
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FiltrosUi(
    val texto: String = "",
    val categoria: Categoria? = null,
    val cercaDeMi: Boolean = false,
    val ubicacion: Coordenadas? = null,
)

data class CatalogoUiState(
    val equipos: List<Equipo> = emptyList(),
    val filtros: FiltrosUi = FiltrosUi(),
    val favoritos: Set<String> = emptySet(),
    val enLinea: Boolean = true,
    val cargando: Boolean = false,
    val error: ErrorApp? = null,
    val mensaje: String? = null,
) {
    /** Primera carga sin datos guardados. */
    val mostrarCargaInicial: Boolean get() = cargando && equipos.isEmpty()
    val mostrarErrorPantalla: Boolean get() = error != null && equipos.isEmpty() && !cargando
    val mostrarVacio: Boolean get() = !cargando && error == null && equipos.isEmpty()
}

private data class EstadoInterno(val cargando: Boolean = false, val error: ErrorApp? = null, val mensaje: String? = null)

/**
 * MVVM: el ViewModel combina el catálogo guardado en Room, los favoritos de Firestore y el estado
 * de la red en un único UI State inmutable que la pantalla solo dibuja.
 */
@OptIn(FlowPreview::class)
class CatalogoViewModel(
    private val equipos: EquipoRepository,
    private val favoritos: FavoritosRepository,
    private val red: MonitorRed,
    private val ubicacion: ProveedorUbicacion,
    private val ajustes: AjustesRepository,
) : ViewModel() {

    private val filtros = MutableStateFlow(FiltrosUi())
    private val interno = MutableStateFlow(EstadoInterno(cargando = true))

    val ui: StateFlow<CatalogoUiState> = combine(
        equipos.observarCatalogo(),
        favoritos.observar(),
        red.enLinea,
        filtros,
        interno,
    ) { lista, favs, enLinea, f, estado ->
        CatalogoUiState(
            equipos = lista,
            filtros = f,
            favoritos = favs,
            enLinea = enLinea,
            cargando = estado.cargando,
            error = estado.error,
            mensaje = estado.mensaje,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogoUiState(cargando = true))

    init {
        viewModelScope.launch {
            val categoriaGuardada = ajustes.categoriaFiltro.first()
            filtros.update { it.copy(categoria = categoriaGuardada) }
            // Cada cambio de filtros vuelve a consultar la API (la búsqueda espera 400 ms de inactividad).
            filtros.debounce(400).distinctUntilChanged().collectLatest { refrescar(it) }
        }
        viewModelScope.launch {
            // Al recuperar la conexión se actualiza automáticamente.
            red.enLinea.distinctUntilChanged().drop(1).filter { it }.collect { refrescar(filtros.value) }
        }
    }

    fun alCambiarTexto(texto: String) = filtros.update { it.copy(texto = texto) }

    fun alElegirCategoria(categoria: Categoria?) {
        filtros.update { it.copy(categoria = categoria) }
        viewModelScope.launch { ajustes.guardarCategoriaFiltro(categoria) }
    }

    /** Se llama después de que el usuario concede (o no) el permiso de ubicación. */
    fun alCambiarCercaDeMi(activar: Boolean) {
        if (!activar) {
            filtros.update { it.copy(cercaDeMi = false, ubicacion = null) }
            return
        }
        viewModelScope.launch {
            val coordenadas = ubicacion.ubicacionActual()
            if (coordenadas == null) {
                interno.update { it.copy(mensaje = "No se pudo obtener tu ubicación. Revisa el permiso y el GPS.") }
            } else {
                filtros.update { it.copy(cercaDeMi = true, ubicacion = coordenadas) }
            }
        }
    }

    fun tienePermisoUbicacion(): Boolean = ubicacion.tienePermiso()

    fun recargar() {
        viewModelScope.launch { refrescar(filtros.value) }
    }

    fun alternarFavorito(equipo: Equipo) {
        val esFavorito = equipo.id in ui.value.favoritos
        viewModelScope.launch { favoritos.establecer(equipo, !esFavorito) }
    }

    fun mensajeMostrado() = interno.update { it.copy(mensaje = null) }

    private suspend fun refrescar(f: FiltrosUi) {
        interno.update { it.copy(cargando = true) }
        val resultado = equipos.refrescarCatalogo(
            FiltroCatalogo(
                texto = f.texto,
                categoria = f.categoria,
                ubicacion = if (f.cercaDeMi) f.ubicacion else null,
                radioKm = if (f.cercaDeMi) RADIO_CERCANIA_KM else null,
            ),
        )
        interno.update {
            when (resultado) {
                is Resultado.Exito -> EstadoInterno(cargando = false)
                is Resultado.Error -> EstadoInterno(
                    cargando = false,
                    error = resultado.error,
                    mensaje = if (ui.value.equipos.isNotEmpty()) "${resultado.error.mensaje} Se muestran los datos guardados." else null,
                )
            }
        }
    }

    companion object {
        const val RADIO_CERCANIA_KM = 15.0
    }
}
