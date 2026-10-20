package com.equishare.app

import android.content.Context
import com.equishare.app.data.auth.DemoAuthRepository
import com.equishare.app.data.auth.FirebaseAuthRepository
import com.equishare.app.data.firebase.FirestoreFavoritosRepository
import com.equishare.app.data.firebase.MemoriaFavoritosRepository
import com.equishare.app.data.local.AjustesRepositoryImpl
import com.equishare.app.data.local.EquishareDatabase
import com.equishare.app.data.remote.ApiProvider
import com.equishare.app.data.remote.llamarApi
import com.equishare.app.data.repositorio.EmpresaRepositoryImpl
import com.equishare.app.data.repositorio.EquipoRepositoryImpl
import com.equishare.app.data.repositorio.ReservaRepositoryImpl
import com.equishare.app.data.sistema.LectorImagen
import com.equishare.app.data.sistema.MonitorRedAndroid
import com.equishare.app.data.sistema.UbicacionFused
import com.equishare.app.domain.model.mapear
import com.equishare.app.domain.repositorio.AjustesRepository
import com.equishare.app.domain.repositorio.AuthRepository
import com.equishare.app.domain.repositorio.EmpresaRepository
import com.equishare.app.domain.repositorio.EquipoRepository
import com.equishare.app.domain.repositorio.FavoritosRepository
import com.equishare.app.domain.repositorio.MonitorRed
import com.equishare.app.domain.repositorio.ProveedorUbicacion
import com.equishare.app.domain.repositorio.ReservaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Inyección de dependencias manual: un único lugar donde se crean los repositorios.
 * Si existe google-services.json se usa Firebase; si no, el modo demostración.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val alcanceApp = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val firebaseActivo: Boolean = BuildConfig.FIREBASE_ENABLED
    val mapasActivos: Boolean = BuildConfig.MAPS_ENABLED

    private val baseDatos: EquishareDatabase by lazy { EquishareDatabase.crear(appContext) }

    private val ajustesImpl: AjustesRepositoryImpl by lazy {
        AjustesRepositoryImpl(appContext) { url ->
            llamarApi { apiProvider.paraUrl(url).salud() }.mapear { it.modoAutenticacion }
        }
    }
    val ajustes: AjustesRepository get() = ajustesImpl

    val auth: AuthRepository by lazy {
        if (firebaseActivo) FirebaseAuthRepository() else DemoAuthRepository(ajustesImpl, alcanceApp)
    }

    val apiProvider: ApiProvider by lazy { ApiProvider(ajustesImpl.urlServidor, auth) }

    val equipos: EquipoRepository by lazy {
        EquipoRepositoryImpl(apiProvider, baseDatos.equipoDao(), LectorImagen(appContext))
    }
    val reservas: ReservaRepository by lazy { ReservaRepositoryImpl(apiProvider, baseDatos.reservaDao()) }
    val empresas: EmpresaRepository by lazy { EmpresaRepositoryImpl(apiProvider) }
    val favoritos: FavoritosRepository by lazy {
        if (firebaseActivo) FirestoreFavoritosRepository(auth) else MemoriaFavoritosRepository()
    }
    val red: MonitorRed by lazy { MonitorRedAndroid(appContext) }
    val ubicacion: ProveedorUbicacion by lazy { UbicacionFused(appContext) }

    /** Borra la caché local de datos privados al cerrar sesión (privacidad en equipos compartidos). */
    suspend fun limpiarDatosDeSesion() {
        baseDatos.reservaDao().borrarTodo()
        baseDatos.equipoDao().borrarLista(EquipoRepositoryImpl.LISTA_PROPIOS)
    }
}
