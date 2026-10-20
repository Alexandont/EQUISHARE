package com.equishare.app.ui.raiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equishare.app.domain.model.Empresa
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.model.Sesion
import com.equishare.app.domain.model.TipoError
import com.equishare.app.domain.repositorio.AuthRepository
import com.equishare.app.domain.repositorio.EmpresaRepository
import com.equishare.app.domain.repositorio.FavoritosRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

sealed interface EstadoRaiz {
    data object Cargando : EstadoRaiz
    data object SinSesion : EstadoRaiz
    data class PerfilIncompleto(val sesion: Sesion) : EstadoRaiz
    data class Listo(val sesion: Sesion, val empresa: Empresa?) : EstadoRaiz
    data class ErrorServidor(val sesion: Sesion, val mensaje: String) : EstadoRaiz
}

/**
 * Decide qué mostrar al abrir la app: login, registro de empresa (RN-01) o la app principal.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RaizViewModel(
    private val auth: AuthRepository,
    private val empresas: EmpresaRepository,
    private val favoritos: FavoritosRepository,
) : ViewModel() {

    private val reintento = MutableStateFlow(0)
    private val sinConexionAceptado = MutableStateFlow(false)

    val estado: StateFlow<EstadoRaiz> =
        combine(auth.sesionCargada, auth.sesion, reintento, sinConexionAceptado) { cargada, sesion, _, offline ->
            Triple(cargada, sesion, offline)
        }.transformLatest { (cargada, sesion, offline) ->
            when {
                !cargada -> emit(EstadoRaiz.Cargando)
                sesion == null -> emit(EstadoRaiz.SinSesion)
                offline -> emit(EstadoRaiz.Listo(sesion, null))
                else -> {
                    emit(EstadoRaiz.Cargando)
                    when (val resultado = empresas.miEmpresa()) {
                        is Resultado.Exito -> {
                            val empresa = resultado.datos
                            emit(if (empresa == null) EstadoRaiz.PerfilIncompleto(sesion) else EstadoRaiz.Listo(sesion, empresa))
                        }
                        is Resultado.Error -> {
                            if (resultado.error.tipo == TipoError.NO_AUTENTICADO) {
                                auth.cerrarSesion()
                                emit(EstadoRaiz.SinSesion)
                            } else {
                                emit(EstadoRaiz.ErrorServidor(sesion, resultado.error.mensaje))
                            }
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, EstadoRaiz.Cargando)

    fun reintentar() {
        sinConexionAceptado.value = false
        reintento.value = reintento.value + 1
    }

    /** Permite usar la app con la información guardada aunque el servidor no responda. */
    fun continuarSinConexion() {
        sinConexionAceptado.value = true
    }

    fun perfilCompletado() = reintentar()

    fun cerrarSesion() {
        viewModelScope.launch { auth.cerrarSesion() }
    }

    fun registrarConsentimiento(email: String?) {
        viewModelScope.launch { favoritos.registrarConsentimiento(email) }
    }
}
