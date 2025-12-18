package au.com.gman.bottlerocket.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.BarcodeDetectionResult
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import au.com.gman.bottlerocket.extensions.applyRotation
import au.com.gman.bottlerocket.extensions.scaleUpWithOffset
import au.com.gman.bottlerocket.extensions.toPath
import au.com.gman.bottlerocket.interfaces.IBitmapRescaler
import au.com.gman.bottlerocket.interfaces.IImageEnhancer
import au.com.gman.bottlerocket.interfaces.IScreenDimensions
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ImageEnhancer @Inject constructor(
    private val bitmapRescaler: IBitmapRescaler,
    private val screenDimensions: IScreenDimensions
) : IImageEnhancer {

    companion object {
        private const val TAG = "ImageEnhancer"
    }

    override fun processImageWithMatchedTemplate(
        bitmap: Bitmap,
        detectionResult: BarcodeDetectionResult
    ): Bitmap? {

        if (!detectionResult.matchFound ||
            detectionResult.pageOverlayPath == null ||
            detectionResult.pageTemplate == null
        ) {
            return null
        }

        var viewportOverlayPath = detectionResult.pageOverlayPath

        val totalRotation = detectionResult.cameraRotation - detectionResult.boundingBoxRotation

        val rotatedBitmap =
            bitmap
                .rotate(detectionResult.cameraRotation)
//                .rotate(totalRotation)

        var bitmapScalingFactor =
            bitmapRescaler
                .calculateScalingFactor(
                    sourceWidth = screenDimensions.getTargetSize()!!.x,
                    sourceHeight = screenDimensions.getTargetSize()!!.y,
                    targetWidth = rotatedBitmap.width.toFloat(),
                    targetHeight = rotatedBitmap.height.toFloat(),

                )
        var previousScreenDimensions = screenDimensions.getTargetSize()
        Log.d(TAG, "previousScreenDimensions: ${previousScreenDimensions!!.x}x${previousScreenDimensions!!.y}")
        Log.d(TAG, "rotatedBitmap: ${rotatedBitmap.width}x${rotatedBitmap.height}")

        Log.d(TAG, "viewportOverlayPath: $viewportOverlayPath")
        Log.d(TAG, "bitmapScalingFactor: $bitmapScalingFactor")

        bitmapScalingFactor = ScaleAndOffset(
            scale = PointF(2F, 2F),
            offset = PointF(0F, 0F)
        )

        var overlayToDraw = detectionResult.pageOverlayPath
            //.scaleUpWithOffset(bitmapScalingFactor)
            //.applyRotation(-detectionResult.boundingBoxRotation)
        //overlayToDraw = overlayToDraw.applyRotation(-detectionResult.boundingBoxRotation)

        /*overlayToDraw =
            detectionResult
                .pageOverlayPath
                .rotateAroundCenter(
                    degrees = totalRotation,
                    bitmapWidth = bitmap.width,
                    bitmapHeight = bitmap.height
                )*/

        //overlayToDraw = overlayToDraw.scaleUpWithOffset(bitmapScalingFactor)

        val correctedBitmap =
            rotatedBitmap
        /*.extractPageWithPerspective(
            pageBounds = rotatedPageBounds,
            targetDimensions = detectionResult.pageTemplate.pageDimensions
        )*/

        val enhancedBitmap =
            correctedBitmap
                .enhanceImage(
                    detectionResult.qrCodeOverlayPath,
                    overlayToDraw
                )


        var pageTemplateBox = detectionResult.pageOverlayPath

        return enhancedBitmap
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun Bitmap.enhanceImage(
        qrBoundingBox: RocketBoundingBox? = null,
        pageBoundingBox: RocketBoundingBox? = null,
    ): Bitmap {

        val enhanced =
            this
                .copy(Bitmap.Config.ARGB_8888, true)

        val contrastFactor = 1.2f
        val offset = 0f

        val cm = ColorMatrix(
            floatArrayOf(
                contrastFactor, 0f, 0f, 0f, offset,
                0f, contrastFactor, 0f, 0f, offset,
                0f, 0f, contrastFactor, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)

        val canvas = Canvas(enhanced)

        canvas
            .drawBitmap(
                this,
                0f,
                0f,
                paint
            )

        var debugPaint = Paint()
            .apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 8f
            }

        if (qrBoundingBox != null)
            canvas.drawPath(qrBoundingBox.toPath(), debugPaint)

        if (pageBoundingBox != null)
            canvas.drawPath(pageBoundingBox.toPath(), debugPaint)

        return enhanced
    }

    fun Bitmap.extractPageWithPerspective(
        pageBounds: RocketBoundingBox,
        outputWidth: Int,
        outputHeight: Int
    ): Bitmap {
        // Source corners (the distorted quadrilateral in the original image)
        val srcCorners = floatArrayOf(
            pageBounds.topLeft.x, pageBounds.topLeft.y,
            pageBounds.topRight.x, pageBounds.topRight.y,
            pageBounds.bottomRight.x, pageBounds.bottomRight.y,
            pageBounds.bottomLeft.x, pageBounds.bottomLeft.y
        )

        // Destination corners (rectangle in output image)
        val dstCorners = floatArrayOf(
            0f, 0f,                             // top-left
            outputWidth.toFloat(), 0f,          // top-right
            outputWidth.toFloat(), outputHeight.toFloat(), // bottom-right
            0f, outputHeight.toFloat()          // bottom-left
        )

        // Create transformation matrix
        val matrix = Matrix()
        matrix.setPolyToPoly(srcCorners, 0, dstCorners, 0, 4)

        // Create output bitmap
        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Draw the transformed bitmap
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(this, matrix, paint)

        return output
    }

    /**
     * Automatically calculates output dimensions to maintain aspect ratio.
     * Uses the average width/height from the bounding box.
     */
    fun Bitmap.extractPageWithPerspective(
        pageBounds: RocketBoundingBox,
        maxDimension: Int = 2048
    ): Bitmap {
        // Calculate average dimensions from the bounding box
        val topWidth = distance(pageBounds.topLeft, pageBounds.topRight)
        val bottomWidth = distance(pageBounds.bottomLeft, pageBounds.bottomRight)
        val leftHeight = distance(pageBounds.topLeft, pageBounds.bottomLeft)
        val rightHeight = distance(pageBounds.topRight, pageBounds.bottomRight)

        val avgWidth = (topWidth + bottomWidth) / 2f
        val avgHeight = (leftHeight + rightHeight) / 2f

        // Scale to fit within maxDimension while maintaining aspect ratio
        val scale = if (avgWidth > avgHeight) {
            maxDimension / avgWidth
        } else {
            maxDimension / avgHeight
        }

        val outputWidth = (avgWidth * scale).roundToInt()
        val outputHeight = (avgHeight * scale).roundToInt()

        return extractPageWithPerspective(pageBounds, outputWidth, outputHeight)
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return sqrt(dx * dx + dy * dy)
    }
}