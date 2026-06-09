package au.com.gman.bottlerocket.scanning

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import au.com.gman.bottlerocket.domain.CaptureStatusEnum
import au.com.gman.bottlerocket.domain.IndicatorBox
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.extensions.aggressiveSmooth
import au.com.gman.bottlerocket.extensions.orderPointsClockwise
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.extensions.toMat
import au.com.gman.bottlerocket.interfaces.ICaptureArtifactDetector
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import au.com.gman.bottlerocket.interfaces.IRocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.IScribzeeMarkerDetector
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class ScribzeeDetector @Inject constructor(
    private val edgeDetector: IEdgeDetector,
    private val scribzeeMarkerDetector : IScribzeeMarkerDetector,
    private val rocketBoundingBoxMedianFilter: IRocketBoundingBoxMedianFilter,
    private val screenDimensions: IScreenDimensions,
) : ICaptureArtifactDetector {

    private var previousPageBounds: RocketBoundingBox? = null

    companion object {
        private const val TAG = "ScribzeeDetector"

        private const val VENDOR_SCRIBZEE = "Scribzee"
    }

    override fun capture(
        imageProxy: ImageProxy,
        image: InputImage,
        rotationDegrees: Int,
        imageWidth: Int,
        imageHeight: Int
    ): CaptureDetectionResult {
        var claimed = false
        var codeFound = false
        var matchFound = false
        var outOfBounds = false
        var pageBoundingBoxPreview: RocketBoundingBox? = null
        var pageBoundingBoxCamera: RocketBoundingBox? = null
        var indicatorBox: RocketBoundingBox? = null
        var qrCodeValue: String? = null
        var cameraRotation: Float = 0F
        var scalingFactor: ScaleAndOffset? = null

        val qrBoundingBoxList: MutableList<RocketBoundingBox?> = mutableListOf()
        val scribzeeIndicatorBoxes: MutableList<RocketBoundingBox?> = mutableListOf()
        var qrIndicatorStatus: CaptureStatusEnum = CaptureStatusEnum.NOT_FOUND

        val mat =
            imageProxy
                .toMat(image, rotationDegrees)!!

        if (!screenDimensions.isInitialised())
            throw IllegalStateException("Screen dimensions not initialised")

        screenDimensions
            .recalculateScalingFactorIfRequired()

        scalingFactor =
            screenDimensions
                .getScalingFactor()

        if (scalingFactor == null) {
            return CaptureDetectionResult.EMPTY
        } else {

            if (screenDimensions.getTargetSize() == null)
                throw IllegalStateException("Screen dimensions not initialised")

            if (screenDimensions.getSourceSize() == null)
                throw IllegalStateException("Screen dimensions not initialised")

            val targetSize =
                screenDimensions
                    .getTargetSize()!!

            cameraRotation =
                screenDimensions
                    .getScreenRotation()

            qrIndicatorStatus = CaptureStatusEnum.CAPTURING

            val scribzeeMarkers =
                scribzeeMarkerDetector
                    .findScribzeeMarkers(mat, imageWidth, imageHeight)

            // Get QR code bounding box in camera space
            scribzeeMarkers.map { box ->
                indicatorBox =
                    box
                        .scaleUpWithOffset(scalingFactor)

                Log.d(TAG, "QR preview: $indicatorBox")

                scribzeeIndicatorBoxes.add(indicatorBox)
            }

            if (scribzeeMarkers.size == 4) {
                Log.d(TAG, "Markers found: $scribzeeMarkers")

                val orderedPoints = (
                        scribzeeMarkers.map { PointF(it.topLeft.x, it.topLeft.y) }
                        ).orderPointsClockwise()

                // Camera space (Mat coordinates)
                pageBoundingBoxCamera = RocketBoundingBox(orderedPoints)

                // Preview space (scaled for display)
                pageBoundingBoxPreview =
                    pageBoundingBoxCamera
                        .scaleUpWithOffset(scalingFactor!!)

                pageBoundingBoxPreview =
                    rocketBoundingBoxMedianFilter
                        .add(pageBoundingBoxPreview)

                Log.d(TAG, "Page camera: $pageBoundingBoxCamera")
                Log.d(TAG, "Page preview: $pageBoundingBoxPreview")

                previousPageBounds = pageBoundingBoxPreview

                claimed = true
                qrCodeValue = VENDOR_SCRIBZEE

                // Apply smoothing to the SCALED version (for preview)
                pageBoundingBoxPreview =
                    pageBoundingBoxPreview
                        .aggressiveSmooth(
                            previous = previousPageBounds,
                            smoothFactor = 0.3f,
                            maxJumpThreshold = 50f
                        )

                matchFound = true
                codeFound = true
                outOfBounds = false
                //pageBoundingBoxPreview = targetSize.createFallbackSquare()
                previousPageBounds = null
                rocketBoundingBoxMedianFilter.reset()
            } else {
                previousPageBounds = null
                claimed = false
                rocketBoundingBoxMedianFilter.reset()
            }

            return CaptureDetectionResult(
                claimed = claimed,
                codeFound = codeFound,
                matchFound = matchFound,
                outOfBounds = outOfBounds,
                qrCode = qrCodeValue,
                pageTemplate = null,
                pageOverlayPath = pageBoundingBoxCamera,
                feedbackOverlayPaths = qrBoundingBoxList,
                pageOverlayPathPreview = pageBoundingBoxPreview,
                indicatorBoxesPreview = scribzeeIndicatorBoxes.map {
                    IndicatorBox(qrIndicatorStatus, it)
                },
                vendor = VENDOR_SCRIBZEE,
                cameraRotation = cameraRotation,
                boundingBoxRotation = 0F,
                scalingFactor = scalingFactor,
                sourceImageWidth = imageWidth,
                sourceImageHeight = imageHeight
            )
        }
    }
}