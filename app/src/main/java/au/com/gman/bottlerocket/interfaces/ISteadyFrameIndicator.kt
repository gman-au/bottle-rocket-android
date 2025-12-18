package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.CaptureStatusEnum

interface ISteadyFrameIndicator {

    fun setListener(listener: ISteadyFrameListener)

    fun getStatus(): CaptureStatusEnum

    fun getStatusMessage(): String

    fun getPercentage(): Float

    fun reset()

    fun increment()

    fun setBlocked(blocked: Boolean)
}