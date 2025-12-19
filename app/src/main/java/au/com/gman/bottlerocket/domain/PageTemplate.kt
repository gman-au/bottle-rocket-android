package au.com.gman.bottlerocket.domain

data class PageTemplate(
    val pageSize: PageSizeEnum,
    val templateType: TemplateTypeEnum,
    val pageDimensions: RocketBoundingBox
)