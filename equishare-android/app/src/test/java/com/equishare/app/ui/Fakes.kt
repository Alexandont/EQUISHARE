package com.equishare.app.ui

import com.equishare.app.domain.model.Categoria
import com.equishare.app.domain.model.Coordenadas
import com.equishare.app.domain.model.Equipo
import com.equishare.app.domain.model.EquipoEntrada
import com.equishare.app.domain.model.ErrorApp
import com.equishare.app.domain.model.EstadoEquipo
import com.equishare.app.domain.model.FotoSubida
import com.equishare.app.domain.model.RangoOcupado
import com.equishare.app.domain.model.Resena
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.Sesion
import com.equishare.app.domain.model.TipoError
import com.equishare.app.domain.repositorio.AjustesRepository
import com.equishare.app.domain.repositorio.AuthRepository
import com.equishare.app.domain.repositorio.EquipoRepository
import com.equishare.app.domain.repositorio.FavoritosRepository
import com.equishare.app.domain.repositorio.FiltroCatalogo
import com.equishare.app.domain.repositorio.MonitorRed
import com.equishare.app.domain.repositorio.ProveedorUbicacion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun equipoDePrueba(id: String, categoria: Categoria = Categoria.CONSTRUCCION) = Equipo(
    id = id,
    empresaId = "demo-constructora",
    empresaNombre = "Constructora Demo",
    nombre = "Equipo $id",
    categoria = categoria,
    descripcion = "Descripción de prueba",
    precioDia = 80.0,
    fotoUrl = null,
    fotoId = null,
    lat = -8.11,
    lng = -79.03,
    direccion = "Trujillo",
    estado = EstadoEquipo.PUBLICADO,
    calificacionPromedio = 0.0,
    totalResenas = 0,
    distanciaKm = null,
)

/** Repositorio falso: simula Room (catálogo) y la API (resultado configurable). */
class FakeEquipoRepository : EquipoRepository {
    val catalogo = MutableStateFlow<List<Equipo>>(emptyList())
    var respuestaServidor: List<Equipo> = listOf(equipoDePrueba("1"), equipoDePrueba("2", Categoria.EVENTOS))
    var fallarCon: ErrorApp? = null
    val filtrosRecibidos = mutableListOf<FiltroCatalogo>()

    override fun observarCatalogo(): Flow<List<Equipo>> = catalogo

    override suspend fun refrescarCatalogo(filtro: FiltroCatalogo): Resultado<Unit> {
        filtrosRecibidos += filtro
        fallarCon?.let { return Resultado.Error(it) }
        catalogo.value = respuestaServidor.filter { filtro.categoria == null || it.categoria == filtro.categoria }
        return Resultado.Exito(Unit)
    }

    override fun observarPropios(): Flow<List<Equipo>> = MutableStateFlow(emptyList())
    override suspend fun refrescarPropios(): Resultado<Unit> = Resultado.Exito(Unit)
    override suspend fun obtener(id: String): Resultado<Equipo> = Resultado.Exito(equipoDePrueba(id))
    override suspend fun crear(entrada: EquipoEntrada): Resultado<Equipo> = Resultado.Exito(equipoDePrueba("nuevo"))
    override suspend fun actualizar(id: String, entrada: EquipoEntrada): Resultado<Equipo> = Resultado.Exito(equipoDePrueba(id))
    override suspend fun eliminar(id: String): Resultado<Unit> = Resultado.Exito(Unit)
    override suspend fun subirFoto(uri: String): Resultado<FotoSubida> = Resultado.Error(ErrorApp(TipoError.VALIDACION))
    override suspend fun disponibilidad(id: String): Resultado<List<RangoOcupado>> = Resultado.Exito(emptyList())
    override suspend fun resenas(id: String): Resultado<List<Resena>> = Resultado.Exito(emptyList())
}

class FakeFavoritos : FavoritosRepository {
    private val ids = MutableStateFlow<Set<String>>(emptySet())
    override fun observar(): Flow<Set<String>> = ids
    override suspend fun establecer(equipo: Equipo, favorito: Boolean): Resultado<Unit> {
        ids.value = if (favorito) ids.value + equipo.id else ids.value - equipo.id
        return Resultado.Exito(Unit)
    }
    override suspend fun registrarConsentimiento(email: String?): Resultado<Unit> = Resultado.Exito(Unit)
}

class FakeRed(inicial: Boolean = true) : MonitorRed {
    val estado = MutableStateFlow(inicial)
    override val enLinea: Flow<Boolean> = estado
}

class FakeUbicacion(var coordenadas: Coordenadas? = Coordenadas(-8.11, -79.03)) : ProveedorUbicacion {
    override fun tienePermiso(): Boolean = true
    override suspend fun ubicacionActual(): Coordenadas? = coordenadas
}

class FakeAjustes : AjustesRepository {
    private val url = MutableStateFlow("http://10.0.2.2:3000/")
    private val categoria = MutableStateFlow<Categoria?>(null)
    override val urlServidor: Flow<String> = url
    override val categoriaFiltro: Flow<Categoria?> = categoria
    override suspend fun guardarUrlServidor(url: String) { this.url.value = url }
    override suspend fun guardarCategoriaFiltro(categoria: Categoria?) { this.categoria.value = categoria }
    override suspend fun probarServidor(url: String): Resultado<String> = Resultado.Exito("demo")
}

class FakeAuth(private val fallar: Boolean = false) : AuthRepository {
    private val _sesion = MutableStateFlow<Sesion?>(null)
    override val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()
    override val sesionCargada: StateFlow<Boolean> = MutableStateFlow(true)
    override val esModoDemo: Boolean = true
    var llamadas = 0

    override suspend fun iniciarSesion(email: String, password: String): Resultado<Sesion> {
        llamadas++
        if (fallar) return Resultado.Error(ErrorApp(TipoError.NO_AUTENTICADO, "Correo o contraseña incorrectos."))
        val s = Sesion("demo-x", email, true)
        _sesion.value = s
        return Resultado.Exito(s)
    }

    override suspend fun registrar(email: String, password: String): Resultado<Sesion> = iniciarSesion(email, password)
    override suspend fun cerrarSesion() { _sesion.value = null }
    override fun tokenParaApi(): String? = _sesion.value?.let { "demo:${it.uid}" }
}
