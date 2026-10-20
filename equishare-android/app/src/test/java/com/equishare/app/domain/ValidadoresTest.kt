package com.equishare.app.domain

import com.equishare.app.domain.validacion.Validadores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidadoresTest {

    @Test
    fun ruc_validoConDigitoVerificador() {
        assertNull(Validadores.ruc("20131312955"))
        assertNull(Validadores.ruc("20100047218"))
    }

    @Test
    fun ruc_invalido() {
        assertNotNull(Validadores.ruc("20131312954")) // dígito verificador
        assertNotNull(Validadores.ruc("30131312955")) // prefijo
        assertNotNull(Validadores.ruc("2013131295")) // longitud
        assertNotNull(Validadores.ruc("2013131295A"))
    }

    @Test
    fun email_y_password() {
        assertNull(Validadores.email("ventas@empresa.pe"))
        assertNotNull(Validadores.email("ventas@empresa"))
        assertNotNull(Validadores.email(""))
        assertNull(Validadores.password("clave2026"))
        assertNotNull(Validadores.password("corta1"))
        assertNotNull(Validadores.password("solamenteletras"))
        assertNotNull(Validadores.confirmacion("clave2026", "clave2025"))
    }

    @Test
    fun telefono_celularPeruano() {
        assertNull(Validadores.telefono("987654321"))
        assertNotNull(Validadores.telefono("87654321"))
        assertNotNull(Validadores.telefono("187654321"))
    }

    @Test
    fun precio_aceptaComaYRango() {
        assertNull(Validadores.precio("80"))
        assertNull(Validadores.precio("80,50"))
        assertEquals(80.5, Validadores.precioComoNumero("80,50")!!, 0.0001)
        assertNotNull(Validadores.precio("0"))
        assertNotNull(Validadores.precio("abc"))
        assertNotNull(Validadores.precio("60000"))
    }

    @Test
    fun textosDeEquipo() {
        assertNull(Validadores.nombreEquipo("Mezcladora"))
        assertNotNull(Validadores.nombreEquipo("ab"))
        assertNotNull(Validadores.descripcion("corta"))
        assertNull(Validadores.descripcion("Mezcladora en buen estado con manual."))
        assertNotNull(Validadores.comentario("ok"))
        assertNotNull(Validadores.estrellas(0))
        assertNull(Validadores.estrellas(5))
    }

    @Test
    fun urlServidor_seNormaliza() {
        assertEquals("http://10.0.2.2:3000/", Validadores.normalizarUrlServidor("10.0.2.2:3000"))
        assertEquals("http://192.168.1.50:3000/", Validadores.normalizarUrlServidor("http://192.168.1.50:3000"))
        assertEquals("https://api.equishare.pe/", Validadores.normalizarUrlServidor("https://api.equishare.pe/"))
        assertNull(Validadores.normalizarUrlServidor("no es url"))
        assertNull(Validadores.normalizarUrlServidor(""))
    }
}
