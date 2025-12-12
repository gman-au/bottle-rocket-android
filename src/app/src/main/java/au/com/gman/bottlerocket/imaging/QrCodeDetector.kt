package au.com.gman.bottlerocket.imaging

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.interfaces.IQrCodeDetector
import au.com.gman.bottlerocket.interfaces.ITemplateListener
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class QrCodeDetector @Inject constructor(
    private val qrCodeMatcher: QrCodeTemplateMatcher
): IQrCodeDetector {

    private val scanner = BarcodeScanning.getClient()

    private var listener: ITemplateListener? = null

    override fun setListener(listener: ITemplateListener) {
        this.listener = listener
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val qrCode = barcodes.firstOrNull()
                    val matchedTemplate = qrCodeMatcher.tryMatch(qrCode?.rawValue ?: "")
                    listener?.onDetectionSuccess(matchedTemplate)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

}