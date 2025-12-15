package au.com.gman.bottlerocket.imaging

import android.graphics.PointF
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.interfaces.IBarcodeDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.ITemplateListener
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class BarcodeDetector @Inject constructor(
    private val qrCodeHandler: IQrCodeHandler,
    private val screenDimensions: IScreenDimensions
) : IBarcodeDetector {

    private val scanner =
        BarcodeScanning
            .getClient()

    private var listener: ITemplateListener? = null

    override fun setListener(listener: ITemplateListener) {
        this.listener = listener
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage =
            imageProxy
                .image

        if (mediaImage != null) {
            val image =
                InputImage
                    .fromMediaImage(
                        mediaImage,
                        imageProxy
                            .imageInfo
                            .rotationDegrees
                    )

            val imageWidth =
                mediaImage
                    .width

            val imageHeight =
                mediaImage
                    .height

            val rotationDegrees =
                imageProxy
                    .imageInfo
                    .rotationDegrees

            screenDimensions
                .setImageSize(
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

                    val barcodeDetectionResult =
                        qrCodeHandler
                            .handle(barcode)

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