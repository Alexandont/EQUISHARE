package com.equishare.app.data.remote

import com.equishare.app.BuildConfig
import com.equishare.app.domain.repositorio.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Construye (y reutiliza) el cliente Retrofit para la URL del servidor configurada en DataStore.
 * Agrega el token de Firebase (o de demostración) en la cabecera Authorization de cada petición.
 */
class ApiProvider(
    private val urlServidor: Flow<String>,
    private val auth: AuthRepository,
) {
    @Volatile
    private var cache: Pair<String, EquishareApi>? = null

    private val clienteHttp: OkHttpClient by lazy {
        val autorizacion = Interceptor { cadena ->
            val token = auth.tokenParaApi()
            val peticion = cadena.request().newBuilder().apply {
                header("Accept", "application/json")
                if (token != null) header("Authorization", "Bearer $token")
            }.build()
            cadena.proceed(peticion)
        }
        val registro = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(autorizacion)
            .addInterceptor(registro)
            .build()
    }

    suspend fun api(): EquishareApi = paraUrl(urlServidor.first())

    fun paraUrl(url: String): EquishareApi {
        cache?.let { (urlCache, api) -> if (urlCache == url) return api }
        val api = Retrofit.Builder()
            .baseUrl(url)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EquishareApi::class.java)
        cache = url to api
        return api
    }
}
