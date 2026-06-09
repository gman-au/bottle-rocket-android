package au.com.gman.bottlerocket.scanning

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import au.com.gman.bottlerocket.injection.ScanningModule
import au.com.gman.bottlerocket.interfaces.ICaptureArtifactDetector
import au.com.gman.bottlerocket.interfaces.ICaptureDetectionListener
import au.com.gman.bottlerocket.interfaces.IDetectionArbiter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class DetectionArbiter @Inject constructor(
    @ScanningModule.RocketbookBarcodeDetector private val rocketbookBarcodeDetector: ICaptureArtifactDetector,
    @ScanningModule.ScribzeeCodeDetector private val scribzeeDetector: ICaptureArtifactDetector,
    private val screenDimensions: IScreenDimensions
) : IDetectionArbiter {

    companion object {
        private const val TAG = "DetectionArbiter"
    }

    private var listener: ICaptureDetectionListener? = null

    private var currentDetectionClaimant: ICaptureArtifactDetector? = null

    override fun setListener(listener: ICaptureDetectionListener) {
        this.listener = listener
    }

    private val allDetectors: Set<ICaptureArtifactDetector> = setOf(
        rocketbookBarcodeDetector,
        scribzeeDetector
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
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

            // current non-null claimant takes precedence, otherwise check others for a claimant
            var detectionResult: CaptureDetectionResult = CaptureDetectionResult.EMPTY

            if (currentDetectionClaimant != null) {
                detectionResult =
                    currentDetectionClaimant
                        ?.capture(
                            imageProxy,
                            image,
                            rotationDegrees,
                            imageWidth,
                            imageHeight
                        )!!
            }

            if (!detectionResult.claimed) {
                val otherDetectors =
                    allDetectors
                        .filter { it != currentDetectionClaimant }

                for (detector in otherDetectors) {
                    detectionResult =
                        detector
                            .capture(
                                imageProxy,
                                image,
                                rotationDegrees,
                                imageWidth,
                                imageHeight
                            )

                    if (detectionResult.claimed) {
                        currentDetectionClaimant = detector
                        break
                    }
                }
            }

            Log.d(TAG, "Current detection claimant: ${currentDetectionClaimant?.let {it::class.toString()}}")

            // TODO: iterate with claims
            if (!detectionResult.claimed) {
                currentDetectionClaimant = null
            }

            listener?.onDetectionSuccess(detectionResult)

            imageProxy
                .close()

        } else {
            imageProxy
                .close()
        }
    }
}