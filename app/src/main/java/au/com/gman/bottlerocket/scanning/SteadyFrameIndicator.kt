package au.com.gman.bottlerocket.scanning

import au.com.gman.bottlerocket.domain.CaptureStatusEnum
import au.com.gman.bottlerocket.interfaces.ISteadyFrameIndicator
import au.com.gman.bottlerocket.interfaces.ISteadyFrameListener
import javax.inject.Inject
import kotlin.math.min

class SteadyFrameIndicator @Inject constructor() : ISteadyFrameIndicator {

    private var consecutiveFramesCount: Int = 0

    private val consecutiveFramesRequired: Int = 30

    private var percentageComplete: Float = 0.0F

    private var amberPercentageThreshold: Float = 0.33F

    private var blocked: Boolean = false

    private var listener: ISteadyFrameListener? = null

    override fun setListener(listener: ISteadyFrameListener) {
        this.listener = listener
    }

    override fun getStatus(): CaptureStatusEnum {
        if (blocked) return CaptureStatusEnum.PROCESSING
        if (percentageComplete > amberPercentageThreshold) return CaptureStatusEnum.CAPTURING
        if (percentageComplete <= amberPercentageThreshold) return CaptureStatusEnum.HOLD_STEADY

        return CaptureStatusEnum.NOT_FOUND
    }

    override fun getStatusMessage(): String {
        if (blocked) return "Please wait..."
        if (percentageComplete > amberPercentageThreshold) return "Capturing: ${(percentageComplete * 100F).toInt()}%"
        if (percentageComplete <= amberPercentageThreshold) return "Hold steady"

        return "Position QR code"
    }

    override fun getPercentage(): Float {
        return percentageComplete
    }

    override fun reset() {
        consecutiveFramesCount = 0
        percentageComplete = 0F
    }

    override fun increment() {
        if (!blocked) {
            consecutiveFramesCount = consecutiveFramesCount + 1
            consecutiveFramesCount = min(consecutiveFramesCount, consecutiveFramesRequired)
            percentageComplete =
                consecutiveFramesCount.toFloat() / consecutiveFramesRequired.toFloat()

            if (percentageComplete >= 1.0F) {
                listener?.onSteadyResult()
            }
        }
    }

    override fun setBlocked(blocked: Boolean) {
        this.blocked = blocked
    }
}