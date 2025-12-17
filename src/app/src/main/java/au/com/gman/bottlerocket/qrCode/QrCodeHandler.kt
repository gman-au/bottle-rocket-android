package au.com.gman.bottlerocket.qrCode

import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.calculateRotationAngle
import au.com.gman.bottlerocket.domain.round
import au.com.gman.bottlerocket.domain.scaleWithOffset
import au.com.gman.bottlerocket.imaging.PageTemplateRescaler
import au.com.gman.bottlerocket.imaging.RocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.imaging.aggressiveSmooth
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.barcode.common.Barcode
import javax.inject.Inject

class QrCodeHandler @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val pageTemplateRescaler: PageTemplateRescaler,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
) : IQrCodeHandler {

    companion object {
        private const val TAG = "QrCodeHandler"
    }

    // Option 1: Track previous frame for temporal smoothing AFTER homography
    private var previousPageBounds: RocketBoundingBox? = null

    // Option 2: Median filter (uncomment to use instead of temporal smoothing)
    private val medianFilter = RocketBoundingBoxMedianFilter(bufferSize = 50)

    override fun handle(barcode: Barcode?): BarcodeDetectionResult {
        var matchFound = false
        var pageBoundingBox: RocketBoundingBox? = null
        var qrCornerPointsBoxUnscaled: RocketBoundingBox? = null
        var qrCornerPointsBoxScaled: RocketBoundingBox? = null
        var qrCodeValue: String? = null
        var validationMessage: String? = null

        val pageTemplate =
            qrCodeTemplateMatcher
                .tryMatch(barcode?.rawValue ?: "")

        if (barcode != null) {
            qrCodeValue = barcode.rawValue

            val qrCornerPoints = RocketBoundingBox(barcode.cornerPoints)

            // Use raw corner points - don't stabilize the input
            // The homography amplifies tiny movements, so we stabilize AFTER
            qrCornerPointsBoxUnscaled = qrCornerPoints

            if (!screenDimensions.isInitialised())
                throw IllegalStateException("Screen dimensions not initialised")

            screenDimensions
                .recalculateScalingFactorIfRequired()

            val scalingFactorViewport =
                screenDimensions
                    .getScalingFactor()

            val rotationAngle =
                qrCornerPointsBoxUnscaled
                    .calculateRotationAngle()

            if (pageTemplate != null && scalingFactorViewport != null) {

                qrCornerPointsBoxScaled =
                    qrCornerPointsBoxUnscaled
                        .scaleWithOffset(scalingFactorViewport)

                Log.d(
                    TAG,
                    buildString {
                        appendLine("qrBoundingBoxUnscaled:")
                        appendLine("$qrCornerPointsBoxUnscaled")
                    }
                )

                Log.d(
                    TAG,
                    buildString {
                        appendLine("qrBoundingBoxScaled:")
                        appendLine("$qrCornerPointsBoxScaled")
                    }
                )

                val rawPageBounds =
                    pageTemplateRescaler
                        .calculatePageBounds(
                            qrCornerPointsBoxUnscaled,
                            RocketBoundingBox(pageTemplate.pageDimensions),
                            rotationAngle
                        )

                val scaledPageBounds =
                    rawPageBounds
                        .scaleWithOffset(scalingFactorViewport)

                // OPTION 1: Aggressive smoothing with complete outlier rejection
                pageBoundingBox = scaledPageBounds.aggressiveSmooth(
                    previous = previousPageBounds,
                    smoothFactor = 0.9f,        // 90% previous, 10% current
                    maxJumpThreshold = 50f      // Completely reject frames with ANY corner jumping >50px
                )

                // OPTION 2: Median filter (uncomment to try - most stable but slight lag)
                pageBoundingBox = medianFilter.add(scaledPageBounds)

                // Store for next frame (Option 1 only)
                previousPageBounds = pageBoundingBox

                matchFound = true

                Log.d(
                    TAG,
                    buildString {
                        appendLine("final pageBoundingBox:")
                        appendLine("$pageBoundingBox")
                    }
                )
            } else {
                // Reset when we lose tracking
                previousPageBounds = null
                // medianFilter.reset() // Uncomment if using Option 2
            }
        } else {
            // Reset when no barcode detected
            previousPageBounds = null
            // medianFilter.reset() // Uncomment if using Option 2
        }

        return BarcodeDetectionResult(
            matchFound = matchFound,
            qrCode = qrCodeValue,
            pageTemplate = pageTemplate,
            pageOverlayPath = pageBoundingBox?.round(),
            qrCodeOverlayPath = qrCornerPointsBoxScaled?.round(),
            validationMessage = validationMessage
        )
    }
}