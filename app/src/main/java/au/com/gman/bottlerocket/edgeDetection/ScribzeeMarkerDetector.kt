package au.com.gman.bottlerocket.scanning

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.CornerEnum
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.interfaces.IScribzeeMarkerDetector
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

class ScribzeeMarkerDetector @Inject constructor() : IScribzeeMarkerDetector {

    companion object {
        private const val SQUARENESS_THRESHOLD = 0.70
        private const val AREA_MIN_RATIO = 0.00001
        private const val AREA_MAX_RATIO = 0.005
        private const val APPROX_POLY_EPSILON = 0.04
        private const val CHILD_AREA_RATIO_MIN = 0.10
        private const val CHILD_AREA_RATIO_MAX = 0.60
        private val BLOCK_SIZES = listOf(7, 15, 31)
        private val SCALES = listOf(1.0, 0.5)
    }

    override fun findScribzeeMarkers(
        src: Mat,
        imageWidth: Int,
        imageHeight: Int
    ): List<RocketBoundingBox> {

        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(3.0, 3.0), 0.0)

        val allCandidates = mutableListOf<ScoredCandidate>()

        for (scale in SCALES) {
            val scaledGray = Mat()
            if (scale != 1.0) {
                Imgproc.resize(
                    gray, scaledGray,
                    Size(gray.width() * scale, gray.height() * scale)
                )
            } else {
                gray.copyTo(scaledGray)
            }

            val scaledW = (imageWidth * scale).toInt()
            val scaledH = (imageHeight * scale).toInt()

            for (blockSize in BLOCK_SIZES) {
                val thresh = Mat()
                Imgproc.adaptiveThreshold(
                    scaledGray, thresh, 255.0,
                    Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                    Imgproc.THRESH_BINARY_INV,
                    blockSize, 3.0
                )
                allCandidates += detectCandidates(thresh, scaledW, scaledH, scale)
            }
        }

        if (allCandidates.isEmpty()) return emptyList()

        // Remove false positives — filter out anything less than 40% of median area
        val medianArea = allCandidates
            .map { it.area }
            .sorted()
            .let { it[it.size / 2] }

        val filtered = allCandidates.filter { it.area >= medianArea * 0.4 }

        // Deduplicate by corner, keep largest per quadrant
        return filtered
            .groupBy { classifyCorner(it.center, imageWidth, imageHeight) }
            .mapValues { (_, group) -> group.maxByOrNull { it.area }!! }
            .values
            .map { RocketBoundingBox(it.boundingRect) }
    }

    // ------------------------------------------------------------------
    // Contour detection for a single thresh + scale combination
    // ------------------------------------------------------------------

    private fun detectCandidates(
        thresh: Mat,
        imageWidth: Int,
        imageHeight: Int,
        scale: Double
    ): List<ScoredCandidate> {

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            thresh, contours, hierarchy,
            Imgproc.RETR_TREE,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        val areaRange = getMarkerAreaRange(imageWidth, imageHeight)
        val candidates = mutableListOf<ScoredCandidate>()

        contours.forEachIndexed { i, contour ->
            val area = Imgproc.contourArea(contour)
            if (area !in areaRange) return@forEachIndexed

            // Must be roughly square
            val rect = Imgproc.boundingRect(contour)
            val squareness = minOf(rect.width, rect.height).toDouble() /
                    maxOf(rect.width, rect.height).toDouble()
            if (squareness < SQUARENESS_THRESHOLD) return@forEachIndexed

            // Must have a child contour (inner notch/cutout)
            val childIdx = hierarchy.get(0, i)[2].toInt()
            if (childIdx < 0) return@forEachIndexed

            // Inner notch must be proportionally square and correctly sized
            val childContour = contours[childIdx]
            val childArea = Imgproc.contourArea(childContour)
            val childRect = Imgproc.boundingRect(childContour)

            val areaRatio = childArea / area
            if (areaRatio !in CHILD_AREA_RATIO_MIN..CHILD_AREA_RATIO_MAX) return@forEachIndexed

            val childSquareness = minOf(childRect.width, childRect.height).toDouble() /
                    maxOf(childRect.width, childRect.height).toDouble()
            if (childSquareness < SQUARENESS_THRESHOLD) return@forEachIndexed

            // Outer contour must approximate to a quadrilateral
            val contour2f = MatOfPoint2f(*contour.toArray())
            val approx = MatOfPoint2f()
            val peri = Imgproc.arcLength(contour2f, true)
            Imgproc.approxPolyDP(contour2f, approx, APPROX_POLY_EPSILON * peri, true)
            if (approx.rows() != 4) return@forEachIndexed

            // Centroid scaled back to original image coordinates
            val moments = Imgproc.moments(contour)
            val cx = (moments.m10 / moments.m00) / scale
            val cy = (moments.m01 / moments.m00) / scale

            val scaledRect = org.opencv.core.Rect(
                (rect.x / scale).toInt(),
                (rect.y / scale).toInt(),
                (rect.width / scale).toInt(),
                (rect.height / scale).toInt()
            )

            candidates.add(
                ScoredCandidate(
                    center = PointF(cx.toFloat(), cy.toFloat()),
                    area = scaledRect.area().toDouble(),
                    boundingRect = scaledRect
                )
            )
        }

        return candidates
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun getMarkerAreaRange(imageWidth: Int, imageHeight: Int): ClosedRange<Double> {
        val imageArea = imageWidth.toDouble() * imageHeight.toDouble()
        return (imageArea * AREA_MIN_RATIO)..(imageArea * AREA_MAX_RATIO)
    }

    private fun classifyCorner(
        center: PointF,
        imageWidth: Int,
        imageHeight: Int
    ): CornerEnum {
        val midX = imageWidth / 2.0
        val midY = imageHeight / 2.0
        return when {
            center.x < midX && center.y < midY -> CornerEnum.TOP_LEFT
            center.x >= midX && center.y < midY -> CornerEnum.TOP_RIGHT
            center.x < midX && center.y >= midY -> CornerEnum.BOTTOM_LEFT
            else -> CornerEnum.BOTTOM_RIGHT
        }
    }

    // ------------------------------------------------------------------
    // Internal models
    // ------------------------------------------------------------------

    private data class ScoredCandidate(
        val center: PointF,
        val area: Double,
        val boundingRect: Rect
    )
}