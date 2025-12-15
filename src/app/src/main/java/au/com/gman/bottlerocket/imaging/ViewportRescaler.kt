package au.com.gman.bottlerocket.imaging

import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import au.com.gman.bottlerocket.BottleRocketApplication.AppConstants
import au.com.gman.bottlerocket.domain.normalize
import au.com.gman.bottlerocket.domain.toFloatArray
import javax.inject.Inject

class ViewportRescaler @Inject constructor() : IViewportRescaler {

    override fun calculateScalingFactorWithOffset(
        firstWidth: Float,
        firstHeight: Float,
        secondWidth: Float,
        secondHeight: Float,
        rotationAngle: Int
    ): ScaleAndOffset {

        // Account for rotation - swap dimensions if rotated 90° or 270°
        val (actualFirstW, actualFirstH) = if (rotationAngle % 180 != 0) {
            Pair(firstHeight, firstWidth)
        } else {
            Pair(firstWidth, firstHeight)
        }

        // Calculate aspect ratios
        val firstAspect = actualFirstW / actualFirstH
        val secondAspect = secondWidth / secondHeight

        val scale: PointF
        val offset: PointF

        if (firstAspect > secondAspect) {
            // First is wider - horizontal crop (left/right sides cut off)
            // Scale based on height
            val uniformScale = secondHeight / actualFirstH
            scale = PointF(uniformScale, uniformScale)

            // Calculate how much width is cropped
            val scaledWidth = actualFirstW * uniformScale
            val cropAmount = (scaledWidth - secondWidth) / 2f

            // The crop happens in the FIRST coordinate space, then scaled
            val cropInFirstSpace = cropAmount / uniformScale
            offset = PointF(-cropInFirstSpace * uniformScale, 0f)

        } else {
            // First is taller - vertical crop (top/bottom cut off)
            // Scale based on width
            val uniformScale = secondWidth / actualFirstW
            scale = PointF(uniformScale, uniformScale)

            // Calculate how much height is cropped
            val scaledHeight = actualFirstH * uniformScale
            val cropAmount = (scaledHeight - secondHeight) / 2f

            val cropInFirstSpace = cropAmount / uniformScale
            offset = PointF(0f, -cropInFirstSpace * uniformScale)
        }

        Log.d(
            AppConstants.APPLICATION_LOG_TAG,
            "First: ${actualFirstW}x${actualFirstH} (aspect: ${firstAspect})"
        )
        Log.d(
            AppConstants.APPLICATION_LOG_TAG,
            "Second: ${secondWidth}x${secondHeight} (aspect: ${secondAspect})"
        )
        Log.d(AppConstants.APPLICATION_LOG_TAG, "Scale: ${scale.x}, ${scale.y}")
        Log.d(AppConstants.APPLICATION_LOG_TAG, "Offset: ${offset.x}, ${offset.y}")

        return ScaleAndOffset(scale, offset)
    }

    override fun calculatePageBounds(
        qrBoxIdeal: RocketBoundingBox,    // Raw barcode corners (camera space)
        qrBoxActual: RocketBoundingBox,   // Scaled result (screen space)
        pageBoxIdeal: RocketBoundingBox   // Template offsets
    ): RocketBoundingBox {

        // Step 1: Normalize the IDEAL QR to get its shape/size
        val normalizedQrIdeal = qrBoxIdeal.normalize()

        // Step 2: Calculate QR dimensions
        val qrWidth = normalizedQrIdeal.topRight.x - normalizedQrIdeal.topLeft.x
        val qrHeight = normalizedQrIdeal.bottomLeft.y - normalizedQrIdeal.topLeft.y

        // Step 3: Scale page template by QR dimensions
        val scaledPageIdeal = RocketBoundingBox(
            topLeft = PointF(pageBoxIdeal.topLeft.x * qrWidth, pageBoxIdeal.topLeft.y * qrHeight),
            topRight = PointF(
                pageBoxIdeal.topRight.x * qrWidth,
                pageBoxIdeal.topRight.y * qrHeight
            ),
            bottomRight = PointF(
                pageBoxIdeal.bottomRight.x * qrWidth,
                pageBoxIdeal.bottomRight.y * qrHeight
            ),
            bottomLeft = PointF(
                pageBoxIdeal.bottomLeft.x * qrWidth,
                pageBoxIdeal.bottomLeft.y * qrHeight
            )
        )

        // Step 4: Create transform from normalized ideal QR to actual QR (keeps position!)
        val matrix = Matrix()
        matrix.setPolyToPoly(
            normalizedQrIdeal.toFloatArray(), 0,
            qrBoxActual.toFloatArray(), 0,     // DON'T normalize this!
            4
        )

        // Step 5: Apply transform to scaled page
        val transformedPage = FloatArray(8)
        matrix.mapPoints(transformedPage, scaledPageIdeal.toFloatArray())

        return RocketBoundingBox(transformedPage)
    }
}