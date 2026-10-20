package com.equishare.app.domain.repositorio

import com.equishare.app.domain.model.Categoria
import com.equishare.app.domain.model.Coordenadas
import com.equishare.app.domain.model.Cotizacion
import com.equishare.app.domain.model.Empresa
import com.equishare.app.domain.model.Equipo
import com.equishare.app.domain.model.EquipoEntrada
import com.equishare.app.domain.model.EstadoReserva
import com.equishare.app.domain.model.FotoSubida
import com.equishare.app.domain.model.RangoOcupado
import com.equishare.app.domain.model.Resena
import com.equishare.app.domain.model.Reserva
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.Rol
import com.equishare.app.domain.model.Sesion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/*
 * Contratos del patrón Repository. Los ViewModel dependen de estas interfaces y no de Retrofit,
 * Room, DataStore o Firebase; así cada capa se puede probar por separado con implementaciones falsas.
 */

data class FiltroCatalogo(
    val texto: String = "",
    val categoria: Categoria? = null,
    val ubicacion: Coordenadas? = null,
    val radioKm: Double? = null,
)

interface EquipoRepository {
    /** Catálogo guardado en Room (fuente única de verdad para la UI, funciona sin conexión). */
    fun observarCatalogo(): Flow<List<Equipo>>
    suspend fun refrescarCatalogo(filtro: FiltroCatalogo): Resultado<Unit>
    fun observarPropios(): Flow<List<Equipo>>
    suspend fun refrescarPropios(): Resultado<Unit>
    suspend fun obtener(id: String): Resultado<Equipo>
    suspend fun crear(entrada: EquipoEntrada): Resultado<Equipo>
    suspend fun actualizar(id: String, entrada: EquipoEntrada): Resultado<Equipo>
    suspend fun eliminar(id: String): Resultado<Unit>
    suspend fun subirFoto(uri: String): Resultado<FotoSubida>
    suspend fun disponibilidad(id: String): Resultado<List<RangoOcupado>>
    suspend fun resenas(id: String): Resultado<List<Resena>>
}

interface ReservaRepository {
    fun observarMias(rol: Rol): Flow<List<Reserva>>
    suspend fun refrescarMias(rol: Rol): Resultado<Unit>
    suspend fun cotizar(equipoId: String, inicio: String, fin: String): Resultado<Cotizacion>
    suspend fun crear(equipoId: String, inicio: String, fin: String): Resultado<Reserva>
    suspend fun cancelar(id: String): Resultado<Reserva>
    suspend fun cambiarEstado(id: String, estado: EstadoReserva): Resultado<Reserva>
    suspend fun calificar(id: String, estrellas: Int, comentario: String): Resultado<Unit>
}

interface EmpresaRepository {
    /** Devuelve null si la empresa aún no completó su registro (HTTP 404). */
    suspend fun miEmpresa(): Resultado<Empresa?>
    suspend fun guardar(ruc: String, razonSocial: String, telefono: String): Resultado<Empresa>
}

interface AuthRepository {
    val sesion: StateFlow<Sesion?>
    /** true cuando ya se sabe si hay una sesión guardada (evita mostrar el login por un instante). */
    val sesionCargada: StateFlow<Boolean>
    val esModoDemo: Boolean
    suspend fun iniciarSesion(email: String, password: String): Resultado<Sesion>
    suspend fun registrar(email: String, password: String): Resultado<Sesion>
    suspend fun cerrarSesion()
    /** Token para la cabecera Authorization. Se llama desde hilos de red (no desde el hilo principal). */
    fun tokenParaApi(): String?
}

interface FavoritosRepository {
    fun observar(): Flow<Set<String>>
    suspend fun establecer(equipo: Equipo, favorito: Boolean): Resultado<Unit>
    /** Registra el consentimiento de privacidad del usuario (Ley N.° 29733). */
    suspend fun registrarConsentimiento(email: String?): Resultado<Unit>
}

interface AjustesRepository {
    val urlServidor: Flow<String>
    val categoriaFiltro: Flow<Categoria?>
    suspend fun guardarUrlServidor(url: String)
    suspend fun guardarCategoriaFiltro(categoria: Categoria?)
    suspend fun probarServidor(url: String): Resultado<String>
}

interface MonitorRed {
    val enLinea: Flow<Boolean>
}

interface ProveedorUbicacion {
    fun tienePermiso(): Boolean
    suspend fun ubicacionActual(): Coordenadas?
}
