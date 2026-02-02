package au.com.gman.bottlerocket.domain

data class CaptureDetectionResult(
    val codeFound: Boolean,
    val matchFound: Boolean,
    val outOfBounds: Boolean,
    val qrCode: String?,
    val pageTemplate: PageTemplate?,
    val pageOverlayPath: RocketBoundingBox?,
    val feedbackOverlayPaths: List<RocketBoundingBox?>,
    val pageOverlayPathPreview: RocketBoundingBox?,
    val feedbackOverlayPathsPreview: List<RocketBoundingBox?>,
    val cameraRotation: Float,
    val boundingBoxRotation: Float,
    val scalingFactor: ScaleAndOffset?,
    val sourceImageWidth: Int,
    val sourceImageHeight: Int
)