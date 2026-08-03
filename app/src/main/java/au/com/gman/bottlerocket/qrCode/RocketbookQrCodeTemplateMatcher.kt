package au.com.gman.bottlerocket.qrCode

import au.com.gman.bottlerocket.domain.PageTemplate
import au.com.gman.bottlerocket.interfaces.IRocketbookQrCodeTemplateMatcher
import javax.inject.Inject

class RocketbookQrCodeTemplateMatcher @Inject constructor(
) : IRocketbookQrCodeTemplateMatcher {

    companion object {
        private const val VENDOR_ROCKETBOOK = "Rocketbook"
    }

    override fun tryMatch(qrCode: String?): PageTemplate? {
        if (qrCode == null) return null

        var matchFound = false

        if (qrCode.startsWith("P")) {
            if (qrCode.length >= 11 && qrCode.length <= 18) {
                matchFound = true
            }
        }

        // A5 pages are handled differently due to their size
        if (qrCode.length == 3 && qrCode.startsWith("0")) {
            matchFound = true
        }

        if (matchFound) {
            return PageTemplate(
                qrCode = qrCode,
                bookVendor = VENDOR_ROCKETBOOK
            )
        }

        return null
    }
}