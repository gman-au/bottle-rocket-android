package au.com.gman.bottlerocket.domain

data class CaptureDetectionResult(
    val claimed: Boolean,
    val codeFound: Boolean,
    val matchFound: Boolean,
    val outOfBounds: Boolean,
    val qrCode: String?,
    val vendor: String?,
    val pageOverlayPath: RocketBoundingBox?,
    val feedbackOverlayPaths: List<RocketBoundingBox?>,
    val pageOverlayPathPreview: RocketBoundingBox?,
    val indicatorBoxesPreview: List<IndicatorBox?>,
    val cameraRotation: Float,
    val boundingBoxRotation: Float,
    val scalingFactor: ScaleAndOffset?,
    val sourceImageWidth: Int,
    val sourceImageHeight: Int
) {
    companion object {
        // Creates a reusable, single instance for the empty state
        val EMPTY = CaptureDetectionResult(
            claimed = false,
            codeFound = false,
            matchFound = false,
            outOfBounds = false,
            qrCode = null,
            vendor = null,
            pageOverlayPath = null,
            feedbackOverlayPaths = emptyList(),
            pageOverlayPathPreview = null,
            indicatorBoxesPreview = emptyList(),
            cameraRotation = 0F,
            boundingBoxRotation = 0F,
            scalingFactor = null,
            sourceImageWidth = 0,
            sourceImageHeight = 0
        )
    }
}