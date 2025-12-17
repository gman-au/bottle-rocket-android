package au.com.gman.bottlerocket.domain

data class BarcodeDetectionResult(
    val matchFound: Boolean,
    val qrCode: String?,
    val pageTemplate: PageTemplate?,
    val pageOverlayPath: RocketBoundingBox?,
    val qrCodeOverlayPath: RocketBoundingBox?,
    val validationMessage: String?,
    val cameraRotation: Float,
    val boundingBoxRotation: Float,
    val scalingFactor: ScaleAndOffset?
)