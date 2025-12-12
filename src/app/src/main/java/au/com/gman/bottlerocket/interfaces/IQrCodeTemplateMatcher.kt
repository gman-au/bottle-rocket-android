package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.TemplateMatchResponse

interface IQrCodeTemplateMatcher {
    fun tryMatch(qrCode: String?): TemplateMatchResponse
}