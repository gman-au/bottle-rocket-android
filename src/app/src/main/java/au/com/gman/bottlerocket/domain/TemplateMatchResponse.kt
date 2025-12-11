package au.com.gman.bottlerocket.domain

data class TemplateMatchResponse (
    val matchFound: Boolean,
    val qrCode: QRTemplateInfo
)