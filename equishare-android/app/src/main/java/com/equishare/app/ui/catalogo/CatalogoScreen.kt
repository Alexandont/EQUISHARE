package com.equishare.app.ui.catalogo

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equishare.app.domain.model.Categoria
import com.equishare.app.ui.AppViewModelProvider
import com.equishare.app.ui.components.BannerSinConexion
import com.equishare.app.ui.components.EstadoCargando
import com.equishare.app.ui.components.EstadoError
import com.equishare.app.ui.components.EstadoVacio
import com.equishare.app.ui.components.TarjetaEquipo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    alAbrirEquipo: (String) -> Unit,
    viewModel: CatalogoViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val permisoUbicacion = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
        if (permisos.values.any { it }) {
            viewModel.alCambiarCercaDeMi(true)
        } else {
            viewModel.alCambiarCercaDeMi(false)
        }
    }

    LaunchedEffect(ui.mensaje) {
        ui.mensaje?.let {
            snackbar.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EQUISHARE") },
                actions = {
                    IconButton(onClick = viewModel::recargar) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar catálogo")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { relleno ->
        Column(modifier = Modifier.fillMaxSize().padding(relleno)) {
            BannerSinConexion(visible = !ui.enLinea)
            if (ui.cargando && ui.equipos.isNotEmpty()) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = ui.filtros.texto,
                onValueChange = viewModel::alCambiarTexto,
                placeholder = { Text("Buscar equipo…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (ui.filtros.texto.isNotEmpty()) {
                        IconButton(onClick = { viewModel.alCambiarTexto("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Borrar búsqueda")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = ui.filtros.cercaDeMi,
                    onClick = {
                        when {
                            ui.filtros.cercaDeMi -> viewModel.alCambiarCercaDeMi(false)
                            viewModel.tienePermisoUbicacion() -> viewModel.alCambiarCercaDeMi(true)
                            else -> permisoUbicacion.launch(
                                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                            )
                        }
                    },
                    label = { Text("Cerca de mí") },
                    leadingIcon = { Icon(Icons.Filled.MyLocation, contentDescription = null) },
                )
                FilterChip(
                    selected = ui.filtros.categoria == null,
                    onClick = { viewModel.alElegirCategoria(null) },
                    label = { Text("Todas") },
                )
                Categoria.entries.forEach { categoria ->
                    FilterChip(
                        selected = ui.filtros.categoria == categoria,
                        onClick = { viewModel.alElegirCategoria(categoria) },
                        label = { Text(categoria.etiqueta) },
                    )
                }
            }

            when {
                ui.mostrarCargaInicial -> EstadoCargando("Cargando equipos…")
                ui.mostrarErrorPantalla -> EstadoError(
                    mensaje = ui.error?.mensaje ?: "No se pudo cargar el catálogo.",
                    alReintentar = viewModel::recargar,
                )
                ui.mostrarVacio -> EstadoVacio(
                    titulo = "No hay equipos con esos filtros",
                    detalle = "Prueba con otra categoría, otra búsqueda o desactiva \"Cerca de mí\".",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(ui.equipos, key = { it.id }) { equipo ->
                        TarjetaEquipo(
                            equipo = equipo,
                            esFavorito = equipo.id in ui.favoritos,
                            alPulsar = { alAbrirEquipo(equipo.id) },
                            alCambiarFavorito = { viewModel.alternarFavorito(equipo) },
                        )
                    }
                }
            }
        }
    }
}
