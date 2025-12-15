package au.com.gman.bottlerocket.domain

data class BarcodeDetectionResult(
    val matchFound: Boolean,
    val qrCode: String?,
    val pageTemplate: PageTemplate?,
    val pageOverlayPath: RocketBoundingBox?,
    val qrCodeOverlayPath: RocketBoundingBox?
)