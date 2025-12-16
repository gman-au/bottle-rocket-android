package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.RocketBoundingBox

interface IPageTemplateRescaler {
    fun calculatePageBounds(
        qrBoxIdeal: RocketBoundingBox,
        pageBoxIdeal: RocketBoundingBox,
        rotationAngle: Float
    ): RocketBoundingBox
}