package au.com.gman.bottlerocket.scanning

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.extensions.aggressiveSmooth
import au.com.gman.bottlerocket.extensions.createFallbackSquare
import au.com.gman.bottlerocket.extensions.orderPointsClockwise
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.extensions.toMat
import au.com.gman.bottlerocket.injection.TheArtifactPointDetector
import au.com.gman.bottlerocket.injection.TheContourPointDetector
import au.com.gman.bottlerocket.interfaces.ICaptureArtifactDetector
import au.com.gman.bottlerocket.interfaces.ICaptureDetectionListener
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import au.com.gman.bottlerocket.interfaces.IRocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class CornerPointDetector @Inject constructor(
//    @TheArtifactPointDetector private val edgeDetector: IEdgeDetector,
    @TheContourPointDetector private val edgeDetector: IEdgeDetector,
    private val rocketBoundingBoxMedianFilter: IRocketBoundingBoxMedianFilter,
    private val screenDimensions: IScreenDimensions
) : ICaptureArtifactDetector {

    companion object {
        private const val TAG = "CornerPointDetector"
    }

    private var listener: ICaptureDetectionListener? = null

    override fun setListener(listener: ICaptureDetectionListener) {
        this.listener = listener
    }

    private var previousPageBounds: RocketBoundingBox? = null

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {

        var pageBoundingBoxPreview: RocketBoundingBox? = null
        var pageBoundingBoxCamera: RocketBoundingBox? = null

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

            val imageWidth =
                imageProxy
                    .width

            val imageHeight =
                imageProxy
                    .height

            screenDimensions
                .setSourceSize(
                    PointF(
                        imageWidth.toFloat(),
                        imageHeight.toFloat()
                    )
                )

            screenDimensions
                .setScreenRotation(rotationDegrees)

            val mat = imageProxy.toMat(image, rotationDegrees)!!

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

            val detectedEdges =
                edgeDetector
                    .detectEdges(mat, 7)

            val targetSize =
                screenDimensions
                    .getTargetSize()!!

            var matchFound = false

            if (detectedEdges?.size == 7) {
                matchFound = true

                Log.d(TAG, "Raw detected edges from Mat:")
                detectedEdges.forEachIndexed { i, pt ->
                    Log.d(TAG, "  Corner $i: (${pt.x}, ${pt.y})")
                }

                val orderedPoints = (
                        detectedEdges.map { PointF(it.x.toFloat(), it.y.toFloat()) }
                        ).orderPointsClockwise()

                Log.d(TAG, "After orderPointsClockwise:")
                orderedPoints.forEachIndexed { i, pt ->
                    Log.d(TAG, "  Corner $i: (${pt.x}, ${pt.y})")
                }

                // Camera space (Mat coordinates)
                pageBoundingBoxCamera = RocketBoundingBox(orderedPoints)

                Log.d(TAG, "Camera bounding box: $pageBoundingBoxCamera")

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

                // Apply smoothing to the SCALED version (for preview)
                /*pageBoundingBoxPreview =
                    pageBoundingBoxPreview
                        .aggressiveSmooth(
                            previous = previousPageBounds,
                            smoothFactor = 0.3f,
                            maxJumpThreshold = 50f
                        )*/
            }
            else {
                pageBoundingBoxPreview = targetSize.createFallbackSquare()
            }

            val barcodeDetectionResult = CaptureDetectionResult(
                codeFound = true, //codeFound,
                matchFound = matchFound, //matchFound,
                outOfBounds = false, //outOfBounds,
                qrCode = null, //qrCodeValue,
                pageTemplate = null, //pageTemplate,
                pageOverlayPath = pageBoundingBoxCamera,
                qrCodeOverlayPath = null, //qrBoundingBoxCamera,
                pageOverlayPathPreview = pageBoundingBoxPreview,
                qrCodeOverlayPathPreview = null, //qrBoundingBoxPreview,
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