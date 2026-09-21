package com.ledger.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

/* Receipt photos — downscaled JPEG data URLs stored on the expense model. */

private const val RECEIPT_PREFIX = "data:image/jpeg;base64,"

/** Downscale to [maxDim] on the longest edge and encode as a JPEG data URL (~0.6 quality). */
fun bitmapToReceiptDataUrl(bitmap: Bitmap, maxDim: Int = 600, quality: Int = 60): String {
    val longest = maxOf(bitmap.width, bitmap.height).coerceAtLeast(1)
    val scale = if (longest > maxDim) maxDim.toFloat() / longest else 1f
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    } else bitmap
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
    if (scaled !== bitmap) scaled.recycle()
    return RECEIPT_PREFIX + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

/** Decode a receipt data URL back into a bitmap, or null if it isn't one. */
fun decodeReceipt(dataUrl: String?): Bitmap? {
    if (dataUrl.isNullOrBlank()) return null
    val comma = dataUrl.indexOf(',')
    if (comma < 0) return null
    return runCatching {
        val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}
