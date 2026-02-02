package au.com.gman.bottlerocket.imaging

import android.graphics.Bitmap
import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import au.com.gman.bottlerocket.domain.ImageEnhancementResponse
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.extensions.cropToPageBounds
import au.com.gman.bottlerocket.extensions.enhanceImage
import au.com.gman.bottlerocket.extensions.matchQrToOverlayTransform
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
        detectionResult: CaptureDetectionResult
    ): ImageEnhancementResponse? {

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

        val scaleFactor =
            ScaleAndOffset(
                PointF(scaleX, scaleY),
                PointF(0F, 0F)
            )

        val qrOverlay =
            detectionResult
                .feedbackOverlayPaths
                .first()

        // Scale the overlay from ImageAnalysis coordinates to bitmap coordinates
        val scaledPageOverlay =
            detectionResult
                .pageOverlayPath
                .scaleUpWithOffset(scaleFactor)

        // Scale the QR overlay the same way
        val scaledQrOverlay =
            qrOverlay
                ?.scaleUpWithOffset(scaleFactor)

        Log.d(TAG, "Original overlay: ${detectionResult.pageOverlayPath}")
        Log.d(TAG, "Scaled overlay: $scaledPageOverlay")
        Log.d(TAG, "Original QR overlay: ${detectionResult.feedbackOverlayPaths}")
        Log.d(TAG, "Scaled QR overlay: $scaledQrOverlay")

        val enhancedBitmap =
            rotatedBitmap
                .enhanceImage()
        //finalQrOverlay,
        //finalOverlay

        val qrBoxTransformed =
            scaledQrOverlay
                ?.matchQrToOverlayTransform(scaledPageOverlay)

        val croppedBitmap =
            enhancedBitmap
                .cropToPageBounds(scaledPageOverlay)

        return ImageEnhancementResponse(
            bitmap = croppedBitmap,
            scaledQrBox = qrBoxTransformed
        )
    }
}