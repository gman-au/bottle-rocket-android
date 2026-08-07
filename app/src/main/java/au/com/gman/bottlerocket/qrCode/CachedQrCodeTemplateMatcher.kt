package au.com.gman.bottlerocket.qrCode

import au.com.gman.bottlerocket.domain.PageTemplate
import au.com.gman.bottlerocket.interfaces.IRocketbookQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IQrTemplateCache
import javax.inject.Inject

@Deprecated("API QR code lists are no longer utilized in favour of vendor approximations")
class CachedQrCodeTemplateMatcher @Inject constructor(
    private val templateCache: IQrTemplateCache
) : IRocketbookQrCodeTemplateMatcher {

    override fun tryMatch(qrCode: String?): PageTemplate? {
        if (qrCode == null) return null

        val template = templateCache.getTemplates()
            .firstOrNull { it.qrCode == qrCode.trim() }
            ?: return null

        return PageTemplate(
            qrCode = template.qrCode,
            bookVendor = template.bookVendor
        )
    }
}