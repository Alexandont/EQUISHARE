package com.equishare.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.repositorio.AuthRepository
import com.equishare.app.domain.validacion.Validadores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val confirmacion: String = "",
    val modoRegistro: Boolean = false,
    /** Casilla de consentimiento SIN marcar por defecto (diseño ético, Anexo de marketing). */
    val aceptaPrivacidad: Boolean = false,
    val errorEmail: String? = null,
    val errorPassword: String? = null,
    val errorConfirmacion: String? = null,
    val errorPrivacidad: String? = null,
    val cargando: Boolean = false,
    val errorGeneral: String? = null,
    val esModoDemo: Boolean = false,
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState(esModoDemo = auth.esModoDemo))
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    fun alCambiarEmail(valor: String) = _ui.update { it.copy(email = valor, errorEmail = null, errorGeneral = null) }
    fun alCambiarPassword(valor: String) = _ui.update { it.copy(password = valor, errorPassword = null, errorGeneral = null) }
    fun alCambiarConfirmacion(valor: String) = _ui.update { it.copy(confirmacion = valor, errorConfirmacion = null) }
    fun alCambiarPrivacidad(valor: Boolean) = _ui.update { it.copy(aceptaPrivacidad = valor, errorPrivacidad = null) }
    fun alternarModo() = _ui.update {
        it.copy(modoRegistro = !it.modoRegistro, errorGeneral = null, errorConfirmacion = null, errorPrivacidad = null)
    }

    /** Valida el formulario; devuelve true si se puede enviar. */
    fun validar(): Boolean {
        val estado = _ui.value
        val errorEmail = Validadores.email(estado.email)
        val errorPassword = Validadores.password(estado.password)
        val errorConfirmacion = if (estado.modoRegistro) Validadores.confirmacion(estado.password, estado.confirmacion) else null
        val errorPrivacidad = if (estado.modoRegistro && !estado.aceptaPrivacidad) {
            "Debes leer y aceptar la política de privacidad para crear la cuenta."
        } else {
            null
        }
        _ui.update {
            it.copy(
                errorEmail = errorEmail,
                errorPassword = errorPassword,
                errorConfirmacion = errorConfirmacion,
                errorPrivacidad = errorPrivacidad,
            )
        }
        return listOf(errorEmail, errorPassword, errorConfirmacion, errorPrivacidad).all { it == null }
    }

    fun enviar(alRegistrar: (String) -> Unit) {
        if (_ui.value.cargando || !validar()) return
        val estado = _ui.value
        _ui.update { it.copy(cargando = true, errorGeneral = null) }
        viewModelScope.launch {
            val resultado = if (estado.modoRegistro) {
                auth.registrar(estado.email, estado.password)
            } else {
                auth.iniciarSesion(estado.email, estado.password)
            }
            when (resultado) {
                is Resultado.Exito -> {
                    // Se limpia el formulario: la contraseña no queda en memoria tras ingresar.
                    _ui.value = LoginUiState(esModoDemo = auth.esModoDemo)
                    if (estado.modoRegistro) alRegistrar(estado.email.trim())
                }
                is Resultado.Error -> _ui.update { it.copy(cargando = false, errorGeneral = resultado.error.mensaje) }
            }
        }
    }
}
