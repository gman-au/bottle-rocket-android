package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.TemplateMatchResponse

interface ITemplateMapper {
    fun tryMatch(qrData: String): TemplateMatchResponse
}