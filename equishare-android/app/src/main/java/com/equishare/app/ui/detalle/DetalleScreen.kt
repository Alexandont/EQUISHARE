package com.equishare.app.ui.detalle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equishare.app.BuildConfig
import com.equishare.app.domain.Formato
import com.equishare.app.domain.model.Cotizacion
import com.equishare.app.domain.model.Equipo
import com.equishare.app.domain.reglas.ReglasNegocio
import com.equishare.app.ui.AppViewModelProvider
import com.equishare.app.ui.components.EstadoCargando
import com.equishare.app.ui.components.EstadoError
import com.equishare.app.ui.components.Estrellas
import com.equishare.app.ui.components.EtiquetaEstadoEquipo
import com.equishare.app.ui.components.ImagenEquipo
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(
    alVolver: () -> Unit,
    alEditar: (String) -> Unit,
    viewModel: DetalleViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var mostrarCalendario by remember { mutableStateOf(false) }

    LaunchedEffect(ui.mensaje, ui.errorSeleccion) {
        val texto = ui.mensaje ?: ui.errorSeleccion
        if (texto != null) {
            snackbar.showSnackbar(texto)
            viewModel.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del equipo") },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    if (ui.equipo != null) {
                        IconButton(onClick = viewModel::alternarFavorito) {
                            Icon(
                                if (ui.esFavorito) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (ui.esFavorito) "Quitar de favoritos" else "Agregar a favoritos",
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            val equipo = ui.equipo
            if (equipo != null) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(Formato.soles(equipo.precioDia), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("por día", style = MaterialTheme.typography.bodySmall)
                        }
                        if (ui.esPropio) {
                            Button(onClick = { alEditar(equipo.id) }) {
                                Icon(Icons.Filled.Edit, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Editar equipo")
                            }
                        } else {
                            Button(
                                onClick = { mostrarCalendario = true },
                                enabled = !ui.reservando && !ui.cotizando,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            ) {
                                Icon(Icons.Filled.DateRange, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Elegir fechas")
                            }
                        }
                    }
                }
            }
        },
    ) { relleno ->
        val equipo = ui.equipo
        when {
            ui.cargando -> EstadoCargando(modifier = Modifier.padding(relleno))
            equipo == null -> EstadoError(
                mensaje = ui.error?.mensaje ?: "No se encontró el equipo.",
                modifier = Modifier.padding(relleno),
                alReintentar = viewModel::cargar,
            )
            else -> ContenidoDetalle(equipo, ui, Modifier.padding(relleno))
        }
    }

    if (mostrarCalendario) {
        val primerDia = viewModel.primerDiaReservable()
        val ocupados = ui.ocupados
        val estadoFechas = rememberDateRangePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val dia = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !dia.isBefore(primerDia) && !ReglasNegocio.haySolapamiento(ocupados, dia, dia)
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(
                    enabled = estadoFechas.selectedStartDateMillis != null,
                    onClick = {
                        val inicio = estadoFechas.selectedStartDateMillis
                        if (inicio != null) {
                            viewModel.seleccionarRango(inicio, estadoFechas.selectedEndDateMillis ?: inicio)
                        }
                        mostrarCalendario = false
                    },
                ) { Text("Ver costo") }
            },
            dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } },
        ) {
            DateRangePicker(
                state = estadoFechas,
                title = { Text("Fechas del alquiler", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    val cotizacion = ui.cotizacion
    val seleccion = ui.seleccion
    if (cotizacion != null && seleccion != null) {
        DialogoCotizacion(
            cotizacion = cotizacion,
            rango = Formato.rango(seleccion.first.toString(), seleccion.second.toString()),
            cargando = ui.cotizando || ui.reservando,
            alConfirmar = viewModel::confirmarReserva,
            alCancelar = viewModel::descartarSeleccion,
        )
    }
}

@Composable
private fun ContenidoDetalle(equipo: Equipo, ui: DetalleUiState, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ImagenEquipo(
                url = equipo.fotoUrl,
                categoria = equipo.categoria,
                descripcion = "Foto de ${equipo.nombre}",
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(equipo.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("${equipo.categoria.etiqueta} · ${equipo.empresaNombre}", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Estrellas(equipo.calificacionPromedio)
                    Spacer(Modifier.width(8.dp))
                    Text(Formato.calificacion(equipo.calificacionPromedio, equipo.totalResenas), style = MaterialTheme.typography.bodySmall)
                }
                if (ui.esPropio) EtiquetaEstadoEquipo(equipo.estado)
            }
        }
        item {
            Text(equipo.descripcion, style = MaterialTheme.typography.bodyLarge)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(equipo.direccion, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (BuildConfig.MAPS_ENABLED) {
            item { MapaPequeno(equipo) }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Disponibilidad", style = MaterialTheme.typography.titleSmall)
                    if (ui.ocupados.isEmpty()) {
                        Text("No tiene reservas activas. Todas las fechas futuras están libres.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Fechas ocupadas:", style = MaterialTheme.typography.bodySmall)
                        ui.ocupados.forEach { Text("• ${Formato.rango(it.fechaInicio, it.fechaFin)}", style = MaterialTheme.typography.bodySmall) }
                    }
                    Text(
                        "Entrega a las 08:00 del primer día. Reserva con al menos 24 horas de anticipación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            Text("Reseñas de otras empresas", style = MaterialTheme.typography.titleMedium)
        }
        if (ui.resenas.isEmpty()) {
            item { Text("Este equipo aún no tiene reseñas.", style = MaterialTheme.typography.bodySmall) }
        } else {
            items(ui.resenas, key = { it.id }) { resena ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Estrellas(resena.estrellas.toDouble(), 14.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(resena.autorNombre, style = MaterialTheme.typography.labelLarge)
                    }
                    Text(resena.comentario, style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun MapaPequeno(equipo: Equipo) {
    val posicion = LatLng(equipo.lat, equipo.lng)
    val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(posicion, 14f) }
    val marcador = remember(equipo.id) { MarkerState(position = posicion) }
    GoogleMap(
        modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp)),
        cameraPositionState = camara,
        googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
    ) {
        Marker(state = marcador, title = equipo.nombre)
    }
}

@Composable
private fun DialogoCotizacion(
    cotizacion: Cotizacion,
    rango: String,
    cargando: Boolean,
    alConfirmar: () -> Unit,
    alCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Resumen de la reserva") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(rango, style = MaterialTheme.typography.titleSmall)
                FilaMonto("${cotizacion.dias} día(s) × ${Formato.soles(cotizacion.precioDia)}", Formato.soles(cotizacion.subtotal))
                HorizontalDivider()
                FilaMonto("Total a pagar", Formato.soles(cotizacion.total), destacado = true)
                Text(
                    "Sin cargos adicionales ni servicios preseleccionados. La comisión de EQUISHARE (12 % = " +
                        "${Formato.soles(cotizacion.comision)}) la paga la empresa arrendadora.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Puedes cancelar sin costo hasta 12 horas antes de la entrega; después se cobra el 20 %.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (cargando) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        },
        confirmButton = {
            Button(onClick = alConfirmar, enabled = !cargando) { Text("Confirmar reserva") }
        },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } },
    )
}

@Composable
private fun FilaMonto(etiqueta: String, monto: String, destacado: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = if (destacado) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
        Text(monto, style = if (destacado) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium, fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal)
    }
}
