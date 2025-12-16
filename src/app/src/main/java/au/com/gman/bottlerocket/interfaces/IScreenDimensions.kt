package au.com.gman.bottlerocket.interfaces

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.ScaleAndOffset

interface IScreenDimensions {
    fun setImageSize(size: PointF?)
    fun setPreviewSize(size: PointF?)
    fun setScreenRotation(angle: Int?)

    fun isInitialised(): Boolean

    fun recalculateScalingFactorIfRequired()

    fun getScalingFactor(): ScaleAndOffset?
}