package com.example.shoter.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import com.example.shoter.network.PhotoPayload
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class PhotoProcessingResult(
    val payload: PhotoPayload,
    val legacyImage: String
)

private val detectorOptions = FaceDetectorOptions.Builder()
    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
    .build()

fun processCapturedPhoto(file: File, angle: String, maxDimension: Int = 1280): PhotoProcessingResult {
    val rawBitmap = BitmapFactory.decodeFile(file.absolutePath)
        ?: throw IllegalStateException("Не удалось декодировать изображение")

    val orientedBitmap = rawBitmap.applyExifOrientation(file)
    if (orientedBitmap != rawBitmap) {
        rawBitmap.recycle()
    }

    val scaledBitmap = orientedBitmap.scaleDown(maxDimension)
    if (scaledBitmap != orientedBitmap) {
        orientedBitmap.recycle()
    }

    val faceRect = detectLargestFace(scaledBitmap)
    val faceBitmap = faceRect?.let { scaledBitmap.cropSafe(it.withMargin(0.2f, scaledBitmap.width, scaledBitmap.height)) }

    val originalBase64 = encodeBitmapToBase64(scaledBitmap)
    val faceBase64 = faceBitmap?.let {
        val result = encodeBitmapToBase64(it)
        it.recycle()
        result
    }

    val payload = PhotoPayload(
        angle = angle,
        original = originalBase64,
        face = faceBase64,
        body = null,
        capturedAt = System.currentTimeMillis(),
        width = scaledBitmap.width,
        height = scaledBitmap.height
    )

    scaledBitmap.recycle()

    return PhotoProcessingResult(
        payload = payload,
        legacyImage = faceBase64 ?: originalBase64
    )
}

private fun Bitmap.scaleDown(maxDimension: Int): Bitmap {
    val largestSide = max(width, height)
    if (largestSide <= maxDimension) return this
    val scale = maxDimension.toFloat() / largestSide
    val newWidth = (width * scale).roundToInt()
    val newHeight = (height * scale).roundToInt()
    return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
}

private fun Bitmap.cropSafe(rect: Rect): Bitmap {
    val safeRect = Rect(
        rect.left.coerceAtLeast(0),
        rect.top.coerceAtLeast(0),
        rect.right.coerceAtMost(width),
        rect.bottom.coerceAtMost(height)
    )
    return Bitmap.createBitmap(this, safeRect.left, safeRect.top, safeRect.width(), safeRect.height())
}

private fun detectLargestFace(bitmap: Bitmap): Rect? {
    val image = InputImage.fromBitmap(bitmap, 0)
    val detector = FaceDetection.getClient(detectorOptions)
    return try {
        val faces = Tasks.await(detector.process(image))
        faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }?.boundingBox
    } catch (_: Exception) {
        null
    } finally {
        detector.close()
    }
}

private fun Rect.withMargin(marginRatio: Float, width: Int, height: Int): Rect {
    val marginX = (width * marginRatio).roundToInt()
    val marginY = (height * marginRatio).roundToInt()
    val left = (this.left - marginX).coerceAtLeast(0)
    val top = (this.top - marginY).coerceAtLeast(0)
    val right = (this.right + marginX).coerceAtMost(width)
    val bottom = (this.bottom + marginY).coerceAtMost(height)
    return Rect(left, top, right, bottom)
}

private fun Bitmap.applyExifOrientation(file: File): Bitmap {
    return try {
        val exif = ExifInterface(file.absolutePath)
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flip(horizontal = true)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> flip(horizontal = false)
            else -> this
        }
    } catch (_: Exception) {
        this
    }
}

private fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(degrees)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun Bitmap.flip(horizontal: Boolean): Bitmap {
    val matrix = Matrix()
    if (horizontal) {
        matrix.preScale(-1f, 1f)
    } else {
        matrix.preScale(1f, -1f)
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

