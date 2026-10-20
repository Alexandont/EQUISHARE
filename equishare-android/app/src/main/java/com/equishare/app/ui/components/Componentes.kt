package com.equishare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.equishare.app.domain.Formato
import com.equishare.app.domain.model.Categoria
import com.equishare.app.domain.model.Equipo
import com.equishare.app.domain.model.EstadoEquipo
import com.equishare.app.domain.model.EstadoReserva

fun iconoCategoria(categoria: Categoria): ImageVector = when (categoria) {
    Categoria.CONSTRUCCION -> Icons.Filled.Construction
    Categoria.EVENTOS -> Icons.Filled.Celebration
    Categoria.HERRAMIENTAS -> Icons.Filled.Handyman
    Categoria.TECNOLOGIA -> Icons.Filled.Devices
}

@Composable
fun EstadoCargando(texto: String = "Cargando…", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun EstadoError(
    mensaje: String,
    modifier: Modifier = Modifier,
    textoBoton: String = "Reintentar",
    alReintentar: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(mensaje, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        if (alReintentar != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = alReintentar) { Text(textoBoton) }
        }
    }
}

@Composable
fun EstadoVacio(titulo: String, detalle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(detalle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Aviso persistente cuando no hay conexión: la app sigue mostrando los datos guardados en Room. */
@Composable
fun BannerSinConexion(visible: Boolean) {
    if (!visible) return
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(8.dp))
            Text(
                "Sin conexión. Mostrando datos guardados en el teléfono.",
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun ImagenEquipo(url: String?, categoria: Categoria, descripcion: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(iconoCategoria(categoria), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = descripcion,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun Estrellas(valor: Double, tamano: Dp = 16.dp) {
    val etiqueta = "Calificación ${"%.1f".format(valor)} de 5"
    Row(modifier = Modifier.semantics { contentDescription = etiqueta }) {
        for (i in 1..5) {
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = if (valor >= i - 0.25) Color(0xFFF2A100) else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(tamano),
            )
        }
    }
}

/** Selector de estrellas (1 a 5). Cada estrella es un objetivo táctil de 48 dp. */
@Composable
fun SelectorEstrellas(valor: Int, alCambiar: (Int) -> Unit) {
    Row {
        for (i in 1..5) {
            IconButton(onClick = { alCambiar(i) }) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "$i estrellas",
                    tint = if (i <= valor) Color(0xFFF2A100) else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
fun Etiqueta(texto: String, fondo: Color, colorTexto: Color) {
    Surface(color = fondo, shape = RoundedCornerShape(50)) {
        Text(
            texto,
            color = colorTexto,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun EtiquetaEstadoEquipo(estado: EstadoEquipo) {
    val esquema = MaterialTheme.colorScheme
    when (estado) {
        EstadoEquipo.PUBLICADO -> Etiqueta(estado.etiqueta, esquema.secondaryContainer, esquema.onSecondaryContainer)
        EstadoEquipo.OCULTO -> Etiqueta(estado.etiqueta, esquema.surfaceVariant, esquema.onSurfaceVariant)
        EstadoEquipo.PENDIENTE_FOTO -> Etiqueta(estado.etiqueta, esquema.tertiaryContainer, esquema.onTertiaryContainer)
        EstadoEquipo.EN_REVISION -> Etiqueta(estado.etiqueta, esquema.errorContainer, esquema.onErrorContainer)
    }
}

@Composable
fun EtiquetaEstadoReserva(estado: EstadoReserva) {
    val esquema = MaterialTheme.colorScheme
    when (estado) {
        EstadoReserva.PENDIENTE -> Etiqueta(estado.etiqueta, esquema.tertiaryContainer, esquema.onTertiaryContainer)
        EstadoReserva.CONFIRMADA, EstadoReserva.EN_CURSO -> Etiqueta(estado.etiqueta, esquema.primaryContainer, esquema.onPrimaryContainer)
        EstadoReserva.DEVUELTA -> Etiqueta(estado.etiqueta, esquema.secondaryContainer, esquema.onSecondaryContainer)
        EstadoReserva.CANCELADA -> Etiqueta(estado.etiqueta, esquema.surfaceVariant, esquema.onSurfaceVariant)
    }
}

@Composable
fun TarjetaEquipo(
    equipo: Equipo,
    esFavorito: Boolean,
    alPulsar: () -> Unit,
    alCambiarFavorito: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = alPulsar)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ImagenEquipo(
                url = equipo.fotoUrl,
                categoria = equipo.categoria,
                descripcion = "Foto de ${equipo.nombre}",
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(12.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(equipo.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${Formato.soles(equipo.precioDia)} / día", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                Text(equipo.empresaNombre, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Estrellas(equipo.calificacionPromedio, 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        listOfNotNull(
                            if (equipo.totalResenas > 0) "(${equipo.totalResenas})" else "Nuevo",
                            Formato.distancia(equipo.distanciaKm),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            IconButton(onClick = alCambiarFavorito) {
                Icon(
                    if (esFavorito) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = if (esFavorito) "Quitar de favoritos" else "Agregar a favoritos",
                    tint = if (esFavorito) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Texto de ayuda/error para el parámetro supportingText de los campos de formulario. */
fun textoAyuda(mensaje: String?): (@Composable () -> Unit)? {
    if (mensaje == null) return null
    return { Text(mensaje) }
}
