package com.equishare.app.data.auth

import com.equishare.app.data.local.AjustesRepositoryImpl
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.Sesion
import com.equishare.app.domain.repositorio.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MODO DEMOSTRACIÓN (cuando no existe app/google-services.json).
 * No valida contraseñas contra un proveedor: el identificador se deriva del correo para poder usar
 * las empresas de prueba del servidor (constructora@demo.pe, eventos@demo.pe, taller@demo.pe).
 * Solo para desarrollo y exposición; con Firebase configurado se usa [FirebaseAuthRepository].
 */
class DemoAuthRepository(
    private val ajustes: AjustesRepositoryImpl,
    alcance: CoroutineScope,
) : AuthRepository {

    private val _sesion = MutableStateFlow<Sesion?>(null)
    override val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    private val _sesionCargada = MutableStateFlow(false)
    override val sesionCargada: StateFlow<Boolean> = _sesionCargada.asStateFlow()

    override val esModoDemo: Boolean = true

    init {
        alcance.launch {
            ajustes.sesionDemo.collect { guardada ->
                _sesion.value = guardada
                _sesionCargada.value = true
            }
        }
    }

    override suspend fun iniciarSesion(email: String, password: String): Resultado<Sesion> {
        val sesion = Sesion(uid = uidDesdeEmail(email), email = email.trim().lowercase(), esDemo = true)
        _sesion.value = sesion
        ajustes.guardarSesionDemo(sesion)
        return Resultado.Exito(sesion)
    }

    override suspend fun registrar(email: String, password: String): Resultado<Sesion> =
        iniciarSesion(email, password)

    override suspend fun cerrarSesion() {
        _sesion.value = null
        ajustes.guardarSesionDemo(null)
    }

    override fun tokenParaApi(): String? = _sesion.value?.let { "demo:${it.uid}" }

    companion object {
        fun uidDesdeEmail(email: String): String {
            val local = email.trim().lowercase().substringBefore('@')
                .replace(Regex("[^a-z0-9-]"), "-")
                .trim('-')
                .take(40)
                .ifBlank { "usuario" }
            return "demo-$local"
        }
    }
}
