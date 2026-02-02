package au.com.gman.bottlerocket.edgeDetection

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

class ScribzeeMarkerDetector @Inject constructor() : IEdgeDetector {

    companion object {
        private const val TAG = "ScribzeeDetector"
        private const val CORNER_BOX_RATIO = 0.3
        private const val MIN_MARKER_AREA = 200.0
        private const val MAX_MARKER_AREA = 8000.0
    }

    override fun detectEdges(src: Mat, lookFor: Int): List<org.opencv.core.Point>? {
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        // Blur to reduce noise
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        // Binary threshold
        val binary = Mat()
        Imgproc.adaptiveThreshold(
            gray, binary, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            11, 2.0
        )

        val width = src.width()
        val height = src.height()
        val boxWidth = (width * CORNER_BOX_RATIO).toInt()
        val boxHeight = (height * CORNER_BOX_RATIO).toInt()

        val cornerRegions = listOf(
            Rect(0, 0, boxWidth, boxHeight),
            Rect(width - boxWidth, 0, boxWidth, boxHeight),
            Rect(0, height - boxHeight, boxWidth, boxHeight),
            Rect(width - boxWidth, height - boxHeight, boxWidth, boxHeight)
        )

        val detectedMarkers = mutableListOf<org.opencv.core.Point?>()

        cornerRegions.forEachIndexed { index, region ->
            val marker = findHexagonInRegion(binary, region, index)
            detectedMarkers.add(marker)
        }

        gray.release()
        binary.release()

        return if (detectedMarkers.all { it != null }) {
            Log.d(TAG, "All 4 markers detected!")
            detectedMarkers.filterNotNull()
        } else {
            Log.d(TAG, "Only ${detectedMarkers.count { it != null }}/4 markers found")
            null
        }
    }

    private fun findHexagonInRegion(
        binary: Mat,
        region: Rect,
        cornerIndex: Int
    ): org.opencv.core.Point? {
        val roi = Mat(binary, region)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()

        Imgproc.findContours(
            roi,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        hierarchy.release()

        var bestMatch: org.opencv.core.Point? = null
        var bestScore = Double.MAX_VALUE

        for (contour in contours) {
            val area = Imgproc.contourArea(contour)

            // Filter by area
            if (area < MIN_MARKER_AREA || area > MAX_MARKER_AREA) continue

            // Approximate polygon
            val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.04 * peri, true)

            val numSides = approx.total().toInt()

            // Look for 4-7 sided polygon (covers different zoom levels)
            if (numSides >= 4 && numSides <= 7) {
                // Additional check: should be roughly square-ish
                val rect = Imgproc.boundingRect(contour)
                val aspectRatio = rect.width.toDouble() / rect.height.toDouble()

                if (aspectRatio < 0.5 || aspectRatio > 2.0) {
                    approx.release()
                    continue
                }

                // Get centroid
                val moments = Imgproc.moments(contour)
                val cx = moments.m10 / moments.m00
                val cy = moments.m01 / moments.m00

                // Prefer markers closer to the actual corner of the region
                val distanceFromCorner = calculateCornerDistance(cx, cy, region, roi.width(), roi.height())

                if (distanceFromCorner < bestScore) {
                    bestScore = distanceFromCorner
                    bestMatch = org.opencv.core.Point(
                        region.x + cx,
                        region.y + cy
                    )
                    Log.d(TAG, "Corner $cornerIndex: candidate with $numSides sides, area=$area, aspect=${"%.2f".format(aspectRatio)}")
                }
            }

            approx.release()
        }

        roi.release()

        if (bestMatch != null) {
            Log.d(TAG, "Corner $cornerIndex: FOUND at (${"%.1f".format(bestMatch.x)}, ${"%.1f".format(bestMatch.y)})")
        } else {
            Log.d(TAG, "Corner $cornerIndex: NOT FOUND")
        }

        return bestMatch
    }

    private fun calculateCornerDistance(
        x: Double,
        y: Double,
        region: Rect,
        roiWidth: Int,
        roiHeight: Int
    ): Double {
        return when {
            region.x == 0 && region.y == 0 -> x + y  // Top-left
            region.x > 0 && region.y == 0 -> (roiWidth - x) + y  // Top-right
            region.x == 0 && region.y > 0 -> x + (roiHeight - y)  // Bottom-left
            else -> (roiWidth - x) + (roiHeight - y)  // Bottom-right
        }
    }
}
