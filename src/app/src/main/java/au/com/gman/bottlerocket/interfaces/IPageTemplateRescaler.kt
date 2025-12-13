package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.RocketBoundingBox

interface IPageTemplateRescaler {
    fun rescalePageOverlay(
        qrCorners: RocketBoundingBox,
        pageTemplateBoundingBox: RocketBoundingBox,
        imageWidth: Float,
        imageHeight: Float,
        previewWidth: Float,
        previewHeight: Float
    ): RocketBoundingBox
}