package au.com.gman.bottlerocket.qrCode

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.PageSizeEnum
import au.com.gman.bottlerocket.domain.PageTemplate
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.TemplateTypeEnum
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import javax.inject.Inject
import kotlin.collections.get

class QrCodeTemplateMatcher @Inject constructor() : IQrCodeTemplateMatcher {

    companion object {
        private val STANDARD_A4_BOUNDS_QR_BOTTOM_LEFT =
            RocketBoundingBox(
                topLeft = PointF(0f, -11.0f),
                topRight = PointF(9.0f, -11.0f),
                bottomRight = PointF(9.0f, 1.0f),
                bottomLeft = PointF(0f, 1.0f)
            )

        private val STANDARD_A4_BOUNDS_QR_BOTTOM_RIGHT =
            RocketBoundingBox(
                topLeft = PointF(-8.0f, -11.0f),
                topRight = PointF(1.0f, -11.0f),
                bottomRight = PointF(1.0f, 1.0f),
                bottomLeft = PointF(-8.0f, 1.0f)
            )

        private val STANDARD_A5_BOUNDS_QR_BOTTOM_LEFT =
            RocketBoundingBox(
                topLeft = PointF(0f, -25.0f),
                topRight = PointF(15.5f, -25.0f),
                bottomRight = PointF(15.5f, 1.0f),
                bottomLeft = PointF(0f, 1.0f)
            )

        private val STANDARD_A5_BOUNDS_QR_BOTTOM_RIGHT =
            RocketBoundingBox(
                topLeft = PointF(-14.5f, -25.0f),
                topRight = PointF(1.0f, -25.0f),
                bottomRight = PointF(1.0f, 1.0f),
                bottomLeft = PointF(-14.5f, 1.0f)
            )
    }

    val templatesMap = mapOf(
        "04o" to PageTemplate(
            templateType = TemplateTypeEnum.STANDARD_LINED,
            pageSize = PageSizeEnum.A5,
            pageDimensions = STANDARD_A5_BOUNDS_QR_BOTTOM_LEFT
        ),
        "04p" to PageTemplate(
            templateType = TemplateTypeEnum.STANDARD_DOTTED,
            pageSize = PageSizeEnum.A5,
            pageDimensions = STANDARD_A5_BOUNDS_QR_BOTTOM_RIGHT
        ),
        "P01 V1F T02 S000" to PageTemplate(
            templateType = TemplateTypeEnum.STANDARD_LINED,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_LEFT
        ),
        "P01 V17 T02 S000" to PageTemplate(
            templateType = TemplateTypeEnum.STANDARD_LINED,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_LEFT
        ),
        "P02 V1F T02 S000" to PageTemplate(
            templateType = TemplateTypeEnum.STANDARD_LINED,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_RIGHT
        ),
        "P02 V17 T02 S000" to PageTemplate(
            templateType = TemplateTypeEnum.STANDARD_LINED,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_RIGHT
        ),
        "P01 V17 T01 S000" to PageTemplate(
            templateType = TemplateTypeEnum.STANDARD_DOTTED,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_LEFT
        ),
        "P02 V17 T01 S000" to PageTemplate(
            templateType = TemplateTypeEnum.STANDARD_DOTTED,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_RIGHT
        ),
        "P01 V17 T03 S000" to PageTemplate(
            templateType = TemplateTypeEnum.PROJECT_TASK_TRACKER,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_LEFT
        ),
        "P01 V17 T04 S000" to PageTemplate(
            templateType = TemplateTypeEnum.WEEKLY_PAGE,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_RIGHT
        ),
        "P02 V17 T04 S000" to PageTemplate(
            templateType = TemplateTypeEnum.WEEKLY_PAGE,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_LEFT
        ),
        "P01 V17 T05 S000" to PageTemplate(
            templateType = TemplateTypeEnum.MONTHLY_PAGE,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_RIGHT
        ),
        "P01 V17 T06 S000" to PageTemplate(
            templateType = TemplateTypeEnum.OKRS_PAGE,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_LEFT
        ),
        "P01 V17 T06 S000" to PageTemplate(
            templateType = TemplateTypeEnum.OKRS_PAGE,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_LEFT
        ),
        "P01 V17 T07 S000" to PageTemplate(
            templateType = TemplateTypeEnum.IDEAS_PAGE,
            pageSize = PageSizeEnum.A4,
            pageDimensions = STANDARD_A4_BOUNDS_QR_BOTTOM_RIGHT
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