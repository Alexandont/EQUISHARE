package com.equishare.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equishare.app.ui.AppViewModelProvider
import com.equishare.app.ui.components.textoAyuda
import com.equishare.app.ui.perfil.DialogoServidor
import com.equishare.app.ui.perfil.TextoPoliticaPrivacidad

@Composable
fun LoginScreen(
    alRegistrarse: (String) -> Unit,
    viewModel: LoginViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var verPassword by rememberSaveable { mutableStateOf(false) }
    var mostrarServidor by remember { mutableStateOf(false) }
    var mostrarPolitica by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(Icons.Filled.Handshake, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Text("EQUISHARE", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(
            "Alquila y comparte equipos entre empresas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (ui.esModoDemo) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Text(
                    "Modo demostración. Ingresa con constructora@demo.pe, eventos@demo.pe o " +
                        "taller@demo.pe y cualquier contraseña de 8 caracteres con letras y números.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        Text(
            if (ui.modoRegistro) "Crear cuenta" else "Iniciar sesión",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = ui.email,
            onValueChange = viewModel::alCambiarEmail,
            label = { Text("Correo de la empresa") },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            isError = ui.errorEmail != null,
            supportingText = textoAyuda(ui.errorEmail),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = ui.password,
            onValueChange = viewModel::alCambiarPassword,
            label = { Text("Contraseña") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { verPassword = !verPassword }) {
                    Icon(
                        if (verPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (verPassword) "Ocultar contraseña" else "Mostrar contraseña",
                    )
                }
            },
            visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
            isError = ui.errorPassword != null,
            supportingText = textoAyuda(ui.errorPassword),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )

        if (ui.modoRegistro) {
            OutlinedTextField(
                value = ui.confirmacion,
                onValueChange = viewModel::alCambiarConfirmacion,
                label = { Text("Repite la contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                isError = ui.errorConfirmacion != null,
                supportingText = textoAyuda(ui.errorConfirmacion),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = ui.aceptaPrivacidad, onCheckedChange = viewModel::alCambiarPrivacidad)
                Text("He leído y acepto la ", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { mostrarPolitica = true }) { Text("política de privacidad") }
            }
            ui.errorPrivacidad?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
            }
        }

        ui.errorGeneral?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
        }

        Button(
            onClick = { viewModel.enviar(alRegistrarse) },
            enabled = !ui.cargando,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (ui.cargando) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (ui.modoRegistro) "Crear cuenta" else "Ingresar")
            }
        }
        TextButton(onClick = viewModel::alternarModo) {
            Text(if (ui.modoRegistro) "¿Ya tienes cuenta? Inicia sesión" else "¿Tu empresa es nueva? Crea una cuenta")
        }
        TextButton(onClick = { mostrarServidor = true }) { Text("Ajustes de conexión") }
    }

    if (mostrarServidor) DialogoServidor(alCerrar = { mostrarServidor = false })
    if (mostrarPolitica) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mostrarPolitica = false },
            confirmButton = { TextButton(onClick = { mostrarPolitica = false }) { Text("Cerrar") } },
            title = { Text("Política de privacidad") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) { TextoPoliticaPrivacidad() }
            },
        )
    }
}
