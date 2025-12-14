package au.com.gman.bottlerocket.interfaces

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.RocketBoundingBox

interface IBoundingBoxRescaler {
    fun rescaleUsingQrCorners(
        qrCorners: RocketBoundingBox,
        sourceBoundingBox: RocketBoundingBox,
        scalingFactor: PointF
    ): RocketBoundingBox

    fun calculateScalingFactor(
        firstWidth: Float,
        firstHeight: Float,
        secondWidth: Float,
        secondHeight: Float,
        rotationAngle: Int
    ): PointF
}