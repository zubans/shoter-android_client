package com.example.shoter.util

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

fun encodeBitmapToBase64(bitmap: Bitmap): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}
