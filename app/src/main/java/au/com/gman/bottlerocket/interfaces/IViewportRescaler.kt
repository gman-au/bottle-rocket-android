package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.ScaleAndOffset

interface IViewportRescaler {
     fun calculateScalingFactorWithOffset(
         sourceWidth: Float,
         sourceHeight: Float,
         targetWidth: Float,
         targetHeight: Float,
         rotationAngle: Int
    ): ScaleAndOffset
}