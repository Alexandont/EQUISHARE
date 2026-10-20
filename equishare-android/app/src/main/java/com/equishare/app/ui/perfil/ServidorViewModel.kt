package com.equishare.app.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equishare.app.domain.model.Resultado
import com.equishare.app.domain.repositorio.AjustesRepository
import com.equishare.app.domain.validacion.Validadores
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServidorUiState(
    val url: String = "",
    val probando: Boolean = false,
    val mensaje: String? = null,
    val exito: Boolean = false,
)

/** Configura la dirección del servidor Node.js (emulador: http://10.0.2.2:3000/). */
class ServidorViewModel(private val ajustes: AjustesRepository) : ViewModel() {
    private val _ui = MutableStateFlow(ServidorUiState())
    val ui: StateFlow<ServidorUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch { _ui.update { it.copy(url = ajustes.urlServidor.first()) } }
    }

    fun alCambiarUrl(valor: String) = _ui.update { it.copy(url = valor, mensaje = null, exito = false) }

    fun probarYGuardar(alGuardar: () -> Unit) {
        val url = Validadores.normalizarUrlServidor(_ui.value.url)
        if (url == null) {
            _ui.update { it.copy(mensaje = "Escribe una dirección válida, por ejemplo http://192.168.1.50:3000/", exito = false) }
            return
        }
        _ui.update { it.copy(url = url, probando = true, mensaje = null) }
        viewModelScope.launch {
            when (val resultado = ajustes.probarServidor(url)) {
                is Resultado.Exito -> {
                    ajustes.guardarUrlServidor(url)
                    _ui.update {
                        it.copy(probando = false, exito = true, mensaje = "Conectado. Autenticación del servidor: ${resultado.datos}.")
                    }
                    alGuardar()
                }
                is Resultado.Error -> _ui.update {
                    it.copy(probando = false, exito = false, mensaje = "No se pudo conectar: ${resultado.error.mensaje}")
                }
            }
        }
    }
}
