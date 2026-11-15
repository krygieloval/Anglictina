package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.LruCache
import java.io.ByteArrayOutputStream

object ImageCodec {
    private const val MAX_DIMENSION = 320
    private const val MAX_BASE64_LENGTH = 120_000

    private val memoryCache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun encodeUriToBase64(context: Context, uri: Uri): String {
        val resolver = context.contentResolver
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val maxSide = maxOf(info.size.width, info.size.height)
                if (maxSide > MAX_DIMENSION * 2) {
                    val ratio = (MAX_DIMENSION * 2).toFloat() / maxSide.toFloat()
                    decoder.setTargetSize(
                        (info.size.width * ratio).toInt().coerceAtLeast(1),
                        (info.size.height * ratio).toInt().coerceAtLeast(1)
                    )
                }
            }
        } else {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
            var sampleSize = 1
            while (maxSide / sampleSize > MAX_DIMENSION * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: throw IllegalStateException("Nepodařilo se otevřít vybraný obrázek.")
        }
        return try {
            encodeToBase64(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    fun encodeToBase64(bitmap: Bitmap): String {
        val scaled = scaleBitmap(bitmap, MAX_DIMENSION)
        var quality = 82
        var encoded: String
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            quality -= 12
        } while (encoded.length > MAX_BASE64_LENGTH && quality >= 30)
        return encoded
    }

    fun decodeFromBase64(cacheKey: String, base64: String): Bitmap? {
        if (base64.isBlank()) return null
        val key = "${cacheKey}_${base64.hashCode()}"
        memoryCache.get(key)?.let { return it }
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                memoryCache.put(key, bitmap)
            }
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmap(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source
        val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
        val newW = (width * ratio).toInt().coerceAtLeast(1)
        val newH = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
