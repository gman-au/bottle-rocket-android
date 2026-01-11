package au.com.gman.bottlerocket.qrCode

import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.extensions.aggressiveSmooth
import au.com.gman.bottlerocket.extensions.isOutOfBounds
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IQrPositionalValidator
import au.com.gman.bottlerocket.interfaces.IRocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.barcode.common.Barcode
import org.opencv.core.Mat
import javax.inject.Inject

class QrCodeHandler @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
    private val rocketBoundingBoxMedianFilter: IRocketBoundingBoxMedianFilter,
    private val qrPositionalValidator: IQrPositionalValidator,
    private val edgeDetector: IEdgeDetector
) : IQrCodeHandler {

    companion object {
        private const val TAG = "QrCodeHandler"
    }

    private var previousPageBounds: RocketBoundingBox? = null

    override fun handle(
        barcode: Barcode?,
        mat: Mat,
        sourceWidth: Int,
        sourceHeight: Int
    ): BarcodeDetectionResult {
        var codeFound = true
        var matchFound = false
        var outOfBounds = false
        var pageBoundingBoxPreview: RocketBoundingBox? = null
        var pageBoundingBoxCamera: RocketBoundingBox? = null
        var qrBoundingBoxCamera: RocketBoundingBox? = null
        var qrBoundingBoxPreview: RocketBoundingBox? = null
        var qrCodeValue: String? = null
        var cameraRotation: Float = 0F
        var scalingFactor: ScaleAndOffset? = null

        val pageTemplate = qrCodeTemplateMatcher.tryMatch(barcode?.rawValue ?: "")

        if (!screenDimensions.isInitialised())
            throw IllegalStateException("Screen dimensions not initialised")

        screenDimensions
            .recalculateScalingFactorIfRequired()

        scalingFactor =
            screenDimensions
                .getScalingFactor()

        if (scalingFactor != null) {

            if (barcode != null) {

                qrCodeValue = barcode.rawValue
                Log.d(TAG, "QR code value found: $qrCodeValue")

                val qrCornerPoints = RocketBoundingBox(barcode.cornerPoints)

                if (screenDimensions.getTargetSize() == null)
                    throw IllegalStateException("Screen dimensions not initialised")

                if (screenDimensions.getSourceSize() == null)
                    throw IllegalStateException("Screen dimensions not initialised")

                val targetSize =
                    screenDimensions
                        .getTargetSize()!!

                val sourceSize =
                    screenDimensions
                        .getSourceSize()!!

                // Get QR code bounding box in camera space
                qrCornerPoints.let { points ->
                    qrBoundingBoxCamera = RocketBoundingBox(points)

                    qrBoundingBoxPreview =
                        qrBoundingBoxCamera
                            .scaleUpWithOffset(scalingFactor)

                    // Check if QR code is out of bounds
                    /*outOfBounds =
                        qrBoundingBoxCamera
                            .isOutOfBounds(sourceSize)*/

                    Log.d(TAG, "QR camera: $qrBoundingBoxCamera")
                    Log.d(TAG, "QR preview: $qrBoundingBoxPreview")
                }

                cameraRotation =
                    screenDimensions
                        .getScreenRotation()

                qrBoundingBoxPreview =
                    qrBoundingBoxCamera!!
                        .scaleUpWithOffset(scalingFactor)

                if (pageTemplate != null) {

                    // openCV edge detection
                    val detectedEdges =
                        edgeDetector
                            .detectEdges(mat)

                    if (detectedEdges?.size == 4) {
                        Log.d(TAG, "Raw edges: $detectedEdges")

                        val orderedPoints = orderPointsClockwise(
                            detectedEdges.map { PointF(it.x.toFloat(), it.y.toFloat()) }
                        )

                        // Camera space (Mat coordinates)
                        pageBoundingBoxCamera = RocketBoundingBox(orderedPoints)

                        // Preview space (scaled for display)
                        pageBoundingBoxPreview =
                            pageBoundingBoxCamera
                                .scaleUpWithOffset(scalingFactor)

                        pageBoundingBoxPreview =
                            rocketBoundingBoxMedianFilter
                                .add(pageBoundingBoxPreview)

                        Log.d(TAG, "Page camera: $pageBoundingBoxCamera")
                        Log.d(TAG, "Page preview: $pageBoundingBoxPreview")

                        // VALIDATION 1: Check if page bounding box is out of bounds
                        val pageOutOfBounds =
                            pageBoundingBoxCamera
                                .isOutOfBounds(sourceSize)

                        /*
                        outOfBounds = outOfBounds || pageOutOfBounds
                         */

                        Log.d(TAG, "Page out of bounds: $pageOutOfBounds")

                        previousPageBounds = pageBoundingBoxPreview

                        // VALIDATION 2: Check if page bounding box is inside QR code bounding box
                        val qrInsidePage =
                            qrPositionalValidator
                                .isBoxInsideBox(qrBoundingBoxCamera, pageBoundingBoxCamera)

                        Log.d(TAG, "QR inside page: $qrInsidePage")

                        if (qrInsidePage) {
                            matchFound = true

                            // Apply smoothing to the SCALED version (for preview)
                            pageBoundingBoxPreview =
                                pageBoundingBoxPreview
                                    .aggressiveSmooth(
                                        previous = previousPageBounds,
                                        smoothFactor = 0.3f,
                                        maxJumpThreshold = 50f
                                    )
                        }
                        else {
                            matchFound = true
                            outOfBounds = true
                            pageBoundingBoxPreview = createFallbackSquare(targetSize)
                            previousPageBounds = null
                            rocketBoundingBoxMedianFilter.reset()
                        }
                    }
                } else {
                    previousPageBounds = null
                    rocketBoundingBoxMedianFilter.reset()
                }
            } else {
                previousPageBounds = null
                rocketBoundingBoxMedianFilter.reset()
            }
        } else {
            codeFound = false
            previousPageBounds = null
            rocketBoundingBoxMedianFilter.reset()
        }

        return BarcodeDetectionResult(
            codeFound = codeFound,
            matchFound = matchFound,
            outOfBounds = outOfBounds,
            qrCode = qrCodeValue,
            pageTemplate = pageTemplate,
            pageOverlayPath = pageBoundingBoxCamera,
            qrCodeOverlayPath = qrBoundingBoxCamera,
            pageOverlayPathPreview = pageBoundingBoxPreview,
            qrCodeOverlayPathPreview = qrBoundingBoxPreview,
            cameraRotation = cameraRotation,
            boundingBoxRotation = 0F,
            scalingFactor = scalingFactor,
            sourceImageWidth = sourceWidth,
            sourceImageHeight = sourceHeight
        )
    }

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

    private fun createFallbackSquare(targetSize: PointF): RocketBoundingBox {
        val centerX = targetSize.x / 2f
        val centerY = targetSize.y / 2f

        val halfSize = minOf(targetSize.x, targetSize.y) * 0.25f // 50% of viewport = 25% from center

        return RocketBoundingBox(
            topLeft = PointF(centerX - halfSize, centerY - halfSize),
            topRight = PointF(centerX + halfSize, centerY - halfSize),
            bottomRight = PointF(centerX + halfSize, centerY + halfSize),
            bottomLeft = PointF(centerX - halfSize, centerY + halfSize)
        )
    }
}