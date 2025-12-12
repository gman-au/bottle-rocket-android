package au.com.gman.bottlerocket.imaging

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Point
import android.graphics.RectF
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import au.com.gman.bottlerocket.domain.TemplateMatchResponse
import au.com.gman.bottlerocket.interfaces.IQrCodeDetector
import au.com.gman.bottlerocket.interfaces.ITemplateListener
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject
import kotlin.math.atan2

class QrCodeDetector @Inject constructor(
    private val qrCodeMatcher: QrCodeTemplateMatcher
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
                    val qrCode = barcodes.firstOrNull()
                    var matchedTemplate = qrCodeMatcher.tryMatch(qrCode?.rawValue ?: "")


                    if (qrCode != null && qrCode.boundingBox != null) {
                        val cornerPoints = qrCode.cornerPoints
                        val templateBoundingBox = matchedTemplate.pageTemplate?.pageDimensions

                        if (templateBoundingBox != null && cornerPoints != null) {
                            // Calculate page overlay path based on QR position and template
                            val overlayPath = calculatePageOverlay(
                                cornerPoints,
                                templateBoundingBox,
                                imageWidth,
                                imageHeight,
                                previewWidth,
                                previewHeight
                            )

                            matchedTemplate = TemplateMatchResponse(
                                matchFound = matchedTemplate.matchFound,
                                qrCode = matchedTemplate.qrCode + rotationDegrees,
                                overlay = overlayPath,
                                pageTemplate = matchedTemplate.pageTemplate
                            )
                        }
                    }

                    listener?.onDetectionSuccess(matchedTemplate)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    /**
     * Calculate the rotation angle of the QR code from its corner points
     * Returns angle in degrees
     */
    private fun calculateQRRotationAngle(corners: Array<Point>): Float {
        if (corners.size < 2) return 0f

        // Use top edge (corner 0 to corner 1) to determine rotation
        val topLeft = corners[0]
        val topRight = corners[1]

        val deltaX = (topRight.x - topLeft.x).toFloat()
        val deltaY = (topRight.y - topLeft.y).toFloat()

        // Calculate angle in radians, then convert to degrees
        val angleRadians = atan2(deltaY, deltaX)
        val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()

        return angleDegrees
    }

    private fun calculatePageOverlay(
        qrCorners: Array<android.graphics.Point>,
        pageBoundingBox: RectF,
        imageWidth: Int,
        imageHeight: Int,
        previewWidth: Int,   // Add this parameter
        previewHeight: Int   // Add this parameter
    ): Path {
        val path = Path()

        if (qrCorners.size < 4) return path

        // Calculate scale factors from image space to preview space
        val scaleX = previewWidth.toFloat() / imageWidth
        val scaleY = previewHeight.toFloat() / imageHeight

        android.util.Log.d("OVERLAY", "Scale: ${scaleX}x, ${scaleY}y")

        val qrRotationDegrees = calculateQRRotationAngle(qrCorners)
        android.util.Log.d("OVERLAY", "Rotation: $qrRotationDegrees°")

        val anchorPoint = qrCorners[3]
        android.util.Log.d("OVERLAY", "Anchor (image space): (${anchorPoint.x}, ${anchorPoint.y})")

        // Scale page dimensions to match preview
        val pageCorners = floatArrayOf(
            pageBoundingBox.left * scaleX, pageBoundingBox.top * scaleY,
            pageBoundingBox.right * scaleX, pageBoundingBox.top * scaleY,
            pageBoundingBox.right * scaleX, pageBoundingBox.bottom * scaleY,
            pageBoundingBox.left * scaleX, pageBoundingBox.bottom * scaleY
        )

        val matrix = Matrix()
        matrix.setRotate(qrRotationDegrees)
        matrix.mapPoints(pageCorners)

        // Scale anchor point to preview space
        val scaledAnchorX = anchorPoint.x * scaleX
        val scaledAnchorY = anchorPoint.y * scaleY

        for (i in pageCorners.indices step 2) {
            pageCorners[i] += scaledAnchorX
            pageCorners[i + 1] += scaledAnchorY
        }

        android.util.Log.d("OVERLAY", "Final page corners (preview space):")
        for (i in pageCorners.indices step 2) {
            android.util.Log.d("OVERLAY", "  (${pageCorners[i]}, ${pageCorners[i+1]})")
        }

        path.moveTo(pageCorners[0], pageCorners[1])
        path.lineTo(pageCorners[2], pageCorners[3])
        path.lineTo(pageCorners[4], pageCorners[5])
        path.lineTo(pageCorners[6], pageCorners[7])
        path.close()

        return path
    }

    private fun calculateDistance(p1: android.graphics.Point, p2: android.graphics.Point): Float {
        val dx = (p2.x - p1.x).toFloat()
        val dy = (p2.y - p1.y).toFloat()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

}