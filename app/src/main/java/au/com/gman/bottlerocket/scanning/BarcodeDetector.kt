package au.com.gman.bottlerocket.scanning

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.extensions.toMat
import au.com.gman.bottlerocket.interfaces.ICaptureDetectionListener
import au.com.gman.bottlerocket.interfaces.ICaptureArtifactDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class BarcodeDetector @Inject constructor(
    private val qrCodeHandler: IQrCodeHandler,
    private val screenDimensions: IScreenDimensions
) : ICaptureArtifactDetector {

    companion object {
        private const val TAG = "BarcodeDetector"
    }

    private val scannerOptions:
            BarcodeScannerOptions =
        BarcodeScannerOptions
            .Builder()
            /*.setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC
            )*/
            .build()

    private val scanner =
        BarcodeScanning
            .getClient(scannerOptions)

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

            scanner
                .process(image)
                .addOnSuccessListener { barcodes ->
                    val barcode =
                        barcodes
                            .firstOrNull()

                    val mat = imageProxy.toMat(image, rotationDegrees)!!

                    val barcodeDetectionResult =
                        qrCodeHandler
                            .handle(
                                barcode,
                                mat,
                                imageWidth,
                                imageHeight
                            )

                    listener?.onDetectionSuccess(barcodeDetectionResult)
                }
                .addOnCompleteListener {
                    imageProxy
                        .close()
                }
        } else {
            imageProxy
                .close()
        }
    }
}