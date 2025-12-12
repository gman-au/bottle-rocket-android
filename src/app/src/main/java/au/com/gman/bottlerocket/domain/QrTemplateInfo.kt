package au.com.gman.bottlerocket.domain

import android.graphics.Path

data class QrTemplateInfo(
    val type: String,      // "FT02", "T01", etc.
    val boundingBox: Path
)