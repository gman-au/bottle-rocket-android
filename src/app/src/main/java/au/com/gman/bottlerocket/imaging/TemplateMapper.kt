package au.com.gman.bottlerocket.imaging

import au.com.gman.bottlerocket.domain.QRTemplateInfo
import au.com.gman.bottlerocket.domain.TemplateMatchResponse
import au.com.gman.bottlerocket.interfaces.ITemplateMapper

class TemplateMapper : ITemplateMapper {
    override fun tryMatch(qrData: String): TemplateMatchResponse {
        val result = TemplateMatchResponse(matchFound = true, qrCode = QRTemplateInfo("", "", "", ""))
        return result
    }
}