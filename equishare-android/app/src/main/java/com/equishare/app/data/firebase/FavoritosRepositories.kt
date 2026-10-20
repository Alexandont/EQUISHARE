package com.equishare.app.data.firebase

import com.equishare.app.domain.model.Equipo
import com.equishare.app.domain.model.ErrorApp
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.TipoError
import com.equishare.app.domain.repositorio.AuthRepository
import com.equishare.app.domain.repositorio.FavoritosRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Favoritos y consentimiento de privacidad en Cloud Firestore:
 *   usuarios/{uid}                       -> correo y consentimiento (versión y fecha)
 *   usuarios/{uid}/favoritos/{equipoId}  -> nombre, categoría y fecha
 * Las reglas de seguridad (firestore.rules) solo permiten que cada usuario lea y escriba lo suyo.
 * Firestore guarda los cambios sin conexión y los sincroniza al volver la red.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreFavoritosRepository(
    private val auth: AuthRepository,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : FavoritosRepository {

    override fun observar(): Flow<Set<String>> = auth.sesion.flatMapLatest { sesion ->
        if (sesion == null) {
            flowOf(emptySet())
        } else {
            callbackFlow {
                val registro = db.collection(USUARIOS).document(sesion.uid).collection(FAVORITOS)
                    .addSnapshotListener { instantanea, error ->
                        if (error != null) {
                            trySend(emptySet())
                        } else {
                            trySend(instantanea?.documents?.map { it.id }?.toSet() ?: emptySet())
                        }
                    }
                awaitClose { registro.remove() }
            }
        }
    }

    override suspend fun establecer(equipo: Equipo, favorito: Boolean): Resultado<Unit> {
        val uid = auth.sesion.value?.uid
            ?: return Resultado.Error(ErrorApp(TipoError.NO_AUTENTICADO))
        val referencia = db.collection(USUARIOS).document(uid).collection(FAVORITOS).document(equipo.id)
        // No se espera la confirmación del servidor: Firestore aplica el cambio localmente al instante.
        if (favorito) {
            referencia.set(
                mapOf(
                    "nombre" to equipo.nombre,
                    "categoria" to equipo.categoria.name,
                    "agregadoEn" to FieldValue.serverTimestamp(),
                ),
            )
        } else {
            referencia.delete()
        }
        return Resultado.Exito(Unit)
    }

    override suspend fun registrarConsentimiento(email: String?): Resultado<Unit> {
        val uid = auth.sesion.value?.uid
            ?: return Resultado.Error(ErrorApp(TipoError.NO_AUTENTICADO))
        db.collection(USUARIOS).document(uid).set(
            mapOf(
                "email" to email,
                "consentimientoPrivacidad" to mapOf(
                    "version" to VERSION_POLITICA,
                    "aceptadoEn" to FieldValue.serverTimestamp(),
                ),
            ),
            SetOptions.merge(),
        )
        return Resultado.Exito(Unit)
    }

    private companion object {
        const val USUARIOS = "usuarios"
        const val FAVORITOS = "favoritos"
        const val VERSION_POLITICA = "1.0-2026"
    }
}

/** Implementación en memoria para el modo demostración (sin Firebase). */
class MemoriaFavoritosRepository : FavoritosRepository {
    private val favoritos = MutableStateFlow<Set<String>>(emptySet())

    override fun observar(): Flow<Set<String>> = favoritos.asStateFlow()

    override suspend fun establecer(equipo: Equipo, favorito: Boolean): Resultado<Unit> {
        favoritos.value = if (favorito) favoritos.value + equipo.id else favoritos.value - equipo.id
        return Resultado.Exito(Unit)
    }

    override suspend fun registrarConsentimiento(email: String?): Resultado<Unit> = Resultado.Exito(Unit)
}
