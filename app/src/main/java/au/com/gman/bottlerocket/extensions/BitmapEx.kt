package au.com.gman.bottlerocket.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import kotlin.math.roundToInt

fun Bitmap.extractPageWithPerspective(
    pageBounds: RocketBoundingBox,
    outputWidth: Int,
    outputHeight: Int
): Bitmap {
    // Source corners (the distorted quadrilateral in the original image)
    val srcCorners =
        pageBounds
            .toFloatArray()

    // Destination corners (rectangle in output image)
    val dstCorners = floatArrayOf(
        0f, 0f,                             // top-left
        outputWidth.toFloat(), 0f,          // top-right
        outputWidth.toFloat(), outputHeight.toFloat(), // bottom-right
        0f, outputHeight.toFloat()          // bottom-left
    )

    // Create transformation matrix
    val matrix = Matrix()
    matrix.setPolyToPoly(srcCorners, 0, dstCorners, 0, 4)

    // Create output bitmap
    val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    // Draw the transformed bitmap
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    canvas.drawBitmap(this, matrix, paint)

    return output
}

fun Bitmap.extractPageWithPerspective(
    pageBounds: RocketBoundingBox,
    targetDimensions: RocketBoundingBox
): Bitmap {
    // Calculate dimensions from the target template
    val templateWidth = distance(targetDimensions.topLeft, targetDimensions.topRight)
    val templateHeight = distance(targetDimensions.topLeft, targetDimensions.bottomLeft)

    // Use template dimensions as output size (scale for reasonable resolution)
    // Assuming template is in "QR units", scale up to get good resolution
    val outputWidth = (templateWidth * 200).roundToInt()  // 200 pixels per QR unit
    val outputHeight = (templateHeight * 200).roundToInt()

    return extractPageWithPerspective(pageBounds, outputWidth, outputHeight)
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    if (degrees == 0f) return this
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)

}
private fun distance(p1: android.graphics.PointF, p2: android.graphics.PointF): Float {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    return kotlin.math.sqrt(dx * dx + dy * dy)
}
