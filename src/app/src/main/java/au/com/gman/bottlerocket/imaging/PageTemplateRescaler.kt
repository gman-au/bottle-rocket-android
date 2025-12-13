package au.com.gman.bottlerocket.imaging

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Point
import android.util.Log
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.scale
import au.com.gman.bottlerocket.domain.toPointArray
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import javax.inject.Inject
import kotlin.math.atan2

class PageTemplateRescaler @Inject constructor(): IPageTemplateRescaler {
    override fun rescalePageOverlay(
        qrCorners: RocketBoundingBox,
        pageTemplateBoundingBox: RocketBoundingBox,
        imageWidth: Float,
        imageHeight: Float,
        previewWidth: Float,
        previewHeight: Float
    ): RocketBoundingBox {

        // Calculate scale factors from image space to preview space
        val scaleX = previewWidth / imageWidth
        val scaleY = previewHeight / imageHeight

        Log.d("OVERLAY", "Scale: ${scaleX}x, ${scaleY}y")

        val qrRotationDegrees = calculateQRRotationAngle(qrCorners)
        Log.d("OVERLAY", "Rotation: $qrRotationDegrees°")

        val anchorPoint = pageTemplateBoundingBox.bottomLeft
        Log.d("OVERLAY", "Anchor (image space): (${anchorPoint.x}, ${anchorPoint.y})")

        // Scale page dimensions to match preview
        val pageCorners = pageTemplateBoundingBox.scale(scaleX, scaleY)

        val matrix = Matrix()
        matrix.setRotate(qrRotationDegrees)
        matrix.mapPoints(pageCorners)

        // Scale anchor point to preview space
        val scaledAnchorX = anchorPoint.x * scaleX
        val scaledAnchorY = anchorPoint.y * scaleY

        for (i in pageCorners.indices step 2) {
            pageCorners[i] += scaledAnchorX
            pageCorners[i + 1] += scaledAnchorY
        }

        Log.d("OVERLAY", "Final page corners (preview space):")
        for (i in pageCorners.indices step 2) {
            Log.d("OVERLAY", "  (${pageCorners[i]}, ${pageCorners[i+1]})")
        }

        return RocketBoundingBox(pageCorners)
    }

    /**
     * Calculate the rotation angle of the QR code from its corner points
     * Returns angle in degrees
     */
    private fun calculateQRRotationAngle(cornerBox: RocketBoundingBox): Float {
        val corners = cornerBox.toPointArray()

        // Use top edge (corner 0 to corner 1) to determine rotation
        val topLeft = corners[0]
        val topRight = corners[1]

        val deltaX = (topRight.x - topLeft.x)
        val deltaY = (topRight.y - topLeft.y)

        // Calculate angle in radians, then convert to degrees
        val angleRadians = atan2(deltaY, deltaX)
        val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()

        return angleDegrees
    }

}