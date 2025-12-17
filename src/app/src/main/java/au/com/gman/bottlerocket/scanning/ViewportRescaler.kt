package au.com.gman.bottlerocket.scanning

import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import javax.inject.Inject

class ViewportRescaler @Inject constructor() : IViewportRescaler {

    companion object {
        private const val TAG = "ViewportRescaler"
    }

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
            TAG,
            "First: ${actualFirstW}x${actualFirstH} (aspect: ${firstAspect})"
        )
        Log.d(
            TAG,
            "Second: ${secondWidth}x${secondHeight} (aspect: ${secondAspect})"
        )
        Log.d(TAG, "Scale: ${scale.x}, ${scale.y}")
        Log.d(TAG, "Offset: ${offset.x}, ${offset.y}")

        return ScaleAndOffset(scale, offset)
    }
}