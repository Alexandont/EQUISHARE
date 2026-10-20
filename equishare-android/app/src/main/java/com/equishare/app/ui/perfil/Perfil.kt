package com.equishare.app.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.equishare.app.BuildConfig
import com.equishare.app.domain.Formato
import com.equishare.app.domain.model.Empresa
import com.equishare.app.domain.model.ErrorApp
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.repositorio.AuthRepository
import com.equishare.app.domain.repositorio.EmpresaRepository
import com.equishare.app.ui.AppViewModelProvider
import com.equishare.app.ui.components.Estrellas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PerfilUiState(
    val empresa: Empresa? = null,
    val email: String? = null,
    val esDemo: Boolean = false,
    val cargando: Boolean = true,
    val error: ErrorApp? = null,
)

class PerfilViewModel(
    private val empresas: EmpresaRepository,
    private val auth: AuthRepository,
    private val limpiarDatosLocales: suspend () -> Unit,
) : ViewModel() {
    private val _ui = MutableStateFlow(PerfilUiState(email = auth.sesion.value?.email, esDemo = auth.esModoDemo))
    val ui: StateFlow<PerfilUiState> = _ui.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val r = empresas.miEmpresa()) {
                is Resultado.Exito -> _ui.update { it.copy(cargando = false, empresa = r.datos) }
                is Resultado.Error -> _ui.update { it.copy(cargando = false, error = r.error) }
            }
        }
    }

    /** Cierra sesión y borra la caché privada del teléfono (reservas y equipos propios). */
    fun cerrarSesion() {
        viewModelScope.launch {
            limpiarDatosLocales()
            auth.cerrarSesion()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    alVerPrivacidad: () -> Unit,
    viewModel: PerfilViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var mostrarServidor by remember { mutableStateOf(false) }
    var confirmarSalida by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Perfil de la empresa") }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { relleno ->
        Column(
            Modifier.fillMaxSize().padding(relleno).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(ui.empresa?.razonSocial ?: if (ui.cargando) "Cargando…" else "Empresa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    ui.empresa?.let { e ->
                        Text("RUC: ${e.ruc}" + if (e.verificada) "  ✓ verificado" else "", style = MaterialTheme.typography.bodyMedium)
                        Text("Celular: ${e.telefono}", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Como arrendatario: ", style = MaterialTheme.typography.bodySmall)
                            Estrellas(e.calificacionComoArrendatario, 14.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(Formato.calificacion(e.calificacionComoArrendatario, e.totalResenasComoArrendatario), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("Correo: ${ui.email ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    ui.error?.let {
                        Text(it.mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = viewModel::cargar) { Text("Reintentar") }
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Información técnica", style = MaterialTheme.typography.titleSmall)
                    Text("Autenticación: " + if (ui.esDemo) "modo demostración (sin Firebase)" else "Firebase Authentication", style = MaterialTheme.typography.bodySmall)
                    Text("Google Maps: " + if (BuildConfig.MAPS_ENABLED) "configurado" else "sin API key", style = MaterialTheme.typography.bodySmall)
                    Text("Versión: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = { mostrarServidor = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Dns, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Servidor de la API")
            }
            OutlinedButton(onClick = alVerPrivacidad, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PrivacyTip, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Privacidad y uso ético")
            }
            OutlinedButton(onClick = { confirmarSalida = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }

    if (mostrarServidor) DialogoServidor(alCerrar = { mostrarServidor = false }, alGuardar = { viewModel.cargar() })
    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Se borrarán de este teléfono tus reservas y equipos guardados para proteger tu información.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    viewModel.cerrarSesion()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacidadScreen(alVolver: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacidad y uso ético") },
                navigationIcon = { IconButton(onClick = alVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
            )
        },
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno).verticalScroll(rememberScrollState()).padding(16.dp)) {
            TextoPoliticaPrivacidad()
        }
    }
}
