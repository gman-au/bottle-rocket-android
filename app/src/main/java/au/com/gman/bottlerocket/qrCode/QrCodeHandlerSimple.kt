package au.com.gman.bottlerocket.qrCode

import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.barcode.common.Barcode
import org.opencv.core.Mat
import javax.inject.Inject

class QrCodeHandlerSimple @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
    private val edgeDetector: IEdgeDetector
) : IQrCodeHandler {

    companion object {
        private const val TAG = "QrCodeHandlerSimple"
    }

    override fun handle(
        barcode: Barcode?,
        mat: Mat,
        sourceWidth: Int,
        sourceHeight: Int
    ): BarcodeDetectionResult {

        var matchFound = false
        var pageBoundingBoxCamera: RocketBoundingBox? = null
        var pageBoundingBoxPreview: RocketBoundingBox? = null
        var qrBoundingBoxCamera: RocketBoundingBox? = null
        var qrBoundingBoxPreview: RocketBoundingBox? = null
        val qrCodeValue = barcode?.rawValue
        var pageTemplate = qrCodeTemplateMatcher.tryMatch(barcode?.rawValue ?: "")

        Log.d(TAG, "Source: ${sourceWidth}x${sourceHeight}, Mat: ${mat.width()}x${mat.height()}")

        // Ensure screen dimensions are initialized
        if (!screenDimensions.isInitialised()) {
            Log.w(TAG, "Screen dimensions not initialized")
            return createEmptyResult(barcode, sourceWidth, sourceHeight)
        }

        screenDimensions.recalculateScalingFactorIfRequired()
        val scalingFactor = screenDimensions.getScalingFactor()

        if (scalingFactor == null) {
            Log.w(TAG, "No scaling factor available")
            return createEmptyResult(barcode, sourceWidth, sourceHeight)
        }

        Log.d(TAG, "Scaling factor: $scalingFactor")

        // Process QR code
        if (barcode != null) {
            pageTemplate = qrCodeTemplateMatcher.tryMatch(qrCodeValue ?: "")

            // Get QR code bounding box in camera space
            barcode.cornerPoints?.let { points ->
                qrBoundingBoxCamera = RocketBoundingBox(points)
                qrBoundingBoxPreview = qrBoundingBoxCamera?.scaleUpWithOffset(scalingFactor)

                Log.d(TAG, "QR camera: $qrBoundingBoxCamera")
                Log.d(TAG, "QR preview: $qrBoundingBoxPreview")
            }

            // Try edge detection
            if (pageTemplate != null) {
                val detectedEdges = edgeDetector.detectEdges(mat)

                if (detectedEdges?.size == 4) {
                    Log.d(TAG, "Raw edges: $detectedEdges")

                    val orderedPoints = orderPointsClockwise(
                        detectedEdges.map { PointF(it.x.toFloat(), it.y.toFloat()) }
                    )

                    // Camera space (Mat coordinates)
                    pageBoundingBoxCamera = RocketBoundingBox(orderedPoints)

                    // Preview space (scaled for display)
                    pageBoundingBoxPreview = pageBoundingBoxCamera.scaleUpWithOffset(scalingFactor)

                    matchFound = true

                    Log.d(TAG, "Page camera: $pageBoundingBoxCamera")
                    Log.d(TAG, "Page preview: $pageBoundingBoxPreview")
                }
            }
        }

        return BarcodeDetectionResult(
            codeFound = barcode != null,
            matchFound = matchFound,
            outOfBounds = false,
            qrCode = qrCodeValue,
            pageTemplate = pageTemplate,
            pageOverlayPath = pageBoundingBoxCamera,
            qrCodeOverlayPath = qrBoundingBoxCamera,
            pageOverlayPathPreview = pageBoundingBoxPreview,
            qrCodeOverlayPathPreview = qrBoundingBoxPreview,
            cameraRotation = screenDimensions.getScreenRotation(),
            boundingBoxRotation = 0f,
            scalingFactor = scalingFactor,
            sourceImageWidth = sourceWidth,
            sourceImageHeight = sourceHeight
        )
    }

    private fun createEmptyResult(barcode: Barcode?, sourceWidth: Int, sourceHeight: Int) =
        BarcodeDetectionResult(
            codeFound = barcode != null,
            matchFound = false,
            outOfBounds = false,
            qrCode = barcode?.rawValue,
            pageTemplate = null,
            pageOverlayPath = null,
            qrCodeOverlayPath = null,
            pageOverlayPathPreview = null,
            qrCodeOverlayPathPreview = null,
            cameraRotation = 0f,
            boundingBoxRotation = 0f,
            scalingFactor = null,
            sourceImageWidth = sourceWidth,
            sourceImageHeight = sourceHeight
        )

    private fun orderPointsClockwise(points: List<PointF>): Array<PointF> {
        val sorted = points.sortedBy { it.y }
        val top = sorted.take(2).sortedBy { it.x }
        val bottom = sorted.takeLast(2).sortedBy { it.x }

        return arrayOf(
            top[0],      // topLeft
            top[1],      // topRight
            bottom[1],   // bottomRight
            bottom[0]    // bottomLeft
        )
    }
}