package au.com.gman.bottlerocket.imaging

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.PageTemplate
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import javax.inject.Inject

class QrCodeTemplateMatcher @Inject constructor(): IQrCodeTemplateMatcher {

    val templatesMap = mapOf(
        "04o" to PageTemplate(
            type = "1",
            // Page dimensions in QR-relative units
            // If QR is ~50px and page is 500x700px, that's 10x14 QR units
            pageDimensions = RocketBoundingBox(
                topLeft = PointF(0f, -220f),    // 21.5 QR-widths left, 28 QR-heights up
                topRight = PointF(350f, -220f),       // at QR X, 28 QR-heights up
                bottomRight = PointF(350f, 0f),      // at QR position (bottom-right corner)
                bottomLeft = PointF(0f, 0f)    // 21.5 QR-widths left, at QR Y
            )
        )
    )

    override fun tryMatch(qrCode: String?): PageTemplate? {
        return if (qrCode in templatesMap) {
            templatesMap[qrCode]
        } else {
            null
        }
    }
}