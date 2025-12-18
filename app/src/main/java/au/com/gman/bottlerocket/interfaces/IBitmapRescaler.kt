package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.ScaleAndOffset

interface IBitmapRescaler {
     fun calculateScalingFactor(
         sourceWidth: Float,
         sourceHeight: Float,
         targetWidth: Float,
         targetHeight: Float
    ): ScaleAndOffset
}