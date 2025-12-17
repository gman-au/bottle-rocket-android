package au.com.gman.bottlerocket.interfaces

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.ScaleAndOffset

interface IScreenDimensions {
    fun setSourceSize(size: PointF?)
    fun setTargetSize(size: PointF?)
    fun setScreenRotation(angle: Int?)
    fun isInitialised(): Boolean
    fun recalculateScalingFactorIfRequired()
    fun getScalingFactor(): ScaleAndOffset?
    fun getScreenRotation(): Float
    fun getTargetSize(): PointF?
    fun getSourceSize(): PointF?
}