package au.com.gman.bottlerocket.qrCode

import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.extensions.aggressiveSmooth
import au.com.gman.bottlerocket.extensions.createFallbackSquare
import au.com.gman.bottlerocket.extensions.orderPointsClockwise
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.injection.TheContourPointDetector
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
    @TheContourPointDetector private val edgeDetector: IEdgeDetector
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
    ): CaptureDetectionResult {
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

        val qrBoundingBoxList: MutableList<RocketBoundingBox?> = mutableListOf()
        val qrBoundingPreviewBoxList: MutableList<RocketBoundingBox?> = mutableListOf()

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

                // Get QR code bounding box in camera space
                qrCornerPoints.let { points ->
                    qrBoundingBoxCamera = RocketBoundingBox(points)

                    qrBoundingBoxPreview =
                        qrBoundingBoxCamera
                            .scaleUpWithOffset(scalingFactor)

                    Log.d(TAG, "QR camera: $qrBoundingBoxCamera")
                    Log.d(TAG, "QR preview: $qrBoundingBoxPreview")
                }

                cameraRotation =
                    screenDimensions
                        .getScreenRotation()

                qrBoundingBoxPreview =
                    qrBoundingBoxCamera!!
                        .scaleUpWithOffset(scalingFactor)

                qrBoundingBoxList.add(qrBoundingBoxCamera)
                qrBoundingPreviewBoxList.add(qrBoundingBoxPreview)

                if (pageTemplate != null) {

                    // openCV edge detection
                    val detectedEdges =
                        edgeDetector
                            .detectEdges(mat, 4)

                    if (detectedEdges?.size == 4) {
                        Log.d(TAG, "Raw edges: $detectedEdges")

                        val orderedPoints = (
                                detectedEdges.map { PointF(it.x.toFloat(), it.y.toFloat()) }
                                ).orderPointsClockwise()

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
                        } else {
                            matchFound = true
                            outOfBounds = true
                            pageBoundingBoxPreview = targetSize.createFallbackSquare()
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

        return CaptureDetectionResult(
            codeFound = codeFound,
            matchFound = matchFound,
            outOfBounds = outOfBounds,
            qrCode = qrCodeValue,
            pageTemplate = pageTemplate,
            pageOverlayPath = pageBoundingBoxCamera,
            feedbackOverlayPaths = qrBoundingBoxList,
            pageOverlayPathPreview = pageBoundingBoxPreview,
            feedbackOverlayPathsPreview = qrBoundingPreviewBoxList,
            cameraRotation = cameraRotation,
            boundingBoxRotation = 0F,
            scalingFactor = scalingFactor,
            sourceImageWidth = sourceWidth,
            sourceImageHeight = sourceHeight
        )
    }
}