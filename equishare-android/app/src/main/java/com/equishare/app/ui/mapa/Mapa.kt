package com.equishare.app.ui.mapa

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equishare.app.BuildConfig
import com.equishare.app.domain.Formato
import com.equishare.app.domain.model.Coordenadas
import com.equishare.app.domain.model.Equipo
import com.equishare.app.domain.repositorio.EquipoRepository
import com.equishare.app.domain.repositorio.ProveedorUbicacion
import com.equishare.app.ui.AppViewModelProvider
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapaUiState(
    val equipos: List<Equipo> = emptyList(),
    val miUbicacion: Coordenadas? = null,
    val permisoConcedido: Boolean = false,
    val mensaje: String? = null,
)

class MapaViewModel(
    private val equipos: EquipoRepository,
    private val ubicacion: ProveedorUbicacion,
) : ViewModel() {
    private val extra = MutableStateFlow(MapaUiState(permisoConcedido = ubicacion.tienePermiso()))

    val ui: StateFlow<MapaUiState> = combine(equipos.observarCatalogo(), extra) { lista, e -> e.copy(equipos = lista) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapaUiState())

    init {
        // El mapa muestra los mismos equipos del catálogo guardado en Room (con los filtros activos).
        if (ubicacion.tienePermiso()) actualizarUbicacion()
    }

    fun alResultadoPermiso(concedido: Boolean) {
        extra.update {
            it.copy(
                permisoConcedido = concedido,
                mensaje = if (concedido) null else "Sin permiso de ubicación: el mapa funciona, pero no muestra tu posición.",
            )
        }
        if (concedido) actualizarUbicacion()
    }

    fun actualizarUbicacion() {
        viewModelScope.launch {
            val c = ubicacion.ubicacionActual()
            extra.update { it.copy(miUbicacion = c, permisoConcedido = ubicacion.tienePermiso()) }
        }
    }

    fun tienePermiso(): Boolean = ubicacion.tienePermiso()
}

private val TRUJILLO = LatLng(-8.1116, -79.0287)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    alAbrirEquipo: (String) -> Unit,
    viewModel: MapaViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        viewModel.alResultadoPermiso(r.values.any { it })
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Equipos en el mapa") }) },
        floatingActionButton = {
            if (BuildConfig.MAPS_ENABLED) {
                FloatingActionButton(onClick = {
                    if (viewModel.tienePermiso()) {
                        viewModel.actualizarUbicacion()
                    } else {
                        permiso.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    }
                }) { Icon(Icons.Filled.MyLocation, contentDescription = "Mostrar mi ubicación") }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { relleno ->
        Box(Modifier.fillMaxSize().padding(relleno)) {
            if (!BuildConfig.MAPS_ENABLED) {
                SinClaveMaps(ui.equipos, alAbrirEquipo)
            } else {
                val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(TRUJILLO, 12f) }
                val miPosicion = ui.miUbicacion
                LaunchedEffect(miPosicion) {
                    if (miPosicion != null) {
                        camara.position = CameraPosition.fromLatLngZoom(LatLng(miPosicion.lat, miPosicion.lng), 13f)
                    }
                }
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = camara,
                    properties = MapProperties(isMyLocationEnabled = ui.permisoConcedido),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = false, mapToolbarEnabled = false),
                ) {
                    ui.equipos.forEach { equipo ->
                        key(equipo.id) {
                            val estado = remember(equipo.id, equipo.lat, equipo.lng) { MarkerState(position = LatLng(equipo.lat, equipo.lng)) }
                            Marker(
                                state = estado,
                                title = equipo.nombre,
                                snippet = "${Formato.soles(equipo.precioDia)} / día · toca para ver",
                                onInfoWindowClick = { alAbrirEquipo(equipo.id) },
                            )
                        }
                    }
                }
                ui.mensaje?.let {
                    Card(
                        modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/** Alternativa cuando el mapa no está disponible: lista de equipos con su dirección. */
@Composable
private fun SinClaveMaps(equipos: List<Equipo>, alAbrirEquipo: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Text(
                "El mapa no está disponible en este momento. Estas son las ubicaciones de los equipos:",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        equipos.take(20).forEach { e ->
            TextButton(onClick = { alAbrirEquipo(e.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("${e.nombre} · ${e.direccion}")
            }
        }
    }
}
