package com.equishare.app.domain

import com.equishare.app.domain.model.RangoOcupado
import com.equishare.app.domain.model.TipoError
import com.equishare.app.domain.reglas.ReglasNegocio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ReglasNegocioTest {
    private val ahora: Instant = Instant.parse("2026-10-16T17:00:00Z") // 12:00 en Lima

    @Test
    fun cotizacion_aplicaComisionDel12PorCientoAlArrendador() {
        val c = ReglasNegocio.cotizar(80.0, LocalDate.parse("2026-10-18"), LocalDate.parse("2026-10-20"))
        assertEquals(3, c.dias)
        assertEquals(240.0, c.subtotal, 0.001)
        assertEquals(28.8, c.comision, 0.001)
        assertEquals(211.2, c.montoArrendador, 0.001)
        assertEquals(240.0, c.total, 0.001)
    }

    @Test
    fun anticipacion_exige24HorasAntesDeLaEntrega() {
        assertFalse(ReglasNegocio.cumpleAnticipacion(LocalDate.parse("2026-10-17"), ahora)) // 20 h
        assertTrue(ReglasNegocio.cumpleAnticipacion(LocalDate.parse("2026-10-18"), ahora)) // 44 h
        assertEquals(LocalDate.parse("2026-10-18"), ReglasNegocio.primerDiaReservable(ahora))
    }

    @Test
    fun validarRango_detectaFinAnteriorYExcesoDeDias() {
        assertNotNull(ReglasNegocio.validarRango(LocalDate.parse("2026-10-20"), LocalDate.parse("2026-10-19"), ahora))
        assertNotNull(ReglasNegocio.validarRango(LocalDate.parse("2026-10-20"), LocalDate.parse("2027-02-20"), ahora))
        assertNull(ReglasNegocio.validarRango(LocalDate.parse("2026-10-20"), LocalDate.parse("2026-10-22"), ahora))
    }

    @Test
    fun solapamiento_consideraDiasIncluidos() {
        val ocupados = listOf(RangoOcupado("2026-10-20", "2026-10-22"))
        assertTrue(ReglasNegocio.haySolapamiento(ocupados, LocalDate.parse("2026-10-22"), LocalDate.parse("2026-10-23")))
        assertFalse(ReglasNegocio.haySolapamiento(ocupados, LocalDate.parse("2026-10-23"), LocalDate.parse("2026-10-24")))
    }

    @Test
    fun penalidad_soloSiFaltanMenosDe12Horas() {
        val inicio = LocalDate.parse("2026-10-18") // entrega 13:00 UTC
        assertEquals(0.0, ReglasNegocio.penalidadSiCancela(inicio, 240.0, Instant.parse("2026-10-17T12:00:00Z")), 0.001)
        assertEquals(48.0, ReglasNegocio.penalidadSiCancela(inicio, 240.0, Instant.parse("2026-10-18T05:00:00Z")), 0.001)
    }

    @Test
    fun ventanaResena_de48Horas() {
        val devuelta = Instant.parse("2026-10-10T12:00:00Z")
        assertTrue(ReglasNegocio.dentroDeVentanaResena(devuelta, Instant.parse("2026-10-11T12:00:00Z")))
        assertFalse(ReglasNegocio.dentroDeVentanaResena(devuelta, Instant.parse("2026-10-13T12:01:00Z")))
        assertFalse(ReglasNegocio.dentroDeVentanaResena(null, ahora))
    }

    @Test
    fun distancia_TrujilloHuanchaco() {
        val d = ReglasNegocio.distanciaKm(-8.1116, -79.0287, -8.0792, -79.1199)
        assertTrue("distancia $d", d in 9.0..12.0)
    }

    @Test
    fun codigosHttp_seTraducenATiposDeError() {
        assertEquals(TipoError.VALIDACION, TipoError.desdeCodigoHttp(400))
        assertEquals(TipoError.NO_AUTENTICADO, TipoError.desdeCodigoHttp(401))
        assertEquals(TipoError.PROHIBIDO, TipoError.desdeCodigoHttp(403))
        assertEquals(TipoError.NO_ENCONTRADO, TipoError.desdeCodigoHttp(404))
        assertEquals(TipoError.CONFLICTO, TipoError.desdeCodigoHttp(409))
        assertEquals(TipoError.REGLA_NEGOCIO, TipoError.desdeCodigoHttp(422))
        assertEquals(TipoError.SERVIDOR, TipoError.desdeCodigoHttp(503))
    }

    @Test
    fun formato_monedaYFechas() {
        assertEquals("S/ 1,250.50", Formato.soles(1250.5).replace(' ', ' '))
        assertEquals("18/10/2026", Formato.fecha("2026-10-18"))
        assertEquals("18/10/2026 al 20/10/2026", Formato.rango("2026-10-18", "2026-10-20"))
    }
}
