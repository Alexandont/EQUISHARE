package com.equishare.app.ui.navegacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.equishare.app.ui.AppViewModelProvider
import com.equishare.app.ui.auth.CompletarPerfilScreen
import com.equishare.app.ui.auth.LoginScreen
import com.equishare.app.ui.catalogo.CatalogoScreen
import com.equishare.app.ui.components.EstadoCargando
import com.equishare.app.ui.detalle.DetalleScreen
import com.equishare.app.ui.mapa.MapaScreen
import com.equishare.app.ui.misequipos.MisEquiposScreen
import com.equishare.app.ui.perfil.DialogoServidor
import com.equishare.app.ui.perfil.PerfilScreen
import com.equishare.app.ui.perfil.PrivacidadScreen
import com.equishare.app.ui.publicar.PublicarScreen
import com.equishare.app.ui.raiz.EstadoRaiz
import com.equishare.app.ui.raiz.RaizViewModel
import com.equishare.app.ui.reservas.ReservasScreen

object Rutas {
    const val CATALOGO = "catalogo"
    const val MAPA = "mapa"
    const val RESERVAS = "reservas"
    const val MIS_EQUIPOS = "mis_equipos"
    const val PERFIL = "perfil"
    const val DETALLE = "detalle/{equipoId}"
    const val PUBLICAR = "publicar?equipoId={equipoId}"
    const val PRIVACIDAD = "privacidad"

    fun detalle(id: String) = "detalle/$id"
    fun publicar(id: String? = null) = if (id == null) "publicar" else "publicar?equipoId=$id"
}

private data class Pestana(val ruta: String, val etiqueta: String, val icono: ImageVector)

private val PESTANAS = listOf(
    Pestana(Rutas.CATALOGO, "Catálogo", Icons.Filled.Search),
    Pestana(Rutas.MAPA, "Mapa", Icons.Filled.Map),
    Pestana(Rutas.RESERVAS, "Reservas", Icons.Filled.DateRange),
    Pestana(Rutas.MIS_EQUIPOS, "Mis equipos", Icons.Filled.Build),
    Pestana(Rutas.PERFIL, "Perfil", Icons.Filled.Person),
)

/** Punto de entrada de la UI: decide entre login, registro de empresa o la app principal. */
@Composable
fun RaizApp(viewModel: RaizViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    when (val e = estado) {
        EstadoRaiz.Cargando -> EstadoCargando("Preparando EQUISHARE…", Modifier.systemBarsPadding())
        EstadoRaiz.SinSesion -> LoginScreen(alRegistrarse = { })
        is EstadoRaiz.PerfilIncompleto -> CompletarPerfilScreen(alTerminar = viewModel::perfilCompletado)
        is EstadoRaiz.ErrorServidor -> PantallaSinServidor(
            mensaje = e.mensaje,
            alReintentar = viewModel::reintentar,
            alContinuar = viewModel::continuarSinConexion,
            alCerrarSesion = viewModel::cerrarSesion,
        )
        is EstadoRaiz.Listo -> PrincipalApp()
    }
}

@Composable
private fun PantallaSinServidor(
    mensaje: String,
    alReintentar: () -> Unit,
    alContinuar: () -> Unit,
    alCerrarSesion: () -> Unit,
) {
    var mostrarServidor by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No se pudo conectar con el servidor", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(mensaje, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = alReintentar) { Text("Reintentar") }
        OutlinedButton(onClick = { mostrarServidor = true }) { Text("Configurar servidor") }
        TextButton(onClick = alContinuar) { Text("Usar datos guardados (sin conexión)") }
        TextButton(onClick = alCerrarSesion) { Text("Cerrar sesión") }
    }
    if (mostrarServidor) {
        DialogoServidor(
            alCerrar = { mostrarServidor = false },
            alGuardar = {
                mostrarServidor = false
                alReintentar()
            },
        )
    }
}

@Composable
private fun PrincipalApp() {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val destino = entradaActual?.destination
    val esPestana = PESTANAS.any { p -> destino?.hierarchy?.any { it.route == p.ruta } == true }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (esPestana) {
                NavigationBar {
                    PESTANAS.forEach { pestana ->
                        NavigationBarItem(
                            selected = destino?.hierarchy?.any { it.route == pestana.ruta } == true,
                            onClick = {
                                navController.navigate(pestana.ruta) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(pestana.icono, contentDescription = null) },
                            label = { Text(pestana.etiqueta) },
                        )
                    }
                }
            }
        },
    ) { relleno ->
        NavHost(
            navController = navController,
            startDestination = Rutas.CATALOGO,
            modifier = Modifier.padding(relleno),
        ) {
            composable(Rutas.CATALOGO) {
                CatalogoScreen(alAbrirEquipo = { navController.navigate(Rutas.detalle(it)) })
            }
            composable(Rutas.MAPA) {
                MapaScreen(alAbrirEquipo = { navController.navigate(Rutas.detalle(it)) })
            }
            composable(Rutas.RESERVAS) { ReservasScreen() }
            composable(Rutas.MIS_EQUIPOS) {
                MisEquiposScreen(
                    alPublicar = { navController.navigate(Rutas.publicar()) },
                    alEditar = { navController.navigate(Rutas.publicar(it)) },
                    alAbrir = { navController.navigate(Rutas.detalle(it)) },
                )
            }
            composable(Rutas.PERFIL) {
                PerfilScreen(alVerPrivacidad = { navController.navigate(Rutas.PRIVACIDAD) })
            }
            composable(
                route = Rutas.DETALLE,
                arguments = listOf(navArgument("equipoId") { type = NavType.StringType }),
            ) {
                DetalleScreen(
                    alVolver = { navController.popBackStack() },
                    alEditar = { navController.navigate(Rutas.publicar(it)) },
                )
            }
            composable(
                route = Rutas.PUBLICAR,
                arguments = listOf(
                    navArgument("equipoId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                PublicarScreen(alVolver = { navController.popBackStack() })
            }
            composable(Rutas.PRIVACIDAD) {
                PrivacidadScreen(alVolver = { navController.popBackStack() })
            }
        }
    }
}
