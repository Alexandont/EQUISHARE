package com.equishare.app.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.equishare.app.AppContainer
import com.equishare.app.EquishareApp
import com.equishare.app.ui.auth.CompletarPerfilViewModel
import com.equishare.app.ui.auth.LoginViewModel
import com.equishare.app.ui.catalogo.CatalogoViewModel
import com.equishare.app.ui.detalle.DetalleViewModel
import com.equishare.app.ui.mapa.MapaViewModel
import com.equishare.app.ui.misequipos.MisEquiposViewModel
import com.equishare.app.ui.perfil.PerfilViewModel
import com.equishare.app.ui.perfil.ServidorViewModel
import com.equishare.app.ui.publicar.PublicarViewModel
import com.equishare.app.ui.raiz.RaizViewModel
import com.equishare.app.ui.reservas.ReservasViewModel

/** Fábrica única de ViewModels: entrega a cada uno solo las interfaces de repositorio que necesita. */
object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { RaizViewModel(contenedor().auth, contenedor().empresas, contenedor().favoritos) }
        initializer { LoginViewModel(contenedor().auth) }
        initializer { CompletarPerfilViewModel(contenedor().empresas, contenedor().favoritos, contenedor().auth) }
        initializer {
            CatalogoViewModel(
                contenedor().equipos,
                contenedor().favoritos,
                contenedor().red,
                contenedor().ubicacion,
                contenedor().ajustes,
            )
        }
        initializer {
            DetalleViewModel(
                createSavedStateHandle(),
                contenedor().equipos,
                contenedor().reservas,
                contenedor().favoritos,
                contenedor().auth,
            )
        }
        initializer {
            PublicarViewModel(createSavedStateHandle(), contenedor().equipos, contenedor().ubicacion)
        }
        initializer { MisEquiposViewModel(contenedor().equipos, contenedor().red) }
        initializer { ReservasViewModel(contenedor().reservas, contenedor().red) }
        initializer { MapaViewModel(contenedor().equipos, contenedor().ubicacion) }
        initializer {
            PerfilViewModel(contenedor().empresas, contenedor().auth) { contenedor().limpiarDatosDeSesion() }
        }
        initializer { ServidorViewModel(contenedor().ajustes) }
    }
}

private fun CreationExtras.contenedor(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as EquishareApp).container
