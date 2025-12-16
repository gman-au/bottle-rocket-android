package au.com.gman.bottlerocket.qrCode

import android.util.Log
import au.com.gman.bottlerocket.BottleRocketApplication
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.calculateRotationAngle
import au.com.gman.bottlerocket.domain.round
import au.com.gman.bottlerocket.domain.scaleWithOffset
import au.com.gman.bottlerocket.imaging.BoundingBoxStabilizer
import au.com.gman.bottlerocket.imaging.PageTemplateRescaler
import au.com.gman.bottlerocket.interfaces.IBoundingBoxValidator
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import com.google.mlkit.vision.barcode.common.Barcode
import javax.inject.Inject

class QrCodeHandler @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val pageTemplateRescaler: PageTemplateRescaler,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
    private val boundingBoxValidator: IBoundingBoxValidator
) : IQrCodeHandler {

    companion object {
        private const val TAG = "QrCodeHandler"

        private const val USE_SMOOTHING = !BottleRocketApplication.USE_SMOOTHING
        private const val USE_VALIDATION = !BottleRocketApplication.USE_VALIDATION
    }

    private val qrStabilizer = BoundingBoxStabilizer(0.15f, 3)
    private val pageStabilizer = BoundingBoxStabilizer(0.15f, 3)

    override fun handle(barcode: Barcode?): BarcodeDetectionResult {
        var matchFound = false
        var pageBoundingBox: RocketBoundingBox? = null
        var qrBoundingBoxUnscaled: RocketBoundingBox? = null
        var qrBoundingBoxScaled: RocketBoundingBox? = null
        var qrCode: String? = null
        var validationMessage: String? = null

        val pageTemplate =
            qrCodeTemplateMatcher
                .tryMatch(barcode?.rawValue ?: "")

        if (barcode != null) {
            qrCode = barcode.rawValue
            val rawQrBounds = RocketBoundingBox(barcode.cornerPoints)

            qrBoundingBoxUnscaled = when (USE_SMOOTHING) {
                true -> qrStabilizer
                    .stabilize(rawQrBounds)
                false -> rawQrBounds
            }

            if (!screenDimensions.isInitialised())
                throw IllegalStateException("Screen dimensions not initialised")

            screenDimensions
                .recalculateScalingFactorIfRequired()

            val scalingFactorViewport =
                screenDimensions
                    .getScalingFactor()

            val rotationAngle =
                qrBoundingBoxUnscaled
                    .calculateRotationAngle()

            if (pageTemplate != null && scalingFactorViewport != null) {

                qrBoundingBoxScaled =
                    qrBoundingBoxUnscaled
                        .scaleWithOffset(scalingFactorViewport)

                val rawPageBounds =
                    pageTemplateRescaler
                        .calculatePageBounds(
                            qrBoundingBoxUnscaled,
                            RocketBoundingBox(pageTemplate.pageDimensions),
                            rotationAngle
                        )

                val scaledPageBounds =
                    rawPageBounds
                        .scaleWithOffset(scalingFactorViewport)

                // Stabilize page bounds
                val stabilizedPageBounds = when (USE_SMOOTHING) {
                    true -> pageStabilizer.stabilize(scaledPageBounds)
                    false -> scaledPageBounds
                }

                matchFound = qrStabilizer.isStable() && pageStabilizer.isStable()

                // Validate the page bounds
                val isValid =
                    boundingBoxValidator.isValid(stabilizedPageBounds) || !USE_VALIDATION
                val isStable =
                    (qrStabilizer.isStable() && pageStabilizer.isStable()) || !USE_SMOOTHING

                if (isValid && isStable) {
                    // Only show page overlay when valid
                    pageBoundingBox = stabilizedPageBounds
                    matchFound = true
                } else {
                    // Show validation feedback
                    val issues = boundingBoxValidator.getValidationIssues(stabilizedPageBounds)
                    validationMessage = issues.firstOrNull() ?: "Align camera with page"
                }

                Log.d(
                    TAG,
                    buildString {
                        appendLine("final qrBoundingBox:")
                        appendLine("$qrBoundingBoxUnscaled")
                    }
                )

                Log.d(
                    TAG,
                    buildString {
                        appendLine("final stabilizedPageBounds:")
                        appendLine("$stabilizedPageBounds")
                    }
                )
            } else {
                pageStabilizer.reset()
            }
        } else {
            qrStabilizer.reset()
            pageStabilizer.reset()
        }

        return BarcodeDetectionResult(
            matchFound = matchFound,
            qrCode = qrCode,
            pageTemplate = pageTemplate,
            pageOverlayPath = pageBoundingBox?.round(),
            qrCodeOverlayPath = qrBoundingBoxScaled?.round(),
            validationMessage = validationMessage
        )
    }
}