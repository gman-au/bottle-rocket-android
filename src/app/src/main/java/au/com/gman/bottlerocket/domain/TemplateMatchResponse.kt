package au.com.gman.bottlerocket.domain

import android.graphics.Path

data class TemplateMatchResponse (
    val matchFound: Boolean,
    val qrCode: String?,
    val pageTemplate: PageTemplate?,
    val overlay: Path?
)