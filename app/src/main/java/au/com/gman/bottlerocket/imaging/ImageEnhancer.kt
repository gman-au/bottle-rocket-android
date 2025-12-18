package au.com.gman.bottlerocket.imaging

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.extensions.applyRotation
import au.com.gman.bottlerocket.extensions.enhanceImage
import au.com.gman.bottlerocket.extensions.rotate
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.interfaces.IImageEnhancer
import javax.inject.Inject

class ImageEnhancer @Inject constructor() : IImageEnhancer {

    companion object {
        private const val TAG = "ImageEnhancer"
    }

    override fun processImageWithMatchedTemplate(
        bitmap: Bitmap,
        detectionResult: BarcodeDetectionResult
    ): Bitmap? {

        if (!detectionResult.matchFound ||
            detectionResult.pageOverlayPath == null ||
            detectionResult.pageTemplate == null
        ) {
            return null
        }

        // Rotate the bitmap first
        val rotatedBitmap =
            bitmap
                .rotate(detectionResult.cameraRotation)

        // Get ImageAnalysis dimensions from the detection result
        var imageAnalysisWidth = detectionResult.sourceImageWidth.toFloat()
        var imageAnalysisHeight = detectionResult.sourceImageHeight.toFloat()

        val rotation = detectionResult.cameraRotation.toInt()
        if (rotation == 90 || rotation == 270) {
            val temp = imageAnalysisWidth
            imageAnalysisWidth = imageAnalysisHeight
            imageAnalysisHeight = temp
        }

        Log.d(
            TAG,
            "ImageAnalysis dimensions (after rotation adjustment): ${imageAnalysisWidth}x${imageAnalysisHeight}"
        )
        Log.d(TAG, "Rotated bitmap: ${rotatedBitmap.width}x${rotatedBitmap.height}")
        Log.d(TAG, "Camera rotation: ${detectionResult.cameraRotation}")

        // Calculate scaling factors from ImageAnalysis coordinates to bitmap coordinates
        val scaleX = rotatedBitmap.width.toFloat() / imageAnalysisWidth
        val scaleY = rotatedBitmap.height.toFloat() / imageAnalysisHeight

        Log.d(TAG, "Scale factors: X=$scaleX, Y=$scaleY")

        // Scale the overlay from ImageAnalysis coordinates to bitmap coordinates
        val scaledOverlay = detectionResult.pageOverlayPath.scaleUpWithOffset(
            ScaleAndOffset(
                PointF(scaleX, scaleY),
                PointF(0F, 0F)
            )
        )

        val scaledQrOverlay = detectionResult.qrCodeOverlayPath?.scaleUpWithOffset(
            ScaleAndOffset(
                PointF(scaleX, scaleY),
                PointF(0F, 0F)
            )
        )

        Log.d(TAG, "Original overlay: ${detectionResult.pageOverlayPath}")
        Log.d(TAG, "Scaled overlay: $scaledOverlay")

        // Apply rotation if needed
        val finalOverlay = if (detectionResult.boundingBoxRotation != 0f) {
            scaledOverlay.applyRotation(
                -detectionResult.boundingBoxRotation,
                PointF(
                    rotatedBitmap.width.toFloat() / 2f,
                    rotatedBitmap.height.toFloat() / 2f
                )
            )
        } else {
            scaledOverlay
        }

        val finalQrOverlay = if (detectionResult.boundingBoxRotation != 0f) {
            scaledQrOverlay?.applyRotation(
                -detectionResult.boundingBoxRotation,
                PointF(
                    rotatedBitmap.width.toFloat() / 2f,
                    rotatedBitmap.height.toFloat() / 2f
                )
            )
        } else {
            scaledQrOverlay
        }

        val enhancedBitmap =
            rotatedBitmap
                .enhanceImage()
                    //finalQrOverlay,
                    //finalOverlay

        return enhancedBitmap
    }
}