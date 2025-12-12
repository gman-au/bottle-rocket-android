package au.com.gman.bottlerocket.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import au.com.gman.bottlerocket.domain.QRTemplateInfo
import au.com.gman.bottlerocket.interfaces.IImageProcessor
import au.com.gman.bottlerocket.interfaces.IQrCodeTemplateMatcher
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

class ImageProcessor @Inject constructor(
    private val templateMapper: IQrCodeTemplateMatcher
): IImageProcessor {

    companion object {
        private const val TAG = "ImageProcessor"
    }

    override fun parseQRCode(qrData: String): QRTemplateInfo {
        val parts = qrData.replace(" ", "")

        val position = if (parts.startsWith("P01")) "LEFT" else "RIGHT"
        val version = parts.substringAfter("P0").take(3)
        val typeMatch = Regex("[FT]+\\d+").find(parts)
        val type = typeMatch?.value ?: "UNKNOWN"
        val sequence = parts.substringAfter("S", "000")

        return QRTemplateInfo(position, version, type, sequence)
    }

    override fun enhanceImage(bitmap: Bitmap): Bitmap {
        val enhanced = bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val contrastFactor = 1.2f
        val offset = 0f

        val cm = ColorMatrix(floatArrayOf(
            contrastFactor, 0f, 0f, 0f, offset,
            0f, contrastFactor, 0f, 0f, offset,
            0f, 0f, contrastFactor, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        ))

        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)

        val canvas = Canvas(enhanced)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return enhanced
    }

    override fun processImage(bitmap: Bitmap, qrData: String): Bitmap {
        val templateInfo = parseQRCode(qrData)
        return enhanceImage(bitmap)
    }

    /**
     * Process image with QR bounding box for cropping and perspective correction
     */
    override fun processImageWithQR(
        bitmap: Bitmap,
        qrData: String,
        qrBoundingBox: Rect?
    ): Bitmap {
        if (qrBoundingBox == null) {
            Log.w(TAG, "No QR bounding box provided, using basic processing")
            return processImage(bitmap, qrData)
        }

        val templateInfo = parseQRCode(qrData)

        // Check if this is a 04o template (500x500 box up and right from QR)
        if (qrData.contains("04o", ignoreCase = true)) {
            return cropFromQRPosition(bitmap, qrBoundingBox, 500, 500, "UP_RIGHT")
        }

        // Add more template-specific handling here
        when (templateInfo.position) {
            "LEFT" -> {
                // QR in bottom-left, page extends up and right
                return cropPageFromBottomLeft(bitmap, qrBoundingBox, templateInfo)
            }
            "RIGHT" -> {
                // QR in bottom-right, page extends up and left
                return cropPageFromBottomRight(bitmap, qrBoundingBox, templateInfo)
            }
            else -> {
                return processImage(bitmap, qrData)
            }
        }
    }

    /**
     * Crop a specific sized box relative to QR position
     */
    private fun cropFromQRPosition(
        bitmap: Bitmap,
        qrBox: Rect,
        width: Int,
        height: Int,
        direction: String
    ): Bitmap {
        // Calculate crop region based on direction
        val cropRect = when (direction) {
            "UP_LEFT" -> {
                // Box extends up and left from QR
                val left = max(0, qrBox.left - width)
                val top = max(0, qrBox.top - height)
                val right = min(bitmap.width, qrBox.left)
                val bottom = min(bitmap.height, qrBox.top)
                Rect(left, top, right, bottom)
            }
            "UP_RIGHT" -> {
                val left = max(0, qrBox.right)
                val top = max(0, qrBox.top - height)
                val right = min(bitmap.width, qrBox.right + width)
                val bottom = min(bitmap.height, qrBox.top)
                Rect(left, top, right, bottom)
            }
            "DOWN_LEFT" -> {
                val left = max(0, qrBox.left - width)
                val top = max(0, qrBox.bottom)
                val right = min(bitmap.width, qrBox.left)
                val bottom = min(bitmap.height, qrBox.bottom + height)
                Rect(left, top, right, bottom)
            }
            "DOWN_RIGHT" -> {
                val left = max(0, qrBox.right)
                val top = max(0, qrBox.bottom)
                val right = min(bitmap.width, qrBox.right + width)
                val bottom = min(bitmap.height, qrBox.bottom + height)
                Rect(left, top, right, bottom)
            }
            else -> return bitmap
        }

        Log.d(TAG, "Cropping $direction: QR at (${qrBox.left},${qrBox.top}), Crop: $cropRect")

        // Validate crop dimensions
        if (cropRect.width() <= 0 || cropRect.height() <= 0) {
            Log.w(TAG, "Invalid crop dimensions, returning original")
            return bitmap
        }

        // Crop the bitmap
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )

        // Scale to exact size if needed
        val scaledBitmap = if (croppedBitmap.width != width || croppedBitmap.height != height) {
            Bitmap.createScaledBitmap(croppedBitmap, width, height, true)
        } else {
            croppedBitmap
        }

        // Enhance and return
        return enhanceImage(scaledBitmap)
    }

    /**
     * Crop full page from bottom-left QR position
     * Assumes standard letter size ratio
     */
    private fun cropPageFromBottomLeft(
        bitmap: Bitmap,
        qrBox: Rect,
        templateInfo: QRTemplateInfo
    ): Bitmap {
        // Estimate page dimensions based on QR size
        // Typical Rocketbook QR is about 0.5" square on an 8.5x11" page
        val qrSizePixels = qrBox.width()
        val estimatedPageWidthPixels = (qrSizePixels * 17).toInt() // 8.5" / 0.5"
        val estimatedPageHeightPixels = (qrSizePixels * 22).toInt() // 11" / 0.5"

        // Page extends up and to the right from QR position
        val pageLeft = max(0, qrBox.left - (qrSizePixels * 2)) // Small margin
        val pageTop = max(0, qrBox.top - estimatedPageHeightPixels + (qrSizePixels * 2))
        val pageRight = min(bitmap.width, pageLeft + estimatedPageWidthPixels)
        val pageBottom = min(bitmap.height, qrBox.bottom + (qrSizePixels * 2))

        val cropRect = Rect(pageLeft, pageTop, pageRight, pageBottom)

        Log.d(TAG, "Page crop (bottom-left): $cropRect")

        if (cropRect.width() <= 0 || cropRect.height() <= 0) {
            return bitmap
        }

        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )

        return enhanceImage(croppedBitmap)
    }

    /**
     * Crop full page from bottom-right QR position
     */
    private fun cropPageFromBottomRight(
        bitmap: Bitmap,
        qrBox: Rect,
        templateInfo: QRTemplateInfo
    ): Bitmap {
        val qrSizePixels = qrBox.width()
        val estimatedPageWidthPixels = (qrSizePixels * 17).toInt()
        val estimatedPageHeightPixels = (qrSizePixels * 22).toInt()

        // Page extends up and to the left from QR position
        val pageRight = min(bitmap.width, qrBox.right + (qrSizePixels * 2))
        val pageLeft = max(0, pageRight - estimatedPageWidthPixels)
        val pageTop = max(0, qrBox.top - estimatedPageHeightPixels + (qrSizePixels * 2))
        val pageBottom = min(bitmap.height, qrBox.bottom + (qrSizePixels * 2))

        val cropRect = Rect(pageLeft, pageTop, pageRight, pageBottom)

        Log.d(TAG, "Page crop (bottom-right): $cropRect")

        if (cropRect.width() <= 0 || cropRect.height() <= 0) {
            return bitmap
        }

        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )

        return enhanceImage(croppedBitmap)
    }

    /**
     * Apply perspective correction to unwarp a skewed page
     * This uses a simple 4-point perspective transform
     */
    fun applyPerspectiveCorrection(
        bitmap: Bitmap,
        corners: FloatArray
    ): Bitmap {
        // corners should be [x0,y0, x1,y1, x2,y2, x3,y3] for the 4 corners
        if (corners.size != 8) {
            Log.w(TAG, "Invalid corners array for perspective correction")
            return bitmap
        }

        // Calculate destination rectangle (straight rectangle)
        val destWidth = 1000 // Target width
        val destHeight = 1300 // Target height (letter aspect ratio)

        val dest = floatArrayOf(
            0f, 0f,                          // top-left
            destWidth.toFloat(), 0f,         // top-right
            destWidth.toFloat(), destHeight.toFloat(), // bottom-right
            0f, destHeight.toFloat()          // bottom-left
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(corners, 0, dest, 0, 4)

        val correctedBitmap = Bitmap.createBitmap(
            destWidth,
            destHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(correctedBitmap)
        canvas.drawBitmap(bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))

        return correctedBitmap
    }

    /**
     * Rotate image based on QR position to ensure correct orientation
     */
    fun rotateToCorrectOrientation(
        bitmap: Bitmap,
        qrPosition: String
    ): Bitmap {
        val rotationDegrees = when (qrPosition) {
            "LEFT" -> 0f   // Already correct if QR is bottom-left
            "RIGHT" -> 0f  // Already correct if QR is bottom-right
            else -> 0f
        }

        if (rotationDegrees == 0f) {
            return bitmap
        }

        val matrix = Matrix()
        matrix.postRotate(rotationDegrees)

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
}