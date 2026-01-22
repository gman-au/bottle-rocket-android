package au.com.gman.bottlerocket.qrCode

import au.com.gman.bottlerocket.domain.PageTemplate
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IQrTemplateCache
import javax.inject.Inject

class QrCodeTemplateMatcher @Inject constructor(
    private val templateCache: IQrTemplateCache
) : IQrCodeTemplateMatcher {

    override fun tryMatch(qrCode: String?): PageTemplate? {
        if (qrCode == null) return null

        val template = templateCache.getTemplates()
            .firstOrNull { it.qrCode == qrCode }
            ?: return null

        return PageTemplate(
            qrCode = template.qrCode,
            bookVendor = template.bookVendor
        )
    }
}