package au.com.gman.bottlerocket.imaging

import android.graphics.Point
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.TemplateMatchResponse
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import au.com.gman.bottlerocket.interfaces.IQrCodeDetector
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import au.com.gman.bottlerocket.interfaces.ITemplateListener
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class QrCodeDetector @Inject constructor(
    private val qrCodeTemplateMatcher: IQrCodeTemplateMatcher,
    private val pageTemplateRescaler: IPageTemplateRescaler
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
                        val cornerPoints = toPointArray(barcode.cornerPoints)
                        qrBoundingBox = RocketBoundingBox(cornerPoints)

                        if (pageTemplate != null) {
                            matchFound = true
                            val pageTemplateBoundingBox = pageTemplate.pageDimensions

                            // Calculate page overlay path based on QR position and template
                            val rescaledPageOverlayPath = pageTemplateRescaler.rescalePageOverlay(
                                qrCorners = qrBoundingBox,
                                pageTemplateBoundingBox = pageTemplateBoundingBox,
                                imageWidth = imageWidth.toFloat(),
                                imageHeight = imageHeight.toFloat(),
                                previewWidth = previewWidth.toFloat(),
                                previewHeight = previewHeight.toFloat()
                            )
                            pageBoundingBox = rescaledPageOverlayPath
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