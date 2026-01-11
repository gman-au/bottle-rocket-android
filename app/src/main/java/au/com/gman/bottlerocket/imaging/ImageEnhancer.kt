package au.com.gman.bottlerocket.imaging

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.ImageEnhancementResponse
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.extensions.cropToPageBounds
import au.com.gman.bottlerocket.extensions.enhanceImage
import au.com.gman.bottlerocket.extensions.rotate
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.interfaces.IImageEnhancer
import javax.inject.Inject
import kotlin.math.sqrt

class ImageEnhancer @Inject constructor() : IImageEnhancer {

    companion object {
        private const val TAG = "ImageEnhancer"
    }

    override fun processImageWithMatchedTemplate(
        bitmap: Bitmap,
        detectionResult: BarcodeDetectionResult
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

        // Scale the overlay from ImageAnalysis coordinates to bitmap coordinates
        val scaledPageOverlay =
            detectionResult
                .pageOverlayPath
                .scaleUpWithOffset(scaleFactor)

        // Scale the QR overlay the same way
        val scaledQrOverlay =
            detectionResult
                .qrCodeOverlayPath
                ?.scaleUpWithOffset(scaleFactor)

        Log.d(TAG, "Original overlay: ${detectionResult.pageOverlayPath}")
        Log.d(TAG, "Scaled overlay: $scaledPageOverlay")
        Log.d(TAG, "Original QR overlay: ${detectionResult.qrCodeOverlayPath}")
        Log.d(TAG, "Scaled QR overlay: $scaledQrOverlay")

        val enhancedBitmap =
            rotatedBitmap
                .enhanceImage()
        //finalQrOverlay,
        //finalOverlay

        /// begin qr transform
        // Calculate the transformation matrix (same as cropToPageBounds)
        val topWidth = distance(scaledPageOverlay.topLeft, scaledPageOverlay.topRight)
        val bottomWidth = distance(scaledPageOverlay.bottomLeft, scaledPageOverlay.bottomRight)
        val leftHeight = distance(scaledPageOverlay.topLeft, scaledPageOverlay.bottomLeft)
        val rightHeight = distance(scaledPageOverlay.topRight, scaledPageOverlay.bottomRight)

        val avgWidth = (topWidth + bottomWidth) / 2f
        val avgHeight = (leftHeight + rightHeight) / 2f

        val maxDimension = 2048
        val scale = if (avgWidth > avgHeight) {
            maxDimension / avgWidth
        } else {
            maxDimension / avgHeight
        }

        val outputWidth = (avgWidth * scale).toInt()
        val outputHeight = (avgHeight * scale).toInt()

        val srcCorners = floatArrayOf(
            scaledPageOverlay.topLeft.x, scaledPageOverlay.topLeft.y,
            scaledPageOverlay.topRight.x, scaledPageOverlay.topRight.y,
            scaledPageOverlay.bottomRight.x, scaledPageOverlay.bottomRight.y,
            scaledPageOverlay.bottomLeft.x, scaledPageOverlay.bottomLeft.y
        )

        val dstCorners = floatArrayOf(
            0f, 0f,
            outputWidth.toFloat(), 0f,
            outputWidth.toFloat(), outputHeight.toFloat(),
            0f, outputHeight.toFloat()
        )

        val transformMatrix = Matrix()
        transformMatrix.setPolyToPoly(srcCorners, 0, dstCorners, 0, 4)

        // Transform the QR box using the same matrix
        val qrBoxTransformed = scaledQrOverlay?.let { qrBox ->
            val points = floatArrayOf(
                qrBox.topLeft.x, qrBox.topLeft.y,
                qrBox.topRight.x, qrBox.topRight.y,
                qrBox.bottomRight.x, qrBox.bottomRight.y,
                qrBox.bottomLeft.x, qrBox.bottomLeft.y
            )
            transformMatrix.mapPoints(points)

            RocketBoundingBox(
                topLeft = PointF(points[0], points[1]),
                topRight = PointF(points[2], points[3]),
                bottomRight = PointF(points[4], points[5]),
                bottomLeft = PointF(points[6], points[7])
            )
        }
        /// end qr transform

        val croppedBitmap =
            enhancedBitmap
                .cropToPageBounds(scaledPageOverlay)

        return ImageEnhancementResponse(
            bitmap = croppedBitmap,
            scaledQrBox = qrBoxTransformed
        )
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }
}