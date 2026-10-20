package com.equishare.app.ui.misequipos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equishare.app.domain.Formato
import com.equishare.app.domain.model.Equipo
import com.equishare.app.domain.model.ErrorApp
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.repositorio.EquipoRepository
import com.equishare.app.domain.repositorio.MonitorRed
import com.equishare.app.ui.AppViewModelProvider
import com.equishare.app.ui.components.BannerSinConexion
import com.equishare.app.ui.components.EstadoCargando
import com.equishare.app.ui.components.EstadoError
import com.equishare.app.ui.components.EstadoVacio
import com.equishare.app.ui.components.EtiquetaEstadoEquipo
import com.equishare.app.ui.components.ImagenEquipo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MisEquiposUiState(
    val equipos: List<Equipo> = emptyList(),
    val cargando: Boolean = false,
    val error: ErrorApp? = null,
    val enLinea: Boolean = true,
    val mensaje: String? = null,
)

private data class Interno(val cargando: Boolean = true, val error: ErrorApp? = null, val mensaje: String? = null)

class MisEquiposViewModel(
    private val equipos: EquipoRepository,
    red: MonitorRed,
) : ViewModel() {
    private val interno = MutableStateFlow(Interno())

    val ui: StateFlow<MisEquiposUiState> = combine(equipos.observarPropios(), red.enLinea, interno) { lista, enLinea, i ->
        MisEquiposUiState(lista, i.cargando, i.error, enLinea, i.mensaje)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MisEquiposUiState(cargando = true))

    init {
        recargar()
    }

    fun recargar() {
        interno.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = equipos.refrescarPropios()) {
                is Resultado.Exito -> interno.update { it.copy(cargando = false) }
                is Resultado.Error -> interno.update { it.copy(cargando = false, error = r.error) }
            }
        }
    }

    /** Eliminar toma los mismos 2 pasos que publicar (botón + confirmación): cancelación simétrica. */
    fun eliminar(equipo: Equipo) {
        viewModelScope.launch {
            when (val r = equipos.eliminar(equipo.id)) {
                is Resultado.Exito -> interno.update { it.copy(mensaje = "Se eliminó \"${equipo.nombre}\".") }
                is Resultado.Error -> interno.update { it.copy(mensaje = r.error.mensaje) }
            }
        }
    }

    fun mensajeMostrado() = interno.update { it.copy(mensaje = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisEquiposScreen(
    alPublicar: () -> Unit,
    alEditar: (String) -> Unit,
    alAbrir: (String) -> Unit,
    viewModel: MisEquiposViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var porEliminar by remember { mutableStateOf<Equipo?>(null) }

    LaunchedEffect(ui.mensaje) {
        val texto = ui.mensaje ?: return@LaunchedEffect
        snackbar.showSnackbar(texto)
        viewModel.mensajeMostrado()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis equipos") },
                actions = {
                    IconButton(onClick = viewModel::recargar) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = alPublicar,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Publicar equipo") },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            BannerSinConexion(!ui.enLinea)
            if (ui.cargando && ui.equipos.isNotEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                ui.cargando && ui.equipos.isEmpty() -> EstadoCargando()
                ui.error != null && ui.equipos.isEmpty() -> EstadoError(ui.error?.mensaje ?: "Error", alReintentar = viewModel::recargar)
                ui.equipos.isEmpty() -> EstadoVacio(
                    "Aún no publicas equipos",
                    "Comparte la maquinaria o herramientas que tu empresa no usa todos los días y genera ingresos.",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(ui.equipos, key = { it.id }) { equipo ->
                        Card(Modifier.fillMaxWidth().clickable { alAbrir(equipo.id) }) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                ImagenEquipo(
                                    equipo.fotoUrl,
                                    equipo.categoria,
                                    "Foto de ${equipo.nombre}",
                                    Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(equipo.nombre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${Formato.soles(equipo.precioDia)} / día", style = MaterialTheme.typography.bodyMedium)
                                    EtiquetaEstadoEquipo(equipo.estado)
                                }
                                IconButton(onClick = { alEditar(equipo.id) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Editar ${equipo.nombre}")
                                }
                                IconButton(onClick = { porEliminar = equipo }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${equipo.nombre}", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    porEliminar?.let { equipo ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            title = { Text("¿Eliminar equipo?") },
            text = { Text("\"${equipo.nombre}\" dejará de aparecer en el catálogo. No se puede eliminar si tiene reservas activas.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(equipo)
                    porEliminar = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { porEliminar = null }) { Text("Cancelar") } },
        )
    }
}
