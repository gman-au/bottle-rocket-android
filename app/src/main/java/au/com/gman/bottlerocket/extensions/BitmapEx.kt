package au.com.gman.bottlerocket.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import au.com.gman.bottlerocket.domain.RocketBoundingBox


fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.enhanceImage(
    pageBoundingBoxUnscaled: RocketBoundingBox? = null,
    pageBoundingBoxScaled: RocketBoundingBox? = null,
): Bitmap {

    val enhanced =
        this
            .copy(Bitmap.Config.ARGB_8888, true)

    val contrastFactor = 1.2f
    val offset = 0f

    val cm = ColorMatrix(
        floatArrayOf(
            contrastFactor, 0f, 0f, 0f, offset,
            0f, contrastFactor, 0f, 0f, offset,
            0f, 0f, contrastFactor, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val paint = Paint()
    paint.colorFilter = ColorMatrixColorFilter(cm)

    val canvas = Canvas(enhanced)

    canvas
        .drawBitmap(
            this,
            0f,
            0f,
            paint
        )

    val debugPaintRed = Paint()
        .apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

    val debugPaintGreen = Paint()
        .apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

    if (pageBoundingBoxUnscaled != null)
        canvas
            .drawPath(
                pageBoundingBoxUnscaled.toPath(),
                debugPaintRed
            )

    if (pageBoundingBoxScaled != null)
        canvas
            .drawPath(
                pageBoundingBoxScaled.toPath(),
                debugPaintGreen
            )

    return enhanced
}
