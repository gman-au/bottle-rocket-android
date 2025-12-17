package au.com.gman.bottlerocket.scanning

import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import javax.inject.Inject

class ScreenDimensions @Inject constructor(
    private val viewportRescaler: IViewportRescaler
) : IScreenDimensions {

    companion object {
        private const val TAG = "ScreenDimensions"
    }

    private var sourceSize: PointF? = null
    private var targetSize: PointF? = null
    private var screenRotation: Int? = null

    private var hasChanged: Boolean = true

    private var scaleAndOffset: ScaleAndOffset? = null

    override fun setSourceSize(size: PointF?) {
        if (sourceSize?.x != size?.x || sourceSize?.y != size?.y) {
            sourceSize = size
            hasChanged = true
        }
    }

    override fun setTargetSize(size: PointF?) {
        if (targetSize?.x != size?.x || targetSize?.y != size?.y) {
            targetSize = size
            hasChanged = true
        }
    }

    override fun getSourceSize(): PointF? {
        return sourceSize
    }

    override fun getTargetSize(): PointF? {
        return targetSize
    }

    override fun setScreenRotation(angle: Int?) {
        if (screenRotation != angle) {
            screenRotation = angle
            hasChanged = true
        }
    }

    override fun getScalingFactor(): ScaleAndOffset? {
        return scaleAndOffset
    }

    override fun getScreenRotation(): Float {
        return screenRotation?.toFloat() ?: 0F
    }

    override fun isInitialised(): Boolean {
        return (sourceSize != null && targetSize != null && screenRotation != null)
    }

    override fun recalculateScalingFactorIfRequired() {
        if (hasChanged) {
            scaleAndOffset =
                viewportRescaler
                    .calculateScalingFactorWithOffset(
                        sourceWidth = sourceSize!!.x,
                        sourceHeight = sourceSize!!.y,
                        targetWidth = targetSize!!.x,
                        targetHeight = targetSize!!.y,
                        rotationAngle = screenRotation!!
                    )
            Log.d(
                TAG,
                "Re-computed scaling factor:${scaleAndOffset?.scale?.x}, ${scaleAndOffset?.scale?.y}"
            )
            hasChanged = false
        }
    }
}