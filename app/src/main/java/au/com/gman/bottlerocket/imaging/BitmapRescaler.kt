package au.com.gman.bottlerocket.imaging

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.interfaces.IBitmapRescaler
import javax.inject.Inject

class BitmapRescaler @Inject constructor(
) : IBitmapRescaler {
    override fun calculateScalingFactor(
        sourceWidth: Float,
        sourceHeight: Float,
        targetWidth: Float,
        targetHeight: Float
    ): ScaleAndOffset {
        val scaleWidth = targetWidth / sourceWidth
        val scaleHeight = targetHeight / sourceHeight
        return ScaleAndOffset(
            PointF(scaleWidth, scaleHeight),
            PointF(0f, 0f)
        )
    }
}