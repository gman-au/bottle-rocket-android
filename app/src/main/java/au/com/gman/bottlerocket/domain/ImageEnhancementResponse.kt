package au.com.gman.bottlerocket.domain

import android.graphics.Bitmap

data class ImageEnhancementResponse(
    val bitmap: Bitmap,
    val scaledQrBox: RocketBoundingBox?
)