package au.com.gman.bottlerocket.qrCode

import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.extensions.aggressiveSmooth
import au.com.gman.bottlerocket.extensions.calculateRotationAngle
import au.com.gman.bottlerocket.extensions.isOutOfBounds
import au.com.gman.bottlerocket.extensions.round
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IRocketBoundingBoxMedianFilter
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import com.google.mlkit.vision.barcode.common.Barcode
import javax.inject.Inject

class QrCodeHandler @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val pageTemplateRescaler: IPageTemplateRescaler,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
    private val rocketBoundingBoxMedianFilter: IRocketBoundingBoxMedianFilter
) : IQrCodeHandler {

    companion object {
        private const val TAG = "QrCodeHandler"
    }

    private var previousPageBounds: RocketBoundingBox? = null

    override fun handle(
        barcode: Barcode?,
        sourceWidth: Int,
        sourceHeight: Int
    ): BarcodeDetectionResult {
        val codeFound = true
        var matchFound = false
        var outOfBounds = false
        var pageBoundingBox: RocketBoundingBox? = null
        var pageBoundingBoxUnscaled: RocketBoundingBox? = null
        var qrCornerPointsBoxUnscaled: RocketBoundingBox? = null
        var qrCornerPointsBoxScaled: RocketBoundingBox? = null
        var qrCodeValue: String? = null
        var cameraRotation: Float = 0F
        var boundingBoxRotation: Float = 0F
        var scalingFactorViewport: ScaleAndOffset? = null

        val pageTemplate = qrCodeTemplateMatcher.tryMatch(barcode?.rawValue ?: "")

        if (barcode != null) {
            qrCodeValue = barcode.rawValue
            Log.d(TAG, "QR code value found: $qrCodeValue")

            val qrCornerPoints = RocketBoundingBox(barcode.cornerPoints)
            qrCornerPointsBoxUnscaled = qrCornerPoints

            if (!screenDimensions.isInitialised())
                throw IllegalStateException("Screen dimensions not initialised")

            screenDimensions
                .recalculateScalingFactorIfRequired()

            scalingFactorViewport =
                screenDimensions
                    .getScalingFactor()

            boundingBoxRotation =
                qrCornerPointsBoxUnscaled
                    .calculateRotationAngle()

            cameraRotation =
                screenDimensions
                    .getScreenRotation()

            if (scalingFactorViewport != null) {

                qrCornerPointsBoxScaled =
                    qrCornerPointsBoxUnscaled
                        .scaleUpWithOffset(scalingFactorViewport)

                if (screenDimensions.getTargetSize() == null)
                    throw IllegalStateException("Screen dimensions not initialised")

                // any qr point outside viewport should be out of bounds
                outOfBounds =
                    qrCornerPointsBoxScaled
                        .isOutOfBounds(screenDimensions.getTargetSize()!!)

                if (pageTemplate != null) {

                    // Calculate page bounds in UNSCALED (ImageAnalysis) space
                    val rawPageBounds =
                        pageTemplateRescaler
                            .calculatePageBoundsFromTemplate(
                                qrCornerPointsBoxUnscaled,
                                RocketBoundingBox(pageTemplate.pageDimensions)
                            )

                    // Store the unscaled version
                    pageBoundingBoxUnscaled = rawPageBounds

                    // Scale for preview display
                    val scaledPageBounds =
                        rawPageBounds
                            .scaleUpWithOffset(scalingFactorViewport)

                    // Apply smoothing to the SCALED version (for preview)
                    pageBoundingBox =
                        scaledPageBounds
                            .aggressiveSmooth(
                                previous = previousPageBounds,
                                smoothFactor = 0.3f,
                                maxJumpThreshold = 50f
                            )

                    // any qr point outside viewport should be out of bounds
                    outOfBounds =
                        outOfBounds ||
                                pageBoundingBox
                                    .isOutOfBounds(screenDimensions.getTargetSize()!!)

                    pageBoundingBox =
                        rocketBoundingBoxMedianFilter
                            .add(pageBoundingBox)

                    previousPageBounds = pageBoundingBox

                    matchFound = true

                    Log.d(
                        TAG,
                        "Unscaled page bounds (ImageAnalysis space): $pageBoundingBoxUnscaled"
                    )
                    Log.d(TAG, "Scaled page bounds (Preview space): $pageBoundingBox")
                } else {
                    previousPageBounds = null
                    rocketBoundingBoxMedianFilter.reset()
                }
            } else {
                previousPageBounds = null
                rocketBoundingBoxMedianFilter.reset()
            }
        } else {
            previousPageBounds = null
            rocketBoundingBoxMedianFilter.reset()
        }

        return BarcodeDetectionResult(
            codeFound = codeFound,
            matchFound = matchFound,
            outOfBounds = outOfBounds,
            qrCode = qrCodeValue,
            pageTemplate = pageTemplate,
            pageOverlayPath = pageBoundingBoxUnscaled?.round(),
            pageOverlayPathPreview = pageBoundingBox?.round(),
            qrCodeOverlayPath = qrCornerPointsBoxUnscaled?.round(),
            qrCodeOverlayPathPreview = qrCornerPointsBoxScaled?.round(),
            cameraRotation = cameraRotation,
            boundingBoxRotation = boundingBoxRotation,
            scalingFactor = scalingFactorViewport,
            sourceImageWidth = sourceWidth,
            sourceImageHeight = sourceHeight
        )
    }
}