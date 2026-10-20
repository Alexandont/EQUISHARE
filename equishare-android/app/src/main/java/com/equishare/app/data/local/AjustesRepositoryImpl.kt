package com.equishare.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.equishare.app.BuildConfig
import com.equishare.app.domain.model.Categoria
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.Sesion
import com.equishare.app.domain.repositorio.AjustesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ajustesDataStore: DataStore<Preferences> by preferencesDataStore(name = "ajustes")

/**
 * Preferencias pequeñas con Jetpack DataStore: URL del servidor, último filtro de categoría
 * y la sesión del modo demostración. No se guardan contraseñas ni tokens.
 */
class AjustesRepositoryImpl(
    context: Context,
    private val probador: suspend (String) -> Resultado<String>,
) : AjustesRepository {

    private val dataStore = context.applicationContext.ajustesDataStore

    override val urlServidor: Flow<String> =
        dataStore.data.map { it[CLAVE_URL] ?: BuildConfig.API_BASE_URL }

    override val categoriaFiltro: Flow<Categoria?> =
        dataStore.data.map { Categoria.desde(it[CLAVE_CATEGORIA]) }

    val sesionDemo: Flow<Sesion?> = dataStore.data.map { prefs ->
        val uid = prefs[CLAVE_DEMO_UID]
        if (uid.isNullOrBlank()) null else Sesion(uid = uid, email = prefs[CLAVE_DEMO_EMAIL], esDemo = true)
    }

    override suspend fun guardarUrlServidor(url: String) {
        dataStore.edit { it[CLAVE_URL] = url }
    }

    override suspend fun guardarCategoriaFiltro(categoria: Categoria?) {
        dataStore.edit { prefs ->
            if (categoria == null) prefs.remove(CLAVE_CATEGORIA) else prefs[CLAVE_CATEGORIA] = categoria.name
        }
    }

    override suspend fun probarServidor(url: String): Resultado<String> = probador(url)

    suspend fun guardarSesionDemo(sesion: Sesion?) {
        dataStore.edit { prefs ->
            if (sesion == null) {
                prefs.remove(CLAVE_DEMO_UID)
                prefs.remove(CLAVE_DEMO_EMAIL)
            } else {
                prefs[CLAVE_DEMO_UID] = sesion.uid
                sesion.email?.let { prefs[CLAVE_DEMO_EMAIL] = it }
            }
        }
    }

    private companion object {
        val CLAVE_URL = stringPreferencesKey("url_servidor")
        val CLAVE_CATEGORIA = stringPreferencesKey("categoria_filtro")
        val CLAVE_DEMO_UID = stringPreferencesKey("demo_uid")
        val CLAVE_DEMO_EMAIL = stringPreferencesKey("demo_email")
    }
}
