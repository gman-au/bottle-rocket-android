package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.RocketBoundingBox

interface IPageTemplateRescaler {
    fun calculatePageBounds(
        qrBoxIdeal: RocketBoundingBox,
        qrBoxActual: RocketBoundingBox,
        pageBoxIdeal: RocketBoundingBox
    ): RocketBoundingBox
}