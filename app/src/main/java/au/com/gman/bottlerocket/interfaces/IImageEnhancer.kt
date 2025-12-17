package au.com.gman.bottlerocket.interfaces

import android.graphics.Bitmap
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult

interface IImageEnhancer {
    fun processImageWithMatchedTemplate(
        bitmap: Bitmap,
        detectionResult: BarcodeDetectionResult
    ): Bitmap?
}