package com.equishare.app.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.equishare.app.ui.AppViewModelProvider

@Composable
fun DialogoServidor(
    alCerrar: () -> Unit,
    alGuardar: () -> Unit = {},
    viewModel: ServidorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Servidor de EQUISHARE") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Emulador: http://10.0.2.2:3000/  ·  Celular físico: la IP de la PC que ejecuta " +
                        "\"npm start\" (por ejemplo http://192.168.1.50:3000/), ambos en la misma red Wi-Fi.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = ui.url,
                    onValueChange = viewModel::alCambiarUrl,
                    label = { Text("Dirección del servidor") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (ui.probando) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                ui.mensaje?.let {
                    Text(
                        it,
                        color = if (ui.exito) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.probarYGuardar { alGuardar() } }, enabled = !ui.probando) {
                Text("Probar y guardar")
            }
        },
        dismissButton = { TextButton(onClick = alCerrar) { Text("Cerrar") } },
    )
}

/** Resumen de la política de privacidad (Ley N.° 29733 y su reglamento, D.S. N.° 016-2024-JUS). */
@Composable
fun TextoPoliticaPrivacidad() {
    val secciones = listOf(
        "Qué datos usamos" to "Correo de acceso, RUC, razón social y teléfono de la empresa; los equipos que publicas " +
            "(fotos, precio y ubicación aproximada del equipo) y el historial de reservas y calificaciones.",
        "Para qué" to "Verificar que eres una empresa (RUC), mostrar tus equipos en el catálogo, gestionar reservas y " +
            "generar confianza mediante calificaciones. No vendemos tus datos ni enviamos mensajes a tus contactos.",
        "Ubicación" to "Solo se usa, con tu permiso, para ordenar equipos por cercanía y ubicar el equipo que publicas. " +
            "Puedes usar la app sin conceder este permiso.",
        "Dónde se guardan" to "En el servidor de EQUISHARE y, para la sesión y los favoritos, en Firebase (Google). " +
            "En tu teléfono se guarda una copia del catálogo para usarlo sin conexión; se borra al cerrar sesión.",
        "Tus derechos" to "Puedes solicitar acceso, rectificación, cancelación u oposición sobre tus datos personales " +
            "(derechos ARCO) escribiendo al equipo del proyecto.",
        "Sin trampas" to "No hay casillas premarcadas, cargos ocultos ni mensajes de urgencia falsos. La comisión del 12 % " +
            "y las penalidades se muestran antes de confirmar cualquier reserva.",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        secciones.forEach { (titulo, texto) ->
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            Text(texto, style = MaterialTheme.typography.bodySmall)
        }
    }
}
