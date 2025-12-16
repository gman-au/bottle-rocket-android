package au.com.gman.bottlerocket.imaging

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import javax.inject.Inject

class ScreenDimensions @Inject constructor(
    private val viewportRescaler: IViewportRescaler
) : IScreenDimensions {

    private var imageSize: PointF? = null
    private var previewSize: PointF? = null
    private var screenRotation: Int? = null

    private var hasChanged: Boolean = true

    private var scaleAndOffset: ScaleAndOffset? = null

    override fun setImageSize(size: PointF?) {
        imageSize = size
        hasChanged = true
    }

    override fun setPreviewSize(size: PointF?) {
        previewSize = size
        hasChanged = true
    }

    override fun setScreenRotation(angle: Int?) {
        screenRotation = angle
        hasChanged = true
    }

    override fun getScalingFactor(): ScaleAndOffset? {
        return scaleAndOffset
    }

    override fun isInitialised(): Boolean {
        return (imageSize != null && previewSize != null && screenRotation != null)
    }

    override fun recalculateScalingFactorIfRequired() {
        if (hasChanged) {
            scaleAndOffset =
                viewportRescaler
                    .calculateScalingFactorWithOffset(
                        firstWidth = imageSize!!.x,
                        firstHeight = imageSize!!.y,
                        secondWidth = previewSize!!.x,
                        secondHeight = previewSize!!.y,
                        rotationAngle = screenRotation!!
                    )
            hasChanged = false
        }
    }
}