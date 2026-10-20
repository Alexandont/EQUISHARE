package com.equishare.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta de marca (coherente con el prototipo de baja fidelidad). Contrastes de texto ≥ 4.5:1.
val AzulMarca = Color(0xFF1E3A5F)
val AzulClaro = Color(0xFFD5E3F7)
val VerdeConfirmar = Color(0xFF2E7D32)
val NaranjaAcento = Color(0xFFB85C00)
val RojoError = Color(0xFFB3261E)

private val EsquemaClaro = lightColorScheme(
    primary = AzulMarca,
    onPrimary = Color.White,
    primaryContainer = AzulClaro,
    onPrimaryContainer = Color(0xFF0B1F36),
    secondary = VerdeConfirmar,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF0B3D0F),
    tertiary = NaranjaAcento,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0C2),
    onTertiaryContainer = Color(0xFF3A1D00),
    error = RojoError,
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFE6EBF2),
    onSurfaceVariant = Color(0xFF424A55),
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFFA9C7F0),
    onPrimary = Color(0xFF0B1F36),
    primaryContainer = Color(0xFF2B4A70),
    onPrimaryContainer = AzulClaro,
    secondary = Color(0xFF8FD694),
    onSecondary = Color(0xFF0B3D0F),
    secondaryContainer = Color(0xFF1F5A23),
    onSecondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFFFFB877),
    onTertiary = Color(0xFF3A1D00),
    tertiaryContainer = Color(0xFF6B3A00),
    onTertiaryContainer = Color(0xFFFFE0C2),
)

@Composable
fun EquishareTheme(oscuro: Boolean = isSystemInDarkTheme(), contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
        typography = Typography(),
        content = contenido,
    )
}
