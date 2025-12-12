package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.TemplateMatchResponse

interface ITemplateListener {
    fun onDetectionSuccess(matchedTemplate: TemplateMatchResponse)
}