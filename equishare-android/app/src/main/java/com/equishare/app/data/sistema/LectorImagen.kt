package com.equishare.app.data.sistema

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * Lee la imagen elegida con el selector de fotos del sistema (no requiere permiso de almacenamiento),
 * la reduce a un máximo de 1600 px por lado y la comprime en JPEG. Así se ahorra datos móviles y
 * se respeta el límite de 5 MB del servidor (uso responsable de recursos).
 */
class LectorImagen(private val context: Context) {

    data class ImagenPreparada(val bytes: ByteArray, val ancho: Int, val alto: Int)

    suspend fun preparar(uriTexto: String, ladoMaximo: Int = 1600, calidad: Int = 85): ImagenPreparada? =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriTexto)
                val bitmap = decodificar(uri, ladoMaximo) ?: return@runCatching null
                val salida = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)
                val resultado = ImagenPreparada(salida.toByteArray(), bitmap.width, bitmap.height)
                bitmap.recycle()
                resultado
            }.getOrNull()
        }

    private fun decodificar(uri: Uri, ladoMaximo: Int): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder aplica la orientación EXIF de las fotos de cámara.
            val fuente = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(fuente) { decoder, info, _ ->
                val mayor = max(info.size.width, info.size.height)
                if (mayor > ladoMaximo) {
                    val escala = ladoMaximo.toFloat() / mayor
                    decoder.setTargetSize(
                        (info.size.width * escala).toInt().coerceAtLeast(1),
                        (info.size.height * escala).toInt().coerceAtLeast(1),
                    )
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
            var muestreo = 1
            while (max(limites.outWidth, limites.outHeight) / (muestreo * 2) >= ladoMaximo) muestreo *= 2
            val opciones = BitmapFactory.Options().apply { inSampleSize = muestreo }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        }
    }
}
