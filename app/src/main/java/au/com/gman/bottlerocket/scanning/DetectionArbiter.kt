package au.com.gman.bottlerocket.scanning

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.injection.ScanningModule
import au.com.gman.bottlerocket.interfaces.ICaptureArtifactDetector
import au.com.gman.bottlerocket.interfaces.ICaptureDetectionListener
import au.com.gman.bottlerocket.interfaces.IDetectionArbiter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class DetectionArbiter @Inject constructor(
    @ScanningModule.RocketbookBarcodeDetector private val rocketbookBarcodeDetector: ICaptureArtifactDetector,
    private val screenDimensions: IScreenDimensions
) : IDetectionArbiter {

    companion object {
        private const val TAG = "DetectionArbiter"
    }

    private var listener: ICaptureDetectionListener? = null

    override fun setListener(listener: ICaptureDetectionListener) {
        this.listener = listener
    }

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

            // TODO: iterate with claims
            val detectionResult = rocketbookBarcodeDetector.capture(imageProxy, image, rotationDegrees, imageWidth, imageHeight);

            if (detectionResult != null) {
                listener?.onDetectionSuccess(detectionResult)
            }

        } else {
            imageProxy
                .close()
        }
    }
}