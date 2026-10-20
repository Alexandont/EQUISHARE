package com.equishare.app.data.remote

import com.equishare.app.data.remote.dto.CambioEstadoDto
import com.equishare.app.data.remote.dto.CotizacionDto
import com.equishare.app.data.remote.dto.DisponibilidadDto
import com.equishare.app.data.remote.dto.EmpresaDto
import com.equishare.app.data.remote.dto.EmpresaEntradaDto
import com.equishare.app.data.remote.dto.EquipoDto
import com.equishare.app.data.remote.dto.EquipoEntradaDto
import com.equishare.app.data.remote.dto.FotoDto
import com.equishare.app.data.remote.dto.PaginaDto
import com.equishare.app.data.remote.dto.ResenaCreadaDto
import com.equishare.app.data.remote.dto.ResenaEntradaDto
import com.equishare.app.data.remote.dto.ResenasDto
import com.equishare.app.data.remote.dto.ReservaDto
import com.equishare.app.data.remote.dto.SaludDto
import com.equishare.app.data.remote.dto.SolicitudReservaDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints de la API REST de EQUISHARE (Node.js + Express) consumidos con Retrofit y corrutinas. */
interface EquishareApi {

    @GET("api/health")
    suspend fun salud(): SaludDto

    // ---- Equipos ----
    @GET("api/equipos")
    suspend fun listarEquipos(
        @Query("q") q: String?,
        @Query("categoria") categoria: String?,
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
        @Query("radioKm") radioKm: Double?,
    ): PaginaDto<EquipoDto>

    @GET("api/equipos/mios")
    suspend fun misEquipos(): PaginaDto<EquipoDto>

    @GET("api/equipos/{id}")
    suspend fun obtenerEquipo(@Path("id") id: String): EquipoDto

    @POST("api/equipos")
    suspend fun crearEquipo(@Body cuerpo: EquipoEntradaDto): EquipoDto

    @PUT("api/equipos/{id}")
    suspend fun actualizarEquipo(@Path("id") id: String, @Body cuerpo: EquipoEntradaDto): EquipoDto

    @DELETE("api/equipos/{id}")
    suspend fun eliminarEquipo(@Path("id") id: String): Response<Unit>

    @Multipart
    @POST("api/equipos/fotos")
    suspend fun subirFoto(@Part foto: MultipartBody.Part): FotoDto

    @GET("api/equipos/{id}/disponibilidad")
    suspend fun disponibilidad(@Path("id") id: String): DisponibilidadDto

    @GET("api/equipos/{id}/resenas")
    suspend fun resenasEquipo(@Path("id") id: String): ResenasDto

    // ---- Empresas ----
    @GET("api/empresas/me")
    suspend fun miEmpresa(): EmpresaDto

    @PUT("api/empresas/me")
    suspend fun guardarEmpresa(@Body cuerpo: EmpresaEntradaDto): EmpresaDto

    // ---- Reservas ----
    @POST("api/reservas/cotizacion")
    suspend fun cotizar(@Body cuerpo: SolicitudReservaDto): CotizacionDto

    @POST("api/reservas")
    suspend fun crearReserva(@Body cuerpo: SolicitudReservaDto): ReservaDto

    @GET("api/reservas/mias")
    suspend fun misReservas(@Query("rol") rol: String): PaginaDto<ReservaDto>

    @PATCH("api/reservas/{id}/cancelar")
    suspend fun cancelarReserva(@Path("id") id: String): ReservaDto

    @PATCH("api/reservas/{id}/estado")
    suspend fun cambiarEstado(@Path("id") id: String, @Body cuerpo: CambioEstadoDto): ReservaDto

    @POST("api/reservas/{id}/resenas")
    suspend fun calificar(@Path("id") id: String, @Body cuerpo: ResenaEntradaDto): ResenaCreadaDto
}
