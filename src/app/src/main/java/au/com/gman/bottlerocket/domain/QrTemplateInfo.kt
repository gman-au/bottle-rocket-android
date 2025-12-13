package au.com.gman.bottlerocket.domain

data class QrTemplateInfo(
    val type: String,      // "FT02", "T01", etc.
    val boundingBox: RocketBoundingBox
)