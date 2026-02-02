package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.CaptureDetectionResult
import com.google.mlkit.vision.barcode.common.Barcode
import org.opencv.core.Mat

interface IQrCodeHandler {
    fun handle(
        barcode: Barcode?,
        mat: Mat,
        sourceWidth: Int,
        sourceHeight: Int
    ): CaptureDetectionResult
}