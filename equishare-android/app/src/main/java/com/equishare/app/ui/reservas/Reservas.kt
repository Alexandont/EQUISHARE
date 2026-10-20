package com.equishare.app.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equishare.app.domain.Formato
import com.equishare.app.domain.model.ErrorApp
import com.equishare.app.domain.model.EstadoReserva
import com.equishare.app.domain.model.Reserva
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.Rol
import com.equishare.app.domain.reglas.ReglasNegocio
import com.equishare.app.domain.repositorio.MonitorRed
import com.equishare.app.domain.repositorio.ReservaRepository
import com.equishare.app.domain.validacion.Validadores
import com.equishare.app.ui.AppViewModelProvider
import com.equishare.app.ui.components.BannerSinConexion
import com.equishare.app.ui.components.EstadoCargando
import com.equishare.app.ui.components.EstadoError
import com.equishare.app.ui.components.EstadoVacio
import com.equishare.app.ui.components.EtiquetaEstadoReserva
import com.equishare.app.ui.components.SelectorEstrellas
import com.equishare.app.ui.components.textoAyuda
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class ReservasUiState(
    val rol: Rol = Rol.ARRENDATARIO,
    val reservas: List<Reserva> = emptyList(),
    val cargando: Boolean = false,
    val error: ErrorApp? = null,
    val enLinea: Boolean = true,
    val mensaje: String? = null,
    val procesandoId: String? = null,
)

private data class InternoReservas(
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
    val mensaje: String? = null,
    val procesandoId: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ReservasViewModel(
    private val reservas: ReservaRepository,
    red: MonitorRed,
    private val reloj: () -> Instant = { Instant.now() },
) : ViewModel() {
    private val rol = MutableStateFlow(Rol.ARRENDATARIO)
    private val interno = MutableStateFlow(InternoReservas())

    val ui: StateFlow<ReservasUiState> = combine(
        rol.flatMapLatest { reservas.observarMias(it) },
        rol,
        red.enLinea,
        interno,
    ) { lista, r, enLinea, i ->
        ReservasUiState(r, lista, i.cargando, i.error, enLinea, i.mensaje, i.procesandoId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReservasUiState(cargando = true))

    init {
        recargar()
    }

    fun cambiarRol(nuevo: Rol) {
        rol.value = nuevo
        recargar()
    }

    fun recargar() {
        interno.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = reservas.refrescarMias(rol.value)) {
                is Resultado.Exito -> interno.update { it.copy(cargando = false) }
                is Resultado.Error -> interno.update { it.copy(cargando = false, error = r.error) }
            }
        }
    }

    /** Penalidad que se cobraría si se cancela ahora (RN-08), para avisar antes de confirmar. */
    fun penalidadSiCancela(reserva: Reserva): Double =
        ReglasNegocio.penalidadSiCancela(LocalDate.parse(reserva.fechaInicio), reserva.total, reloj())

    fun puedeCalificar(reserva: Reserva): Boolean {
        val devuelta = reserva.devueltaEn?.let { runCatching { Instant.parse(it) }.getOrNull() }
        return reserva.estado == EstadoReserva.DEVUELTA && !reserva.resenaEnviada &&
            ReglasNegocio.dentroDeVentanaResena(devuelta, reloj())
    }

    fun cancelar(reserva: Reserva) = ejecutar(reserva.id, "Reserva cancelada.") { reservas.cancelar(reserva.id) }

    fun avanzarEstado(reserva: Reserva) {
        val siguiente = reserva.estado.siguienteParaArrendador() ?: return
        ejecutar(reserva.id, "Reserva actualizada a \"${siguiente.etiqueta}\".") { reservas.cambiarEstado(reserva.id, siguiente) }
    }

    fun calificar(reserva: Reserva, estrellas: Int, comentario: String) =
        ejecutar(reserva.id, "¡Gracias! Tu calificación fue registrada.") { reservas.calificar(reserva.id, estrellas, comentario) }

    private fun <T> ejecutar(id: String, exito: String, accion: suspend () -> Resultado<T>) {
        if (interno.value.procesandoId != null) return
        interno.update { it.copy(procesandoId = id) }
        viewModelScope.launch {
            when (val r = accion()) {
                is Resultado.Exito -> interno.update { it.copy(procesandoId = null, mensaje = exito) }
                is Resultado.Error -> interno.update { it.copy(procesandoId = null, mensaje = r.error.mensaje) }
            }
        }
    }

    fun mensajeMostrado() = interno.update { it.copy(mensaje = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservasScreen(viewModel: ReservasViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var porCancelar by remember { mutableStateOf<Reserva?>(null) }
    var porCalificar by remember { mutableStateOf<Reserva?>(null) }

    LaunchedEffect(ui.mensaje) {
        val texto = ui.mensaje ?: return@LaunchedEffect
        snackbar.showSnackbar(texto)
        viewModel.mensajeMostrado()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservas") },
                actions = { IconButton(onClick = viewModel::recargar) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            TabRow(selectedTabIndex = ui.rol.ordinal) {
                Tab(selected = ui.rol == Rol.ARRENDATARIO, onClick = { viewModel.cambiarRol(Rol.ARRENDATARIO) }, text = { Text("Lo que alquilo") })
                Tab(selected = ui.rol == Rol.ARRENDADOR, onClick = { viewModel.cambiarRol(Rol.ARRENDADOR) }, text = { Text("Mis equipos alquilados") })
            }
            BannerSinConexion(!ui.enLinea)
            if (ui.cargando && ui.reservas.isNotEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                ui.cargando && ui.reservas.isEmpty() -> EstadoCargando()
                ui.error != null && ui.reservas.isEmpty() -> EstadoError(ui.error?.mensaje ?: "Error", alReintentar = viewModel::recargar)
                ui.reservas.isEmpty() -> EstadoVacio(
                    "Sin reservas",
                    if (ui.rol == Rol.ARRENDATARIO) "Cuando reserves un equipo aparecerá aquí." else "Cuando otra empresa reserve tus equipos aparecerá aquí.",
                )
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(ui.reservas, key = { it.id }) { reserva ->
                        TarjetaReserva(
                            reserva = reserva,
                            procesando = ui.procesandoId == reserva.id,
                            puedeCalificar = viewModel.puedeCalificar(reserva),
                            alCancelar = { porCancelar = reserva },
                            alAvanzar = { viewModel.avanzarEstado(reserva) },
                            alCalificar = { porCalificar = reserva },
                        )
                    }
                }
            }
        }
    }

    porCancelar?.let { reserva ->
        val penalidad = viewModel.penalidadSiCancela(reserva)
        AlertDialog(
            onDismissRequest = { porCancelar = null },
            title = { Text("¿Cancelar la reserva?") },
            text = {
                Text(
                    if (penalidad > 0) {
                        "Faltan menos de 12 horas para la entrega: se aplicará una penalidad de ${Formato.soles(penalidad)} (20 %, RN-08)."
                    } else {
                        "Aún estás a tiempo: la cancelación no tiene costo."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelar(reserva)
                    porCancelar = null
                }) { Text("Sí, cancelar") }
            },
            dismissButton = { TextButton(onClick = { porCancelar = null }) { Text("No") } },
        )
    }

    porCalificar?.let { reserva ->
        DialogoCalificar(
            titulo = if (reserva.rolSolicitante == Rol.ARRENDATARIO) "Califica el equipo y a ${reserva.arrendadorNombre}" else "Califica a ${reserva.arrendatarioNombre}",
            alCerrar = { porCalificar = null },
            alEnviar = { estrellas, comentario ->
                viewModel.calificar(reserva, estrellas, comentario)
                porCalificar = null
            },
        )
    }
}

@Composable
private fun TarjetaReserva(
    reserva: Reserva,
    procesando: Boolean,
    puedeCalificar: Boolean,
    alCancelar: () -> Unit,
    alAvanzar: () -> Unit,
    alCalificar: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(reserva.equipoNombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                EtiquetaEstadoReserva(reserva.estado)
            }
            Text(
                if (reserva.rolSolicitante == Rol.ARRENDATARIO) "Arrendador: ${reserva.arrendadorNombre}" else "Cliente: ${reserva.arrendatarioNombre}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("${Formato.rango(reserva.fechaInicio, reserva.fechaFin)} · ${reserva.dias} día(s)", style = MaterialTheme.typography.bodyMedium)
            if (reserva.rolSolicitante == Rol.ARRENDADOR) {
                Text(
                    "Total ${Formato.soles(reserva.total)} · Comisión 12 %: ${Formato.soles(reserva.comision)} · Recibes ${Formato.soles(reserva.montoArrendador)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text("Total: ${Formato.soles(reserva.total)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            if (reserva.penalidad > 0) {
                Text("Penalidad por cancelación tardía: ${Formato.soles(reserva.penalidad)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (procesando) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val siguiente = reserva.estado.siguienteParaArrendador()
                if (reserva.rolSolicitante == Rol.ARRENDADOR && siguiente != null) {
                    Button(onClick = alAvanzar, enabled = !procesando) {
                        Text(
                            when (siguiente) {
                                EstadoReserva.CONFIRMADA -> "Confirmar"
                                EstadoReserva.EN_CURSO -> "Marcar entregado"
                                EstadoReserva.DEVUELTA -> "Marcar devuelto"
                                else -> siguiente.etiqueta
                            },
                        )
                    }
                }
                if (reserva.estado.esCancelable) {
                    OutlinedButton(onClick = alCancelar, enabled = !procesando) { Text("Cancelar") }
                }
                if (puedeCalificar) {
                    Button(onClick = alCalificar, enabled = !procesando) { Text("Calificar") }
                }
            }
        }
    }
}

@Composable
private fun DialogoCalificar(titulo: String, alCerrar: () -> Unit, alEnviar: (Int, String) -> Unit) {
    var estrellas by remember { mutableIntStateOf(0) }
    var comentario by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tienes 48 horas después de la devolución para calificar (RN-05).", style = MaterialTheme.typography.bodySmall)
                SelectorEstrellas(estrellas) { estrellas = it; error = null }
                OutlinedTextField(
                    value = comentario,
                    onValueChange = { comentario = it; error = null },
                    label = { Text("Comentario") },
                    supportingText = textoAyuda(error),
                    isError = error != null,
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val e = Validadores.estrellas(estrellas) ?: Validadores.comentario(comentario)
                if (e != null) error = e else alEnviar(estrellas, comentario)
            }) { Text("Enviar") }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cancelar") } },
    )
}
