package au.com.gman.bottlerocket.scanning

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.core.graphics.toPoint
import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.extensions.createFallbackSquare
import au.com.gman.bottlerocket.extensions.orderPointsClockwise
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.extensions.toMat
import au.com.gman.bottlerocket.injection.TheContourPointDetector
import au.com.gman.bottlerocket.interfaces.ICaptureArtifactDetector
import au.com.gman.bottlerocket.interfaces.ICaptureDetectionListener
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import au.com.gman.bottlerocket.interfaces.IRocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.common.InputImage
import org.opencv.core.Point
import org.opencv.core.Rect
import javax.inject.Inject

class CornerPointDetector @Inject constructor(
//    @TheArtifactPointDetector private val edgeDetector: IEdgeDetector,
    @TheContourPointDetector private val edgeDetector: IEdgeDetector,
    private val rocketBoundingBoxMedianFilter: IRocketBoundingBoxMedianFilter,
    private val screenDimensions: IScreenDimensions
) : ICaptureArtifactDetector {

    companion object {
        private const val CORNER_BOX_RATIO = 0.3
        private const val TAG = "CornerPointDetector"
    }

    private var listener: ICaptureDetectionListener? = null

    override fun setListener(listener: ICaptureDetectionListener) {
        this.listener = listener
    }

    private var previousPageBounds: RocketBoundingBox? = null

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {

        var quandrantBoxCameraPreview: RocketBoundingBox? = null
        var quadrantBoxCamera: RocketBoundingBox? = null

        val cornerBoundingBoxList: MutableList<RocketBoundingBox?> = mutableListOf()
        val cornerBoundingPreviewBoxList: MutableList<RocketBoundingBox?> = mutableListOf()

        val mediaImage =
            imageProxy
                .image

        if (mediaImage != null) {

            val rotationDegrees =
                imageProxy
                    .imageInfo
                    .rotationDegrees

            val image =
                InputImage
                    .fromMediaImage(
                        mediaImage,
                        rotationDegrees
                    )

            Log.d(TAG, "ImageProxy dimensions: ${imageProxy.width}x${imageProxy.height}")
            Log.d(TAG, "Rotation degrees: $rotationDegrees")

            screenDimensions
                .setScreenRotation(rotationDegrees)

            val mat = imageProxy.toMat(image, rotationDegrees)!!

            val imageWidth =
                mat
                    .width()

            val imageHeight =
                mat
                    .height()

            screenDimensions
                .setSourceSize(
                    PointF(
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                )

            if (!screenDimensions.isInitialised())
                throw IllegalStateException("Screen dimensions not initialised")

            screenDimensions
                .recalculateScalingFactorIfRequired()

            val scalingFactor =
                screenDimensions
                    .getScalingFactor()

            val cameraRotation =
                screenDimensions
                    .getScreenRotation()

            val boxWidth = (image.width * CORNER_BOX_RATIO).toInt()
            val boxHeight = (image.height * CORNER_BOX_RATIO).toInt()

            val cornerRegions = listOf(
                Rect(0, 0, boxWidth, boxHeight),
                Rect(imageWidth - boxWidth, 0, boxWidth, boxHeight),
                Rect(0, imageHeight - boxHeight, boxWidth, boxHeight),
                Rect(imageWidth - boxWidth, imageHeight - boxHeight, boxWidth, boxHeight)
            )

            var matchFound = false

            if (scalingFactor != null) {

                val detectedMarkers = mutableListOf<Point?>()

                cornerRegions.forEachIndexed { index, region ->

                    // add to feedback
                    cornerBoundingBoxList.add(RocketBoundingBox(region))
                    cornerBoundingPreviewBoxList.add(
                        RocketBoundingBox(region).scaleUpWithOffset(
                            scalingFactor
                        )
                    )

                    val subMat = mat.submat(region).clone()
                    val marker = edgeDetector.detectEdges(subMat, 4)
                    if (marker?.size == 4) {
                        // should be the center of the 4
                        val orderedPoints = (
                                marker.map { PointF(it.x.toFloat(), it.y.toFloat()) }
                                ).orderPointsClockwise()

                        // Camera space (Mat coordinates)
                        quadrantBoxCamera = RocketBoundingBox(orderedPoints)

                        // Preview space (scaled for display)
                        quandrantBoxCameraPreview =
                            quadrantBoxCamera
                                //.scaleUpWithOffset(scalingFactor)

                        detectedMarkers.add(
                            Point(
                                quandrantBoxCameraPreview.topLeft.x.toDouble(),
                                quandrantBoxCameraPreview.topLeft.y.toDouble()
                            )
                        )
                    }
                    subMat.release()
                }

                val targetSize =
                    screenDimensions
                        .getTargetSize()!!

                if (detectedMarkers.size == 4) {
                    matchFound = true

                    Log.d(TAG, "Raw detected edges from Mat:")
                    detectedMarkers.forEachIndexed { i, pt ->
                        Log.d(TAG, "  Corner $i: (${pt!!.x}, ${pt.y})")
                    }

                    val orderedPoints = (
                            detectedMarkers.map { PointF(it!!.x.toFloat(), it.y.toFloat()) }
                            ).orderPointsClockwise()

                    Log.d(TAG, "After orderPointsClockwise:")
                    orderedPoints.forEachIndexed { i, pt ->
                        Log.d(TAG, "  Corner $i: (${pt.x}, ${pt.y})")
                    }

                    // Camera space (Mat coordinates)
                    quadrantBoxCamera = RocketBoundingBox(orderedPoints)

                    Log.d(TAG, "Camera bounding box: $quadrantBoxCamera")

                    // Preview space (scaled for display)
                    quandrantBoxCameraPreview =
                        quadrantBoxCamera
                            .scaleUpWithOffset(scalingFactor)

                    quandrantBoxCameraPreview =
                        rocketBoundingBoxMedianFilter
                            .add(quandrantBoxCameraPreview)

                    Log.d(TAG, "Page camera: $quadrantBoxCamera")
                    Log.d(TAG, "Page preview: $quandrantBoxCameraPreview")

                    previousPageBounds = quandrantBoxCameraPreview

                    // Apply smoothing to the SCALED version (for preview)
                    /*pageBoundingBoxPreview =
                    pageBoundingBoxPreview
                        .aggressiveSmooth(
                            previous = previousPageBounds,
                            smoothFactor = 0.3f,
                            maxJumpThreshold = 50f
                        )*/
                } else {
                    quandrantBoxCameraPreview = targetSize.createFallbackSquare()
                }
            }

            val barcodeDetectionResult = CaptureDetectionResult(
                codeFound = true, //codeFound,
                matchFound = matchFound, //matchFound,
                outOfBounds = false, //outOfBounds,
                qrCode = null, //qrCodeValue,
                pageTemplate = null, //pageTemplate,
                pageOverlayPath = quadrantBoxCamera,
                feedbackOverlayPaths = cornerBoundingBoxList, //qrBoundingBoxCamera,
                pageOverlayPathPreview = quandrantBoxCameraPreview,
                feedbackOverlayPathsPreview = cornerBoundingPreviewBoxList, //qrBoundingBoxPreview,
                cameraRotation = cameraRotation,
                boundingBoxRotation = 0F,
                scalingFactor = scalingFactor,
                sourceImageWidth = imageWidth,
                sourceImageHeight = imageHeight
            )

            listener?.onDetectionSuccess(barcodeDetectionResult)
        }
        imageProxy
            .close()
    }
}