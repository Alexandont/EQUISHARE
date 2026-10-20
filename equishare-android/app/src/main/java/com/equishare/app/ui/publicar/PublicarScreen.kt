package com.equishare.app.ui.publicar

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.equishare.app.BuildConfig
import com.equishare.app.domain.model.Categoria
import com.equishare.app.ui.AppViewModelProvider
import com.equishare.app.ui.components.EstadoCargando
import com.equishare.app.ui.components.EtiquetaEstadoEquipo
import com.equishare.app.ui.components.textoAyuda
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

private val TRUJILLO = LatLng(-8.1116, -79.0287)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicarScreen(
    alVolver: () -> Unit,
    viewModel: PublicarViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Selector de fotos del sistema: no necesita permiso de almacenamiento.
    val selectorFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.alElegirFoto(uri?.toString())
    }
    val permisoUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
        if (permisos.values.any { it }) viewModel.usarMiUbicacion()
    }

    val contexto = LocalContext.current
    LaunchedEffect(ui.mensaje, ui.guardadoOk) {
        val texto = ui.mensaje
        if (ui.guardadoOk) {
            // El aviso se muestra como Toast porque la pantalla se cierra al guardar.
            if (texto != null) Toast.makeText(contexto, texto, Toast.LENGTH_LONG).show()
            viewModel.cierreProcesado()
            alVolver()
        } else if (texto != null) {
            snackbar.showSnackbar(texto)
            viewModel.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ui.esEdicion) "Editar equipo" else "Publicar equipo") },
                navigationIcon = {
                    IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { relleno ->
        if (ui.cargandoEquipo) {
            EstadoCargando(modifier = Modifier.padding(relleno))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(relleno)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ui.estadoActual?.let { EtiquetaEstadoEquipo(it) }

            // ---- Foto ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val modelo = ui.fotoUriLocal ?: ui.fotoUrlRemota
                if (modelo != null) {
                    AsyncImage(
                        model = modelo,
                        contentDescription = "Foto del equipo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Card(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(40.dp))
                            Text("Agrega una foto clara del equipo", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    selectorFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (ui.fotoUriLocal != null || ui.fotoUrlRemota != null) "Cambiar foto" else "Elegir foto")
            }
            Text(
                "La foto se valida automáticamente al guardar: JPG, PNG o WebP, mínimo 640×480 px y máximo 5 MB.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (ui.motivosFoto.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("La foto fue rechazada:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        ui.motivosFoto.forEach { Text("• $it", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            // ---- Datos ----
            OutlinedTextField(
                value = ui.nombre,
                onValueChange = viewModel::alCambiarNombre,
                label = { Text("Nombre del equipo") },
                isError = ui.errorNombre != null,
                supportingText = textoAyuda(ui.errorNombre),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Categoría", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Categoria.entries.forEach { c ->
                    FilterChip(selected = ui.categoria == c, onClick = { viewModel.alElegirCategoria(c) }, label = { Text(c.etiqueta) })
                }
            }
            ui.errorCategoria?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            OutlinedTextField(
                value = ui.descripcion,
                onValueChange = viewModel::alCambiarDescripcion,
                label = { Text("Descripción (estado, accesorios, condiciones)") },
                isError = ui.errorDescripcion != null,
                supportingText = textoAyuda(ui.errorDescripcion ?: "${ui.descripcion.trim().length}/500"),
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.precio,
                onValueChange = viewModel::alCambiarPrecio,
                label = { Text("Precio por día (S/)") },
                isError = ui.errorPrecio != null,
                supportingText = textoAyuda(ui.errorPrecio),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = ui.direccion,
                onValueChange = viewModel::alCambiarDireccion,
                label = { Text("Dirección de recojo (referencial)") },
                isError = ui.errorDireccion != null,
                supportingText = textoAyuda(ui.errorDireccion),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ---- Ubicación (Google Maps / GPS) ----
            Text("Ubicación del equipo", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = {
                    if (viewModel.tienePermisoUbicacion()) {
                        viewModel.usarMiUbicacion()
                    } else {
                        permisoUbicacion.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                    }
                },
                enabled = !ui.obteniendoUbicacion,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (ui.obteniendoUbicacion) "Obteniendo ubicación…" else "Usar mi ubicación actual")
            }
            if (BuildConfig.MAPS_ENABLED) {
                SelectorEnMapa(ui.lat, ui.lng, viewModel::alElegirPunto)
                Text("También puedes tocar el mapa para ubicar el equipo.", style = MaterialTheme.typography.bodySmall)
            }
            val lat = ui.lat
            val lng = ui.lng
            Text(
                if (lat != null && lng != null) {
                    String.format(Locale.US, "Coordenadas: %.5f, %.5f", lat, lng)
                } else {
                    "Aún no se indicó la ubicación."
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (ui.errorUbicacion != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ui.errorUbicacion?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Visible en el catálogo", style = MaterialTheme.typography.bodyLarge)
                    Text("Puedes ocultarlo temporalmente sin borrarlo.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = ui.visible, onCheckedChange = viewModel::alCambiarVisible)
            }

            Button(
                onClick = viewModel::guardar,
                enabled = !ui.guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (ui.guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(12.dp))
                    Text(ui.pasoGuardado ?: "Guardando…")
                } else {
                    Text(if (ui.esEdicion) "Guardar cambios" else "Publicar")
                }
            }
        }
    }
}

@Composable
private fun SelectorEnMapa(lat: Double?, lng: Double?, alElegir: (Double, Double) -> Unit) {
    val punto = if (lat != null && lng != null) LatLng(lat, lng) else null
    val camara = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(punto ?: TRUJILLO, 13f) }
    LaunchedEffect(punto) {
        if (punto != null) camara.position = CameraPosition.fromLatLngZoom(punto, camara.position.zoom)
    }
    GoogleMap(
        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
        cameraPositionState = camara,
        uiSettings = MapUiSettings(zoomControlsEnabled = true, mapToolbarEnabled = false),
        onMapClick = { alElegir(it.latitude, it.longitude) },
    ) {
        if (punto != null) {
            val marcador = remember(punto) { MarkerState(position = punto) }
            Marker(state = marcador, title = "Ubicación del equipo")
        }
    }
}
