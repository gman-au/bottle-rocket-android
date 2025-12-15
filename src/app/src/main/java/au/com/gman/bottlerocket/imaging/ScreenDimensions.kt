package au.com.gman.bottlerocket.imaging

import android.graphics.PointF
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import javax.inject.Inject

class ScreenDimensions @Inject constructor() : IScreenDimensions {
    private var imageSize: PointF? = null
    private var previewSize: PointF? = null
    private var screenRotation: Int? = null

    override fun setImageSize(size: PointF?) {
        imageSize = size
    }

    override fun setPreviewSize(size: PointF?) {
        previewSize = size
    }

    override fun setScreenRotation(angle: Int?) {
        screenRotation = angle
    }

    override fun getImageSize(): PointF? {
        return imageSize
    }

    override fun getPreviewSize(): PointF? {
        return previewSize
    }

    override fun getScreenRotation(): Int? {
        return screenRotation
    }

    override fun isInitialised(): Boolean {
        return (imageSize != null && previewSize != null && screenRotation != null)
    }
}