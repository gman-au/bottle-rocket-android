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

    private var isProcessing: Boolean = false

    private var isOutOfBounds: Boolean = false

    private var listener: ISteadyFrameListener? = null

    override fun setListener(listener: ISteadyFrameListener) {
        this.listener = listener
    }

    override fun getStatus(): CaptureStatusEnum {
        if (isProcessing) return CaptureStatusEnum.PROCESSING
        if (isOutOfBounds) return CaptureStatusEnum.OUT_OF_BOUNDS
        if (percentageComplete > amberPercentageThreshold) return CaptureStatusEnum.CAPTURING
        if (percentageComplete <= amberPercentageThreshold) return CaptureStatusEnum.HOLD_STEADY

        return CaptureStatusEnum.NOT_FOUND
    }

    override fun getStatusMessage(): String {
        if (isProcessing) return "Please wait..."
        if (isOutOfBounds) return "Try to fit the page in the camera view"
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
        isOutOfBounds = false
    }

    override fun increment() {
        if (!isProcessing && !isOutOfBounds) {
            consecutiveFramesCount = consecutiveFramesCount + 1
            consecutiveFramesCount = min(consecutiveFramesCount, consecutiveFramesRequired)
            percentageComplete =
                consecutiveFramesCount.toFloat() / consecutiveFramesRequired.toFloat()

            if (percentageComplete >= 1.0F) {
                listener?.onSteadyResult()
            }
        }
    }

    override fun setProcessing(value: Boolean) {
        this.isProcessing = value
    }

    override fun setOutOfBounds(value: Boolean) {
        this.isOutOfBounds = value
    }
}