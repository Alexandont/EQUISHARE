package com.equishare.app.ui

import com.equishare.app.domain.model.Categoria
import com.equishare.app.domain.model.ErrorApp
import com.equishare.app.domain.model.TipoError
import com.equishare.app.ui.auth.LoginViewModel
import com.equishare.app.ui.catalogo.CatalogoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Pruebas de los ViewModel con repositorios falsos (sin red, Room ni Firebase). */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val despachador = UnconfinedTestDispatcher()

    @Before
    fun preparar() = Dispatchers.setMain(despachador)

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun catalogo_cargaDesdeElRepositorioYFiltraPorCategoria() = runTest(despachador) {
        val repo = FakeEquipoRepository()
        val vm = CatalogoViewModel(repo, FakeFavoritos(), FakeRed(), FakeUbicacion(), FakeAjustes())
        backgroundScope.launch { vm.ui.collect {} }

        advanceTimeBy(500)
        advanceUntilIdle()
        assertEquals(2, vm.ui.value.equipos.size)
        assertFalse(vm.ui.value.cargando)

        vm.alElegirCategoria(Categoria.EVENTOS)
        advanceTimeBy(500)
        advanceUntilIdle()
        assertEquals(listOf("2"), vm.ui.value.equipos.map { it.id })
        assertEquals(Categoria.EVENTOS, repo.filtrosRecibidos.last().categoria)
    }

    @Test
    fun catalogo_muestraErrorSinDatosYModoSinConexion() = runTest(despachador) {
        val repo = FakeEquipoRepository().apply { fallarCon = ErrorApp(TipoError.SIN_CONEXION) }
        val red = FakeRed(inicial = false)
        val vm = CatalogoViewModel(repo, FakeFavoritos(), red, FakeUbicacion(), FakeAjustes())
        backgroundScope.launch { vm.ui.collect {} }

        advanceTimeBy(500)
        advanceUntilIdle()
        assertTrue(vm.ui.value.mostrarErrorPantalla)
        assertFalse(vm.ui.value.enLinea)

        // Al volver la conexión se reintenta automáticamente.
        repo.fallarCon = null
        red.estado.value = true
        advanceUntilIdle()
        assertEquals(2, vm.ui.value.equipos.size)
        assertNull(vm.ui.value.error)
    }

    @Test
    fun catalogo_cercaDeMiEnviaUbicacionYRadio() = runTest(despachador) {
        val repo = FakeEquipoRepository()
        val vm = CatalogoViewModel(repo, FakeFavoritos(), FakeRed(), FakeUbicacion(), FakeAjustes())
        backgroundScope.launch { vm.ui.collect {} }
        advanceTimeBy(500)

        vm.alCambiarCercaDeMi(true)
        advanceTimeBy(500)
        advanceUntilIdle()
        val ultimo = repo.filtrosRecibidos.last()
        assertNotNull(ultimo.ubicacion)
        assertEquals(CatalogoViewModel.RADIO_CERCANIA_KM, ultimo.radioKm!!, 0.0)
    }

    @Test
    fun catalogo_alternaFavoritos() = runTest(despachador) {
        val vm = CatalogoViewModel(FakeEquipoRepository(), FakeFavoritos(), FakeRed(), FakeUbicacion(), FakeAjustes())
        backgroundScope.launch { vm.ui.collect {} }
        advanceTimeBy(500)
        advanceUntilIdle()
        val equipo = vm.ui.value.equipos.first()
        vm.alternarFavorito(equipo)
        advanceUntilIdle()
        assertTrue(equipo.id in vm.ui.value.favoritos)
    }

    @Test
    fun login_noLlamaAlRepositorioSiElFormularioEsInvalido() = runTest(despachador) {
        val auth = FakeAuth()
        val vm = LoginViewModel(auth)
        vm.alCambiarEmail("correo-malo")
        vm.alCambiarPassword("123")
        vm.enviar { }
        assertNotNull(vm.ui.value.errorEmail)
        assertNotNull(vm.ui.value.errorPassword)
        assertEquals(0, auth.llamadas)
    }

    @Test
    fun login_registroExigeAceptarPrivacidad() = runTest(despachador) {
        val auth = FakeAuth()
        val vm = LoginViewModel(auth)
        vm.alternarModo()
        vm.alCambiarEmail("ventas@empresa.pe")
        vm.alCambiarPassword("clave2026")
        vm.alCambiarConfirmacion("clave2026")
        vm.enviar { }
        assertNotNull(vm.ui.value.errorPrivacidad)
        assertEquals(0, auth.llamadas)

        vm.alCambiarPrivacidad(true)
        vm.enviar { }
        advanceUntilIdle()
        assertEquals(1, auth.llamadas)
        assertEquals("", vm.ui.value.password) // el formulario se limpia
    }

    @Test
    fun login_muestraErrorDelProveedor() = runTest(despachador) {
        val vm = LoginViewModel(FakeAuth(fallar = true))
        vm.alCambiarEmail("ventas@empresa.pe")
        vm.alCambiarPassword("clave2026")
        vm.enviar { }
        advanceUntilIdle()
        assertEquals("Correo o contraseña incorrectos.", vm.ui.value.errorGeneral)
        assertFalse(vm.ui.value.cargando)
    }
}
