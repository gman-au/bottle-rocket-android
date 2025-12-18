package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import com.google.mlkit.vision.barcode.common.Barcode

interface IQrCodeHandler {
    fun handle(
        barcode: Barcode?,
        sourceWidth: Int,
        sourceHeight: Int
    ): BarcodeDetectionResult
}