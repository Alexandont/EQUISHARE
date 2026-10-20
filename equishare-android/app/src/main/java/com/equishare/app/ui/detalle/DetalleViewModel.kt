package com.equishare.app.ui.detalle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equishare.app.domain.model.Cotizacion
import com.equishare.app.domain.model.Equipo
import com.equishare.app.domain.model.ErrorApp
import com.equishare.app.domain.model.RangoOcupado
import com.equishare.app.domain.model.Resena
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.datosONull
import com.equishare.app.domain.reglas.ReglasNegocio
import com.equishare.app.domain.repositorio.AuthRepository
import com.equishare.app.domain.repositorio.EquipoRepository
import com.equishare.app.domain.repositorio.FavoritosRepository
import com.equishare.app.domain.repositorio.ReservaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class DetalleUiState(
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
    val equipo: Equipo? = null,
    val resenas: List<Resena> = emptyList(),
    val ocupados: List<RangoOcupado> = emptyList(),
    val esFavorito: Boolean = false,
    val esPropio: Boolean = false,
    val seleccion: Pair<LocalDate, LocalDate>? = null,
    val cotizacion: Cotizacion? = null,
    val errorSeleccion: String? = null,
    val cotizando: Boolean = false,
    val reservando: Boolean = false,
    val mensaje: String? = null,
)

class DetalleViewModel(
    estadoGuardado: SavedStateHandle,
    private val equipos: EquipoRepository,
    private val reservas: ReservaRepository,
    private val favoritos: FavoritosRepository,
    private val auth: AuthRepository,
    private val reloj: () -> Instant = { Instant.now() },
) : ViewModel() {

    val equipoId: String = estadoGuardado.get<String>("equipoId").orEmpty()

    private val _ui = MutableStateFlow(DetalleUiState())
    val ui: StateFlow<DetalleUiState> = _ui.asStateFlow()

    init {
        cargar()
        viewModelScope.launch {
            favoritos.observar().collect { ids -> _ui.update { it.copy(esFavorito = equipoId in ids) } }
        }
    }

    fun cargar() {
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val disponibilidad = async { equipos.disponibilidad(equipoId) }
            val resenas = async { equipos.resenas(equipoId) }
            when (val resultado = equipos.obtener(equipoId)) {
                is Resultado.Exito -> _ui.update {
                    it.copy(
                        cargando = false,
                        equipo = resultado.datos,
                        esPropio = resultado.datos.empresaId == auth.sesion.value?.uid,
                        ocupados = disponibilidad.await().datosONull().orEmpty(),
                        resenas = resenas.await().datosONull().orEmpty(),
                    )
                }
                is Resultado.Error -> {
                    disponibilidad.cancel()
                    resenas.cancel()
                    _ui.update { it.copy(cargando = false, error = resultado.error) }
                }
            }
        }
    }

    /** Primer día que el selector permite elegir. */
    fun primerDiaReservable(): LocalDate = ReglasNegocio.primerDiaReservable(reloj())

    fun estaOcupado(dia: LocalDate): Boolean =
        ReglasNegocio.haySolapamiento(_ui.value.ocupados, dia, dia)

    /** Recibe las fechas del DateRangePicker (milisegundos UTC a medianoche). */
    fun seleccionarRango(inicioMillis: Long, finMillis: Long) {
        val inicio = Instant.ofEpochMilli(inicioMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val fin = Instant.ofEpochMilli(finMillis).atZone(ZoneOffset.UTC).toLocalDate()
        val equipo = _ui.value.equipo ?: return
        val error = ReglasNegocio.validarRango(inicio, fin, reloj())
            ?: if (ReglasNegocio.haySolapamiento(_ui.value.ocupados, inicio, fin)) {
                "El rango elegido incluye días ya reservados. Elige otras fechas."
            } else {
                null
            }
        if (error != null) {
            _ui.update { it.copy(seleccion = null, cotizacion = null, errorSeleccion = error) }
            return
        }
        // Vista previa inmediata calculada en el teléfono; luego se confirma con el servidor.
        _ui.update {
            it.copy(
                seleccion = inicio to fin,
                cotizacion = ReglasNegocio.cotizar(equipo.precioDia, inicio, fin),
                errorSeleccion = null,
                cotizando = true,
            )
        }
        viewModelScope.launch {
            when (val r = reservas.cotizar(equipoId, inicio.toString(), fin.toString())) {
                is Resultado.Exito -> _ui.update {
                    if (!r.datos.disponible) {
                        it.copy(cotizando = false, cotizacion = null, seleccion = null, errorSeleccion = "El equipo ya no está disponible en esas fechas.")
                    } else {
                        it.copy(cotizando = false, cotizacion = r.datos)
                    }
                }
                is Resultado.Error -> _ui.update {
                    it.copy(cotizando = false, cotizacion = null, seleccion = null, errorSeleccion = r.error.mensaje)
                }
            }
        }
    }

    fun descartarSeleccion() = _ui.update { it.copy(seleccion = null, cotizacion = null, errorSeleccion = null) }

    fun confirmarReserva() {
        val (inicio, fin) = _ui.value.seleccion ?: return
        if (_ui.value.reservando) return
        _ui.update { it.copy(reservando = true) }
        viewModelScope.launch {
            when (val r = reservas.crear(equipoId, inicio.toString(), fin.toString())) {
                is Resultado.Exito -> {
                    _ui.update {
                        it.copy(
                            reservando = false,
                            seleccion = null,
                            cotizacion = null,
                            mensaje = "Reserva enviada. La empresa arrendadora debe confirmarla; puedes verla en Reservas.",
                        )
                    }
                    equipos.disponibilidad(equipoId).datosONull()?.let { ocupados -> _ui.update { it.copy(ocupados = ocupados) } }
                }
                is Resultado.Error -> _ui.update {
                    it.copy(reservando = false, seleccion = null, cotizacion = null, errorSeleccion = r.error.mensaje)
                }
            }
        }
    }

    fun alternarFavorito() {
        val equipo = _ui.value.equipo ?: return
        viewModelScope.launch { favoritos.establecer(equipo, !_ui.value.esFavorito) }
    }

    fun mensajeMostrado() = _ui.update { it.copy(mensaje = null, errorSeleccion = null) }
}
