package au.com.gman.bottlerocket.scanning

import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import au.com.gman.bottlerocket.extensions.toMat
import au.com.gman.bottlerocket.interfaces.ICaptureArtifactDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class BarcodeDetector @Inject constructor(
    private val qrCodeHandler: IQrCodeHandler
) : ICaptureArtifactDetector {

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

    companion object {
        private const val TAG = "BarcodeDetector"
    }

    override fun capture( imageProxy: ImageProxy,
                          image: InputImage,
                          rotationDegrees: Int,
                          imageWidth: Int,
                          imageHeight: Int
    ): CaptureDetectionResult? {

        var barcodeDetectionResult: CaptureDetectionResult? = null

        try {
            val barcodes = Tasks.await(scanner.process(image))

            val barcode = barcodes.firstOrNull()
            val mat = imageProxy.toMat(image, rotationDegrees)!!

            barcodeDetectionResult =
                qrCodeHandler
                    .handle(
                        barcode,
                        mat,
                        imageWidth,
                        imageHeight
                    )

        } catch (e: Exception) {
            // handle cancellation / execution exceptions
        } finally {
            imageProxy.close()
        }

        return barcodeDetectionResult
    }
}