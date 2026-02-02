package au.com.gman.bottlerocket.edgeDetection

import android.util.Log
import au.com.gman.bottlerocket.injection.TheContourPointDetector
import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

class ScribzeeMarkerDetector @Inject constructor(
    @TheContourPointDetector private val edgeDetector: IEdgeDetector
) : IEdgeDetector {

    companion object {
        private const val TAG = "ScribzeeDetector"
        private const val CORNER_BOX_RATIO = 0.3
        private const val MIN_MARKER_AREA = 200.0
        private const val MAX_MARKER_AREA = 8000.0
    }

    override fun detectEdges(src: Mat, lookFor: Int): List<Point>? {
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

        val detectedMarkers = mutableListOf<Point?>()

        cornerRegions.forEachIndexed { index, region ->
            val submat = src.submat(region)
            val marker = edgeDetector.detectEdges(submat, 4)
            if (marker?.size == 4) {
                detectedMarkers.add(marker[0])
            }
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
}


