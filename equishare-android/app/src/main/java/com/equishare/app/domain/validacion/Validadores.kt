package com.equishare.app.domain.validacion

/**
 * Validaciones de formularios. Cada función devuelve el mensaje de error o null si el valor es válido.
 * Son las mismas reglas que aplica la API, para dar retroalimentación inmediata sin gastar datos.
 */
object Validadores {
    private val REGEX_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PESOS_RUC = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)
    private val PREFIJOS_RUC = setOf("10", "15", "16", "17", "20")

    fun email(valor: String): String? = when {
        valor.isBlank() -> "Ingresa tu correo."
        !REGEX_EMAIL.matches(valor.trim()) -> "El correo no tiene un formato válido."
        else -> null
    }

    fun password(valor: String): String? = when {
        valor.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
        valor.none { it.isLetter() } || valor.none { it.isDigit() } -> "Usa letras y números en la contraseña."
        else -> null
    }

    fun confirmacion(password: String, confirmacion: String): String? =
        if (password != confirmacion) "Las contraseñas no coinciden." else null

    fun digitoVerificadorRuc(primeros10: String): Int {
        val suma = primeros10.mapIndexed { i, c -> (c - '0') * PESOS_RUC[i] }.sum()
        return when (val resultado = 11 - (suma % 11)) {
            10 -> 0
            11 -> 1
            else -> resultado
        }
    }

    fun ruc(valor: String): String? {
        val r = valor.trim()
        return when {
            r.length != 11 || r.any { !it.isDigit() } -> "El RUC debe tener 11 dígitos."
            r.substring(0, 2) !in PREFIJOS_RUC -> "El RUC debe empezar con 10, 15, 16, 17 o 20."
            digitoVerificadorRuc(r.substring(0, 10)) != r[10] - '0' -> "El dígito verificador del RUC no es correcto."
            else -> null
        }
    }

    fun telefono(valor: String): String? =
        if (!Regex("^9\\d{8}$").matches(valor.trim())) "Ingresa un celular de 9 dígitos que empiece con 9." else null

    fun razonSocial(valor: String): String? =
        if (valor.trim().length !in 3..120) "La razón social debe tener entre 3 y 120 caracteres." else null

    fun nombreEquipo(valor: String): String? =
        if (valor.trim().length !in 3..80) "El nombre debe tener entre 3 y 80 caracteres." else null

    fun descripcion(valor: String): String? =
        if (valor.trim().length !in 10..500) "La descripción debe tener entre 10 y 500 caracteres." else null

    fun direccion(valor: String): String? =
        if (valor.trim().length !in 5..150) "La dirección debe tener entre 5 y 150 caracteres." else null

    /** Acepta coma o punto decimal. */
    fun precio(valor: String): String? {
        val numero = valor.trim().replace(',', '.').toDoubleOrNull()
        return when {
            numero == null -> "Ingresa un precio válido."
            numero < 1.0 || numero > 50_000.0 -> "El precio por día debe estar entre S/ 1 y S/ 50 000."
            else -> null
        }
    }

    fun precioComoNumero(valor: String): Double? = valor.trim().replace(',', '.').toDoubleOrNull()

    fun comentario(valor: String): String? =
        if (valor.trim().length !in 3..300) "El comentario debe tener entre 3 y 300 caracteres." else null

    fun estrellas(valor: Int): String? = if (valor !in 1..5) "Elige de 1 a 5 estrellas." else null

    /** Normaliza la URL del servidor: agrega http:// y la barra final que exige Retrofit. */
    fun normalizarUrlServidor(valor: String): String? {
        var url = valor.trim()
        if (url.isEmpty()) return null
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://$url"
        if (!url.endsWith("/")) url = "$url/"
        val valida = Regex("^https?://[A-Za-z0-9.-]+(:\\d{1,5})?(/[A-Za-z0-9._~/-]*)?$").matches(url)
        return if (valida) url else null
    }
}
