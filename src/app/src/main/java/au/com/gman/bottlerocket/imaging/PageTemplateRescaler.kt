package au.com.gman.bottlerocket.imaging

import android.graphics.Matrix
import android.util.Log
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.scale
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import javax.inject.Inject

class PageTemplateRescaler @Inject constructor(): IPageTemplateRescaler {
    override fun rescalePageOverlay(
        qrCorners: RocketBoundingBox,
        pageTemplateBoundingBox: RocketBoundingBox,
        imageWidth: Float,
        imageHeight: Float,
        previewWidth: Float,
        previewHeight: Float
    ): RocketBoundingBox {

        val scaleX = previewWidth / imageWidth
        val scaleY = previewHeight / imageHeight

        Log.d("OVERLAY", "Image size: ${imageWidth}x${imageHeight}")
        Log.d("OVERLAY", "Preview size: ${previewWidth}x${previewHeight}")
        Log.d("OVERLAY", "Scale: ${scaleX}x, ${scaleY}y")

        // Calculate QR size in image space
        val qrWidth = qrCorners.topRight.x - qrCorners.topLeft.x
        val qrHeight = qrCorners.bottomLeft.y - qrCorners.topLeft.y

        Log.d("OVERLAY", "QR size (image space): ${qrWidth}x${qrHeight}")
        Log.d("OVERLAY", "QR corners (image space):\n$qrCorners")

        // Scale QR corners to preview space
        val scaledQrCorners = qrCorners.scale(scaleX, scaleY)

        Log.d("OVERLAY", "QR corners (preview space):")
        for (i in scaledQrCorners.indices step 2) {
            Log.d("OVERLAY", "  [${i/2+1}]: ${scaledQrCorners[i]}, ${scaledQrCorners[i+1]}")
        }

        // Define ideal QR square in unit coordinates (0,0 to 1,1)
        val qrSquare = floatArrayOf(
            0f, 0f,      // top-left
            1f, 0f,      // top-right
            1f, 1f,      // bottom-right
            0f, 1f       // bottom-left
        )

        // Scale page template by QR size BEFORE applying perspective
        // Template is in QR-relative units, multiply by actual QR dimensions
        val pageInQrSpace = floatArrayOf(
            pageTemplateBoundingBox.topLeft.x * qrWidth,
            pageTemplateBoundingBox.topLeft.y * qrHeight,
            pageTemplateBoundingBox.topRight.x * qrWidth,
            pageTemplateBoundingBox.topRight.y * qrHeight,
            pageTemplateBoundingBox.bottomRight.x * qrWidth,
            pageTemplateBoundingBox.bottomRight.y * qrHeight,
            pageTemplateBoundingBox.bottomLeft.x * qrWidth,
            pageTemplateBoundingBox.bottomLeft.y * qrHeight
        )

        Log.d("OVERLAY", "Page template (QR-relative):\n$pageTemplateBoundingBox")
        Log.d("OVERLAY", "Page in QR space (pixels):")
        for (i in pageInQrSpace.indices step 2) {
            Log.d("OVERLAY", "  [${i/2+1}]: ${pageInQrSpace[i]}, ${pageInQrSpace[i+1]}")
        }

        // Create perspective transform: ideal square → actual QR quadrilateral
        val matrix = Matrix()
        matrix.setPolyToPoly(
            qrSquare, 0,
            scaledQrCorners, 0,
            4
        )

        // Apply same transform to page corners
        val pageInImageSpace = FloatArray(8)
        matrix.mapPoints(pageInImageSpace, pageInQrSpace)

        Log.d("OVERLAY", "Page overlay box (preview space):")
        for (i in pageInImageSpace.indices step 2) {
            Log.d("OVERLAY", "  [${i/2+1}]: ${pageInImageSpace[i]}, ${pageInImageSpace[i+1]}")
        }

        return RocketBoundingBox(pageInImageSpace)
    }
}