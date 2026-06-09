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
import au.com.gman.bottlerocket.extensions.createFallbackSquare
import au.com.gman.bottlerocket.extensions.orderBoxesClockwise
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.extensions.toMat
import au.com.gman.bottlerocket.interfaces.ICaptureArtifactDetector
import au.com.gman.bottlerocket.interfaces.IRocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.IScribzeeMarkerDetector
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class ScribzeeDetector @Inject constructor(
    private val scribzeeMarkerDetector: IScribzeeMarkerDetector,
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
        var qrIndicatorStatus: CaptureStatusEnum

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

            scribzeeMarkers.map { box ->
                indicatorBox =
                    box
                        .scaleUpWithOffset(scalingFactor)

                scribzeeIndicatorBoxes.add(indicatorBox)
            }

            if (scribzeeMarkers.size == 4) {
                Log.d(TAG, "Markers found: $scribzeeMarkers")
                qrIndicatorStatus = CaptureStatusEnum.CAPTURING

                val orderedMarkers =
                    scribzeeMarkers
                        .orderBoxesClockwise()

                val orderedPoints: Array<PointF> = arrayOf(
                    PointF(orderedMarkers[0].bottomRight.x, orderedMarkers[0].bottomRight.y),
                    PointF(orderedMarkers[1].bottomLeft.x, orderedMarkers[1].bottomLeft.y),
                    PointF(orderedMarkers[2].topLeft.x, orderedMarkers[2].topLeft.y),
                    PointF(orderedMarkers[3].topRight.x, orderedMarkers[3].topRight.y)
                )

                // Camera space (Mat coordinates)
                pageBoundingBoxCamera =
                    RocketBoundingBox(orderedPoints)

                val hasRightAngles = hasRightAngles(
                    orderedPoints[0],
                    orderedPoints[1],
                    orderedPoints[2],
                    orderedPoints[3]
                )



                if (hasRightAngles) {

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

                    previousPageBounds = null
                    rocketBoundingBoxMedianFilter.reset()
                }
                else {
                    previousPageBounds = null
                    rocketBoundingBoxMedianFilter.reset()
                }
            } else {
                previousPageBounds = null
                rocketBoundingBoxMedianFilter.reset()
                if (scribzeeMarkers.isNotEmpty()) {
                    outOfBounds = true
                    pageBoundingBoxPreview = targetSize.createFallbackSquare()
                }
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

    private fun hasRightAngles(
        tl: PointF, tr: PointF,
        br: PointF, bl: PointF,
        toleranceDegrees: Float = 10f
    ): Boolean {
        Log.d(TAG, "hasRightAngles: tl=$tl tr=$tr bl=$bl br=$br")

        // sanity check — TL should have smallest x+y sum
        // TR should have largest x, smallest y
        // BL should have smallest x, largest y
        // BR should have largest x+y sum
        Log.d(TAG, "hasRightAngles: tl x<tr.x=${tl.x < tr.x} tl.y<bl.y=${tl.y < bl.y}")
        Log.d(TAG, "hasRightAngles: tr x>tl.x=${tr.x > tl.x} tr.y<br.y=${tr.y < br.y}")
        Log.d(TAG, "hasRightAngles: bl x<br.x=${bl.x < br.x} bl.y>tl.y=${bl.y > tl.y}")
        Log.d(TAG, "hasRightAngles: br x>bl.x=${br.x > bl.x} br.y>tr.y=${br.y > tr.y}")

        val corners = listOf(
            Triple(tr, tl, bl), // angle at TL — arms go to TR and BL
            Triple(tl, tr, br), // angle at TR — arms go to TL and BR
            Triple(tl, bl, br), // angle at BL — arms go to TL and BR
            Triple(bl, br, tr)  // angle at BR — arms go to BL and TR
        )

        return corners.all { (a, vertex, c) ->
            val angle = angleDegrees(a, vertex, c)
            Log.d(TAG, "hasRightAngles: ${vertex} angle: $angle")
            kotlin.math.abs(angle - 90f) <= toleranceDegrees
        }
    }

    private fun angleDegrees(a: PointF, vertex: PointF, c: PointF): Float {
        val v1x = a.x - vertex.x
        val v1y = a.y - vertex.y
        val v2x = c.x - vertex.x
        val v2y = c.y - vertex.y

        val dot = v1x * v2x + v1y * v2y
        val mag1 = Math.sqrt((v1x * v1x + v1y * v1y).toDouble()).toFloat()
        val mag2 = Math.sqrt((v2x * v2x + v2y * v2y).toDouble()).toFloat()

        if (mag1 == 0f || mag2 == 0f) return 0f

        val cosAngle = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
        return Math.toDegrees(Math.acos(cosAngle.toDouble())).toFloat()
    }
}