package au.com.gman.bottlerocket.imaging

import au.com.gman.bottlerocket.domain.QRTemplateInfo
import au.com.gman.bottlerocket.domain.TemplateMatchResponse
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import javax.inject.Inject

class QrCodeTemplateMatcher @Inject constructor(): IQrCodeTemplateMatcher {

    val templatesMap = mapOf(
        "04o" to QRTemplateInfo(
            position = "1",
            version = "1",
            type = "1",
            sequence = "1"
        )
    )

    override fun tryMatch(qrCode: String?): TemplateMatchResponse {        
        return if (qrCode in templatesMap) {
            TemplateMatchResponse(matchFound = true, qrCode, template = templatesMap[qrCode]!!)
        } else {
            TemplateMatchResponse(matchFound = false, qrCode, template = null)
        }
    }
}