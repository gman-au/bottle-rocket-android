package au.com.gman.bottlerocket.interfaces

import android.graphics.Bitmap
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.ImageEnhancementResponse

interface IImageEnhancer {
    fun processImageWithMatchedTemplate(
        bitmap: Bitmap,
        detectionResult: BarcodeDetectionResult
    ): ImageEnhancementResponse?
}