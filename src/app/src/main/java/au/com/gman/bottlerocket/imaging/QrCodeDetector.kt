package au.com.gman.bottlerocket.imaging

import android.graphics.Point
import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.TemplateMatchResponse
import au.com.gman.bottlerocket.domain.applyRotation
import au.com.gman.bottlerocket.domain.calculateRotationAngle
import au.com.gman.bottlerocket.domain.normalize
import au.com.gman.bottlerocket.domain.scale
import au.com.gman.bottlerocket.interfaces.IBoundingBoxRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.ITemplateListener
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class QrCodeDetector @Inject constructor(
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
    private val pageTemplateRescaler: IBoundingBoxRescaler
): IQrCodeDetector {

    private val scanner = BarcodeScanning.getClient()
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0

    override fun setPreviewSize(width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
    }

    private var listener: ITemplateListener? = null

    override fun setListener(listener: ITemplateListener) {
        this.listener = listener
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            val imageWidth = mediaImage.width
            val imageHeight = mediaImage.height
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val barcode = barcodes.firstOrNull()
                    val qrCode = barcode?.rawValue
                    var matchFound = false
                    var pageBoundingBox: RocketBoundingBox? = null
                    var qrBoundingBox: RocketBoundingBox? = null

                    val pageTemplate = qrCodeTemplateMatcher.tryMatch(barcode?.rawValue ?: "")

                    if (barcode != null && barcode.boundingBox != null) {
                        Log.d("QR_DEBUG", "QR corner points:")
                        barcode.cornerPoints!!.forEachIndexed { i, pt ->
                            Log.d("QR_DEBUG", "  [$i]: (${pt.x}, ${pt.y})")
                        }
                        Log.d("QR_DEBUG", "QR boundingBox: ${barcode.boundingBox}")
                        val cornerPoints = toPointArray(barcode.cornerPoints)
                        qrBoundingBox = RocketBoundingBox(cornerPoints)

                        // the first scale factor is the viewport vs the preview
                        val scalingFactorViewport =
                            pageTemplateRescaler
                                .calculateScalingFactor(
                                    firstWidth = imageWidth.toFloat(),
                                    firstHeight = imageHeight.toFloat(),
                                    secondWidth = previewWidth.toFloat(),
                                    secondHeight = previewHeight.toFloat(),
                                    rotationAngle = rotationDegrees
                                )

                        // the second scale factor is the comparative QR code vs the actual
                        // we need to 'straighten up' the QR code box
                        val straightQrBox =
                            qrBoundingBox
                                .applyRotation(
                                    qrBoundingBox.calculateRotationAngle(),
                                    qrBoundingBox.topLeft
                                )
                                .normalize()

                        val scalingFactorQrCode =
                            pageTemplateRescaler
                                .calculateScalingFactor(
                                    firstWidth = 20.0F,
                                    firstHeight = 20.0F,
                                    secondWidth = straightQrBox.topRight.x - straightQrBox.topLeft.x,
                                    secondHeight = straightQrBox.bottomRight.y - straightQrBox.topRight.y,
                                    rotationAngle = 0
                                )

                        val scalingFactor = PointF(
                            scalingFactorQrCode.x * scalingFactorViewport.x,
                            scalingFactorQrCode.y * scalingFactorViewport.y
                            )

                        if (pageTemplate != null) {
                            matchFound = true

                            val qrTopLeft = cornerPoints[0]
                            val template = pageTemplate.pageDimensions

                            // the page template is offset from the location of the QR code
                            val pageTemplateBoundingBox = RocketBoundingBox(
                                topLeft = PointF(template.topLeft.x + qrTopLeft.x,template.topLeft.y + qrTopLeft.y),
                                topRight = PointF(template.topRight.x + qrTopLeft.x, template.topRight.y + qrTopLeft.y),
                                bottomRight = PointF(template.bottomRight.x + qrTopLeft.x, template.bottomRight.y + qrTopLeft.y),
                                bottomLeft = PointF(template.bottomLeft.x + qrTopLeft.x, template.bottomLeft.y + qrTopLeft.y)
                            )

                            val scaledBoundingBox = pageTemplateBoundingBox.scale(scalingFactor.x, scalingFactor.y)
                            qrBoundingBox = qrBoundingBox.scale(scalingFactor.x, scalingFactor.y)

                            pageBoundingBox = scaledBoundingBox

                            Log.d("IQrCodeDetector", "pageBoundingBox: ${pageBoundingBox}")
                        }
                    }

                    val result = TemplateMatchResponse(
                        matchFound = matchFound,
                        qrCode = qrCode,
                        pageTemplate = pageTemplate,
                        pageOverlayPath = pageBoundingBox,
                        qrCodeOverlayPath = qrBoundingBox
                    )

                    listener?.onDetectionSuccess(result)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun toPointArray(points: Array<out Point>?): Array<Point> {
        return points?.toList()?.toTypedArray() ?: arrayOf()
    }

}