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

    private var imageSize: PointF? = null
    private var previewSize: PointF? = null
    private var screenRotation: Int? = null

    private var hasChanged: Boolean = true

    private var scaleAndOffset: ScaleAndOffset? = null

    override fun setImageSize(size: PointF?) {
        if (imageSize?.x != size?.x || imageSize?.y != size?.y) {
            imageSize = size
            hasChanged = true
        }
    }

    override fun setPreviewSize(size: PointF?) {
        if (previewSize?.x != size?.x || previewSize?.y != size?.y) {
            previewSize = size
            hasChanged = true
        }
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
            Log.d(TAG, "Re-computed scaling factor:${scaleAndOffset?.scale?.x}, ${scaleAndOffset?.scale?.y}")
            hasChanged = false
        }
    }
}