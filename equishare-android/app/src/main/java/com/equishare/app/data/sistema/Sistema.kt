package com.equishare.app.data.sistema

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.equishare.app.domain.model.Coordenadas
import com.equishare.app.domain.repositorio.MonitorRed
import com.equishare.app.domain.repositorio.ProveedorUbicacion
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Observa la conectividad con ConnectivityManager para mostrar el aviso "Sin conexión". */
class MonitorRedAndroid(context: Context) : MonitorRed {
    private val conectividad = context.getSystemService(ConnectivityManager::class.java)

    override val enLinea: Flow<Boolean> = callbackFlow {
        fun estadoActual(): Boolean {
            val capacidades = conectividad.getNetworkCapabilities(conectividad.activeNetwork)
            return capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(estadoActual())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }
        }
        trySend(estadoActual())
        conectividad.registerDefaultNetworkCallback(callback)
        awaitClose { conectividad.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()
}

/**
 * Ubicación actual con Fused Location Provider. Se usa prioridad "balanceada" (precisión de cuadra),
 * suficiente para ordenar equipos por cercanía y con menor consumo de batería.
 * La ubicación no se envía al servidor salvo como parámetro de búsqueda y no se almacena.
 */
class UbicacionFused(private val context: Context) : ProveedorUbicacion {
    private val cliente = LocationServices.getFusedLocationProviderClient(context)

    override fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override suspend fun ubicacionActual(): Coordenadas? {
        if (!tienePermiso()) return null
        return runCatching {
            val token = CancellationTokenSource()
            val ubicacion = withTimeoutOrNull(10_000) {
                cliente.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token).await()
            } ?: cliente.lastLocation.await()
            ubicacion?.let { Coordenadas(it.latitude, it.longitude) }
        }.getOrNull()
    }
}
