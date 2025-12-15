package au.com.gman.bottlerocket.qrCode

import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.applyRotation
import au.com.gman.bottlerocket.domain.calculateRotationAngle
import au.com.gman.bottlerocket.domain.round
import au.com.gman.bottlerocket.domain.scaleWithOffset
import au.com.gman.bottlerocket.imaging.PageTemplateRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeHandler
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import au.com.gman.bottlerocket.interfaces.IViewportRescaler
import com.google.mlkit.vision.barcode.common.Barcode
import javax.inject.Inject

class QrCodeHandler @Inject constructor(
    private val screenDimensions: IScreenDimensions,
    private val viewportRescaler: IViewportRescaler,
    private val pageTemplateRescaler: PageTemplateRescaler,
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher
) : IQrCodeHandler {

    companion object {
        private const val TAG = "QrCodeHandler"
    }

    override fun handle(barcode: Barcode?): BarcodeDetectionResult {
        var matchFound = false
        var pageBoundingBox: RocketBoundingBox? = null
        var qrBoundingBoxUnscaled: RocketBoundingBox? = null
        var qrBoundingBoxScaled: RocketBoundingBox? = null
        var qrCode: String? = null

        val pageTemplate = qrCodeTemplateMatcher.tryMatch(barcode?.rawValue ?: "")

        if (barcode != null) {
            qrCode = barcode.rawValue
            qrBoundingBoxUnscaled = RocketBoundingBox(barcode.cornerPoints)

            if (!screenDimensions.isInitialised())
                throw IllegalStateException("Screen dimensions not initialised")

            val imageSize = screenDimensions.getImageSize()
            val previewSize = screenDimensions.getPreviewSize()
            val rotationDegrees = screenDimensions.getScreenRotation()

            Log.d(
                TAG,
                buildString {
                    appendLine("imageSize: ${imageSize?.x} x ${imageSize?.y}")
                    appendLine("previewSize: ${previewSize?.x} x ${previewSize?.y}")
                    appendLine("rotationDegrees: $rotationDegrees")
                }
            )

            // the first scale factor is the viewport vs the preview
            val scalingFactorViewport =
                viewportRescaler
                    .calculateScalingFactorWithOffset(
                        firstWidth = imageSize!!.x,
                        firstHeight = imageSize.y,
                        secondWidth = previewSize!!.x,
                        secondHeight = previewSize.y,
                        rotationAngle = rotationDegrees!!
                    )

            Log.d(
                TAG,
                buildString {
                    appendLine("scalingFactorViewport: $scalingFactorViewport")
                }
            )

            val rotationAngle =
                qrBoundingBoxUnscaled
                    .calculateRotationAngle();

            Log.d(
                TAG,
                buildString {
                    appendLine("rotationAngle: $rotationAngle")
                }
            )

            if (pageTemplate != null) {
                matchFound = true

                qrBoundingBoxScaled =
                    qrBoundingBoxUnscaled
                        .scaleWithOffset(scalingFactorViewport)

                pageBoundingBox =
                    pageTemplateRescaler
                        .calculatePageBounds(
                            qrBoundingBoxUnscaled,
                            qrBoundingBoxScaled,
                            RocketBoundingBox(pageTemplate.pageDimensions)
                        )

                Log.d(
                    TAG,
                    buildString {
                        appendLine("final qrBoundingBox:")
                        appendLine("$qrBoundingBoxUnscaled")
                    }
                )

                pageBoundingBox =
                    pageBoundingBox
                        .applyRotation(
                            rotationAngle,
                            pageBoundingBox.bottomLeft
                        )

            }
        }

        return BarcodeDetectionResult(
            matchFound = matchFound,
            qrCode = qrCode,
            pageTemplate = pageTemplate,
            pageOverlayPath = pageBoundingBox?.round(),
            qrCodeOverlayPath = qrBoundingBoxScaled?.round()
        )
    }
}