package au.com.gman.bottlerocket.qrCode

import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.extensions.aggressiveSmooth
import au.com.gman.bottlerocket.extensions.calculateRotationAngle
import au.com.gman.bottlerocket.extensions.isOutOfBounds
import au.com.gman.bottlerocket.extensions.round
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IRocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.barcode.common.Barcode
import org.opencv.core.Mat
import javax.inject.Inject

class QrCodeHandler @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val pageTemplateRescaler: IPageTemplateRescaler,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
    private val rocketBoundingBoxMedianFilter: IRocketBoundingBoxMedianFilter,
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
        val codeFound = true
        var matchFound = false
        var outOfBounds = false
        var pageBoundingBox: RocketBoundingBox? = null
        var pageBoundingBoxUnscaled: RocketBoundingBox? = null
        var qrCornerPointsBoxUnscaled: RocketBoundingBox? = null
        var qrCornerPointsBoxScaled: RocketBoundingBox? = null
        var qrCodeValue: String? = null
        var cameraRotation: Float = 0F
        var boundingBoxRotation: Float = 0F
        var scalingFactorViewport: ScaleAndOffset? = null

        val pageTemplate = qrCodeTemplateMatcher.tryMatch(barcode?.rawValue ?: "")

        if (barcode != null) {
            qrCodeValue = barcode.rawValue
            Log.d(TAG, "QR code value found: $qrCodeValue")

            val qrCornerPoints = RocketBoundingBox(barcode.cornerPoints)
            qrCornerPointsBoxUnscaled = qrCornerPoints

            if (!screenDimensions.isInitialised())
                throw IllegalStateException("Screen dimensions not initialised")

            screenDimensions
                .recalculateScalingFactorIfRequired()

            scalingFactorViewport =
                screenDimensions
                    .getScalingFactor()

            boundingBoxRotation =
                qrCornerPointsBoxUnscaled
                    .calculateRotationAngle()

            cameraRotation =
                screenDimensions
                    .getScreenRotation()

            if (scalingFactorViewport != null) {

                qrCornerPointsBoxScaled =
                    qrCornerPointsBoxUnscaled
                        .scaleUpWithOffset(scalingFactorViewport)

                if (screenDimensions.getTargetSize() == null)
                    throw IllegalStateException("Screen dimensions not initialised")

                // any qr point outside viewport should be out of bounds
                outOfBounds =
                    qrCornerPointsBoxScaled
                        .isOutOfBounds(screenDimensions.getTargetSize()!!)

                if (pageTemplate != null) {

                    // Calculate page bounds in UNSCALED (ImageAnalysis) space
                    val rawPageBounds =
                        pageTemplateRescaler
                            .calculatePageBoundsFromTemplate(
                                qrCornerPointsBoxUnscaled,
                                RocketBoundingBox(pageTemplate.pageDimensions)
                            )

                    // Store the unscaled version
                    pageBoundingBoxUnscaled = rawPageBounds

                    // openCV edge detection
                    val detectedEdges =
                        edgeDetector
                            .detectEdges(mat)
                    if (detectedEdges?.size == 4) {
                        Log.d(TAG, "Raw detected edges: $detectedEdges")
                        Log.d(TAG, "Mat dimensions: ${mat.width()} x ${mat.height()}")
                        Log.d(TAG, "Source dimensions: $sourceWidth x $sourceHeight")

                        val newPoints =
                            orderPointsClockwise(
                                detectedEdges
                                    .take(4)
                                    .map { o -> PointF(o.x.toFloat(), o.y.toFloat()) }
                            )

                        Log.d(TAG, "Ordered points: ${newPoints.contentToString()}")

                        pageBoundingBox =
                            RocketBoundingBox(newPoints)
                                //.scaleUpWithOffset(scalingFactorViewport)

                        Log.d(TAG, "Edge detection - unscaled: $pageBoundingBoxUnscaled")
                        Log.d(TAG, "Edge detection - scaled: $pageBoundingBox")
                    } else {
                        // Scale for preview display
                        val scaledPageBounds =
                            rawPageBounds
                                .scaleUpWithOffset(scalingFactorViewport)

                        // Apply smoothing to the SCALED version (for preview)
                        pageBoundingBox =
                            scaledPageBounds
                                .aggressiveSmooth(
                                    previous = previousPageBounds,
                                    smoothFactor = 0.3f,
                                    maxJumpThreshold = 50f
                                )
                    }

                    // any qr point outside viewport should be out of bounds
                    outOfBounds =
                        outOfBounds ||
                                pageBoundingBox
                                    .isOutOfBounds(screenDimensions.getTargetSize()!!)

                    pageBoundingBox =
                        rocketBoundingBoxMedianFilter
                            .add(pageBoundingBox)

                    previousPageBounds = pageBoundingBox

                    matchFound = true

                    Log.d(
                        TAG,
                        "Unscaled page bounds (ImageAnalysis space): $pageBoundingBoxUnscaled"
                    )
                    Log.d(TAG, "Scaled page bounds (Preview space): $pageBoundingBox")
                } else {
                    previousPageBounds = null
                    rocketBoundingBoxMedianFilter.reset()
                }
            } else {
                previousPageBounds = null
                rocketBoundingBoxMedianFilter.reset()
            }
        } else {
            previousPageBounds = null
            rocketBoundingBoxMedianFilter.reset()
        }

        return BarcodeDetectionResult(
            codeFound = codeFound,
            matchFound = matchFound,
            outOfBounds = false,// outOfBounds,
            qrCode = qrCodeValue,
            pageTemplate = pageTemplate,
            pageOverlayPath = pageBoundingBoxUnscaled?.round(),
            pageOverlayPathPreview = pageBoundingBox?.round(),
            qrCodeOverlayPath = qrCornerPointsBoxUnscaled?.round(),
            qrCodeOverlayPathPreview = qrCornerPointsBoxScaled?.round(),
            cameraRotation = cameraRotation,
            boundingBoxRotation = boundingBoxRotation,
            scalingFactor = scalingFactorViewport,
            sourceImageWidth = sourceWidth,
            sourceImageHeight = sourceHeight
        )
    }

    private fun orderPointsClockwise(points: List<PointF>): Array<PointF> {
        // Sort by y coordinate to get top and bottom pairs
        val sorted = points.sortedBy { it.y }

        val top = sorted.take(2).sortedBy { it.x }  // Top two, left to right
        val bottom = sorted.takeLast(2).sortedBy { it.x }  // Bottom two, left to right

        return arrayOf(
            top[0],      // topLeft
            top[1],      // topRight
            bottom[1],   // bottomRight
            bottom[0]    // bottomLeft
        )
    }
}