package com.yummy.app.ui.addrecipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageUtils {
    private const val MAX_SIDE = 1280
    private const val QUALITY = 85

    /** Lee la imagen elegida, la reduce (máx. 1280 px), corrige la rotación y la devuelve como JPEG. */
    fun readJpeg(context: Context, uri: Uri): ByteArray? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val rotation = resolver.openInputStream(uri)?.use { exifRotation(it) } ?: 0
        val matrix = Matrix()
        if (rotation != 0) matrix.postRotate(rotation.toFloat())
        val longest = max(decoded.width, decoded.height)
        if (longest > MAX_SIDE) {
            val scale = MAX_SIDE.toFloat() / longest
            matrix.postScale(scale, scale)
        }

        val result = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        val out = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        if (result !== decoded) decoded.recycle()
        result.recycle()
        return out.toByteArray()
    }

    private fun exifRotation(stream: InputStream): Int = try {
        when (
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (e: Exception) {
        0
    }
}