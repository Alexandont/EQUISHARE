package com.equishare.app.data.auth

import com.equishare.app.domain.model.ErrorApp
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.Sesion
import com.equishare.app.domain.model.TipoError
import com.equishare.app.domain.repositorio.AuthRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/** Autenticación con Firebase Authentication (correo y contraseña). */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthRepository {

    private val _sesion = MutableStateFlow(firebaseAuth.currentUser?.aSesion())
    override val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()
    override val sesionCargada: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
    override val esModoDemo: Boolean = false

    init {
        firebaseAuth.addAuthStateListener { auth -> _sesion.value = auth.currentUser?.aSesion() }
    }

    override suspend fun iniciarSesion(email: String, password: String): Resultado<Sesion> = ejecutar {
        firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await().user
    }

    override suspend fun registrar(email: String, password: String): Resultado<Sesion> = ejecutar {
        firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await().user
    }

    override suspend fun cerrarSesion() {
        firebaseAuth.signOut()
    }

    override fun tokenParaApi(): String? {
        val usuario = firebaseAuth.currentUser ?: return null
        return runCatching { Tasks.await(usuario.getIdToken(false), 10, TimeUnit.SECONDS).token }.getOrNull()
    }

    private suspend fun ejecutar(accion: suspend () -> FirebaseUser?): Resultado<Sesion> = try {
        val usuario = accion()
        if (usuario == null) {
            Resultado.Error(ErrorApp(TipoError.DESCONOCIDO, "No se pudo iniciar la sesión."))
        } else {
            val sesion = usuario.aSesion()
            _sesion.value = sesion
            Resultado.Exito(sesion)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseAuthWeakPasswordException) {
        Resultado.Error(ErrorApp(TipoError.VALIDACION, "La contraseña es muy débil. Usa al menos 8 caracteres con letras y números."))
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Resultado.Error(ErrorApp(TipoError.NO_AUTENTICADO, "Correo o contraseña incorrectos."))
    } catch (e: FirebaseAuthInvalidUserException) {
        Resultado.Error(ErrorApp(TipoError.NO_AUTENTICADO, "No existe una cuenta con ese correo o fue deshabilitada."))
    } catch (e: FirebaseAuthUserCollisionException) {
        Resultado.Error(ErrorApp(TipoError.CONFLICTO, "Ya existe una cuenta con ese correo. Inicia sesión."))
    } catch (e: FirebaseNetworkException) {
        Resultado.Error(ErrorApp(TipoError.SIN_CONEXION, "Sin conexión a internet para validar tu cuenta."))
    } catch (e: Exception) {
        Resultado.Error(ErrorApp(TipoError.DESCONOCIDO, e.localizedMessage ?: "No se pudo completar la autenticación."))
    }

    private fun FirebaseUser.aSesion(): Sesion = Sesion(uid = uid, email = email, esDemo = false)
}
