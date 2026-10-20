package com.equishare.app.ui.publicar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equishare.app.domain.model.Categoria
import com.equishare.app.domain.model.EquipoEntrada
import com.equishare.app.domain.model.EstadoEquipo
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.repositorio.EquipoRepository
import com.equishare.app.domain.repositorio.ProveedorUbicacion
import com.equishare.app.domain.validacion.Validadores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PublicarUiState(
    val esEdicion: Boolean = false,
    val cargandoEquipo: Boolean = false,
    val nombre: String = "",
    val categoria: Categoria? = null,
    val descripcion: String = "",
    val precio: String = "",
    val direccion: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val visible: Boolean = true,
    val fotoUriLocal: String? = null,
    val fotoUrlRemota: String? = null,
    val fotoIdActual: String? = null,
    val estadoActual: EstadoEquipo? = null,
    val errorNombre: String? = null,
    val errorCategoria: String? = null,
    val errorDescripcion: String? = null,
    val errorPrecio: String? = null,
    val errorDireccion: String? = null,
    val errorUbicacion: String? = null,
    val motivosFoto: List<String> = emptyList(),
    val obteniendoUbicacion: Boolean = false,
    val guardando: Boolean = false,
    val pasoGuardado: String? = null,
    val mensaje: String? = null,
    val guardadoOk: Boolean = false,
)

class PublicarViewModel(
    estadoGuardado: SavedStateHandle,
    private val equipos: EquipoRepository,
    private val ubicacion: ProveedorUbicacion,
) : ViewModel() {

    private val equipoId: String? = estadoGuardado.get<String>("equipoId")?.takeIf { it.isNotBlank() }

    private val _ui = MutableStateFlow(PublicarUiState(esEdicion = equipoId != null, cargandoEquipo = equipoId != null))
    val ui: StateFlow<PublicarUiState> = _ui.asStateFlow()

    init {
        if (equipoId != null) cargarEquipo(equipoId)
    }

    private fun cargarEquipo(id: String) {
        viewModelScope.launch {
            when (val r = equipos.obtener(id)) {
                is Resultado.Exito -> {
                    val e = r.datos
                    _ui.update {
                        it.copy(
                            cargandoEquipo = false,
                            nombre = e.nombre,
                            categoria = e.categoria,
                            descripcion = e.descripcion,
                            precio = e.precioDia.toString(),
                            direccion = e.direccion,
                            lat = e.lat,
                            lng = e.lng,
                            visible = e.estado != EstadoEquipo.OCULTO,
                            fotoUrlRemota = e.fotoUrl,
                            fotoIdActual = e.fotoId,
                            estadoActual = e.estado,
                        )
                    }
                }
                is Resultado.Error -> _ui.update { it.copy(cargandoEquipo = false, mensaje = r.error.mensaje) }
            }
        }
    }

    fun alCambiarNombre(v: String) = _ui.update { it.copy(nombre = v, errorNombre = null) }
    fun alElegirCategoria(c: Categoria) = _ui.update { it.copy(categoria = c, errorCategoria = null) }
    fun alCambiarDescripcion(v: String) = _ui.update { it.copy(descripcion = v, errorDescripcion = null) }
    fun alCambiarPrecio(v: String) = _ui.update { it.copy(precio = v.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(9), errorPrecio = null) }
    fun alCambiarDireccion(v: String) = _ui.update { it.copy(direccion = v, errorDireccion = null) }
    fun alCambiarVisible(v: Boolean) = _ui.update { it.copy(visible = v) }
    fun alElegirFoto(uri: String?) {
        if (uri != null) _ui.update { it.copy(fotoUriLocal = uri, motivosFoto = emptyList()) }
    }

    fun alElegirPunto(lat: Double, lng: Double) = _ui.update { it.copy(lat = lat, lng = lng, errorUbicacion = null) }

    fun usarMiUbicacion() {
        _ui.update { it.copy(obteniendoUbicacion = true) }
        viewModelScope.launch {
            val c = ubicacion.ubicacionActual()
            _ui.update {
                if (c == null) {
                    it.copy(obteniendoUbicacion = false, mensaje = "No se pudo obtener la ubicación. Activa el GPS o elige el punto en el mapa.")
                } else {
                    it.copy(obteniendoUbicacion = false, lat = c.lat, lng = c.lng, errorUbicacion = null)
                }
            }
        }
    }

    fun tienePermisoUbicacion(): Boolean = ubicacion.tienePermiso()

    private fun validar(): Boolean {
        val e = _ui.value
        val validado = e.copy(
            errorNombre = Validadores.nombreEquipo(e.nombre),
            errorCategoria = if (e.categoria == null) "Elige una categoría." else null,
            errorDescripcion = Validadores.descripcion(e.descripcion),
            errorPrecio = Validadores.precio(e.precio),
            errorDireccion = Validadores.direccion(e.direccion),
            errorUbicacion = if (e.lat == null || e.lng == null) "Indica la ubicación del equipo." else null,
        )
        _ui.value = validado
        return listOf(
            validado.errorNombre, validado.errorCategoria, validado.errorDescripcion,
            validado.errorPrecio, validado.errorDireccion, validado.errorUbicacion,
        ).all { it == null }
    }

    fun guardar() {
        if (_ui.value.guardando || !validar()) return
        _ui.update { it.copy(guardando = true, motivosFoto = emptyList()) }
        viewModelScope.launch {
            val e = _ui.value
            var fotoId = e.fotoIdActual
            val uriLocal = e.fotoUriLocal
            if (uriLocal != null) {
                _ui.update { it.copy(pasoGuardado = "Validando la foto…") }
                when (val subida = equipos.subirFoto(uriLocal)) {
                    is Resultado.Exito -> fotoId = subida.datos.id
                    is Resultado.Error -> {
                        // RN-04: la foto rechazada explica el motivo y no se publica.
                        _ui.update {
                            it.copy(
                                guardando = false,
                                pasoGuardado = null,
                                motivosFoto = subida.error.detalles.ifEmpty { listOf(subida.error.mensaje) },
                            )
                        }
                        return@launch
                    }
                }
            }
            _ui.update { it.copy(pasoGuardado = "Guardando el equipo…") }
            val entrada = EquipoEntrada(
                nombre = e.nombre,
                categoria = e.categoria ?: Categoria.HERRAMIENTAS,
                descripcion = e.descripcion,
                precioDia = Validadores.precioComoNumero(e.precio) ?: 0.0,
                lat = e.lat ?: 0.0,
                lng = e.lng ?: 0.0,
                direccion = e.direccion,
                fotoId = fotoId,
                visible = e.visible,
            )
            val resultado = if (equipoId == null) equipos.crear(entrada) else equipos.actualizar(equipoId, entrada)
            when (resultado) {
                is Resultado.Exito -> {
                    val aviso = when (resultado.datos.estado) {
                        EstadoEquipo.PENDIENTE_FOTO -> "Guardado. Agrega una foto aprobada para que aparezca en el catálogo (RN-04)."
                        EstadoEquipo.EN_REVISION -> "Guardado. El equipo sigue en revisión por calificaciones bajas (RN-06)."
                        EstadoEquipo.OCULTO -> "Guardado como oculto."
                        EstadoEquipo.PUBLICADO -> "¡Equipo publicado en el catálogo!"
                    }
                    _ui.update { it.copy(guardando = false, pasoGuardado = null, guardadoOk = true, mensaje = aviso) }
                }
                is Resultado.Error -> _ui.update {
                    it.copy(
                        guardando = false,
                        pasoGuardado = null,
                        mensaje = (listOf(resultado.error.mensaje) + resultado.error.detalles).joinToString(" "),
                    )
                }
            }
        }
    }

    fun mensajeMostrado() = _ui.update { it.copy(mensaje = null) }

    /** Evita que la pantalla procese dos veces el cierre tras guardar. */
    fun cierreProcesado() = _ui.update { it.copy(mensaje = null, guardadoOk = false) }
}
