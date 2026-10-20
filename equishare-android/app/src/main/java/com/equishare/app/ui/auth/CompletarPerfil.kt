package com.equishare.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.repositorio.AuthRepository
import com.equishare.app.domain.repositorio.EmpresaRepository
import com.equishare.app.domain.repositorio.FavoritosRepository
import com.equishare.app.domain.validacion.Validadores
import com.equishare.app.ui.AppViewModelProvider
import com.equishare.app.ui.components.textoAyuda
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompletarPerfilUiState(
    val ruc: String = "",
    val razonSocial: String = "",
    val telefono: String = "",
    val errorRuc: String? = null,
    val errorRazon: String? = null,
    val errorTelefono: String? = null,
    val guardando: Boolean = false,
    val errorGeneral: String? = null,
)

/** La empresa registra y verifica su RUC antes de publicar o alquilar. */
class CompletarPerfilViewModel(
    private val empresas: EmpresaRepository,
    private val favoritos: FavoritosRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(CompletarPerfilUiState())
    val ui: StateFlow<CompletarPerfilUiState> = _ui.asStateFlow()

    fun alCambiarRuc(v: String) = _ui.update { it.copy(ruc = v.filter(Char::isDigit).take(11), errorRuc = null) }
    fun alCambiarRazon(v: String) = _ui.update { it.copy(razonSocial = v, errorRazon = null) }
    fun alCambiarTelefono(v: String) = _ui.update { it.copy(telefono = v.filter(Char::isDigit).take(9), errorTelefono = null) }

    fun guardar(alTerminar: () -> Unit) {
        val e = _ui.value
        val errores = e.copy(
            errorRuc = Validadores.ruc(e.ruc),
            errorRazon = Validadores.razonSocial(e.razonSocial),
            errorTelefono = Validadores.telefono(e.telefono),
        )
        _ui.value = errores
        if (errores.errorRuc != null || errores.errorRazon != null || errores.errorTelefono != null || e.guardando) return
        _ui.update { it.copy(guardando = true, errorGeneral = null) }
        viewModelScope.launch {
            when (val r = empresas.guardar(e.ruc, e.razonSocial, e.telefono)) {
                is Resultado.Exito -> {
                    favoritos.registrarConsentimiento(auth.sesion.value?.email)
                    _ui.value = CompletarPerfilUiState()
                    alTerminar()
                }
                is Resultado.Error -> _ui.update {
                    it.copy(guardando = false, errorGeneral = (listOf(r.error.mensaje) + r.error.detalles).joinToString("\n"))
                }
            }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch { auth.cerrarSesion() }
    }
}

@Composable
fun CompletarPerfilScreen(
    alTerminar: () -> Unit,
    viewModel: CompletarPerfilViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text("Registra tu empresa", style = MaterialTheme.typography.headlineSmall)
        Card {
            Text(
                "Para publicar o alquilar equipos debes registrar el RUC de tu empresa. " +
                    "Antes de continuar verificamos que el número de RUC sea válido.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
            )
        }
        OutlinedTextField(
            value = ui.ruc,
            onValueChange = viewModel::alCambiarRuc,
            label = { Text("RUC (11 dígitos)") },
            leadingIcon = { Icon(Icons.Filled.Verified, contentDescription = null) },
            isError = ui.errorRuc != null,
            supportingText = textoAyuda(ui.errorRuc),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = ui.razonSocial,
            onValueChange = viewModel::alCambiarRazon,
            label = { Text("Razón social") },
            leadingIcon = { Icon(Icons.Filled.Business, contentDescription = null) },
            isError = ui.errorRazon != null,
            supportingText = textoAyuda(ui.errorRazon),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = ui.telefono,
            onValueChange = viewModel::alCambiarTelefono,
            label = { Text("Celular de contacto") },
            leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
            isError = ui.errorTelefono != null,
            supportingText = textoAyuda(ui.errorTelefono),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        ui.errorGeneral?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = { viewModel.guardar(alTerminar) },
            enabled = !ui.guardando,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (ui.guardando) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Verificar y continuar")
            }
        }
        TextButton(onClick = viewModel::cerrarSesion, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
    }
}
