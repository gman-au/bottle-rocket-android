package au.com.gman.bottlerocket.domain

data class IndicatorBox (
    val status: CaptureStatusEnum,
    val box: RocketBoundingBox?
)