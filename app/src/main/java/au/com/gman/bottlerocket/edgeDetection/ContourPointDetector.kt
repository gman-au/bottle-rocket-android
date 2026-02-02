package au.com.gman.bottlerocket.edgeDetection

import au.com.gman.bottlerocket.interfaces.IEdgeDetector
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

class ContourPointDetector @Inject constructor() : IEdgeDetector {
    override fun detectEdges(src: Mat, lookFor: Int): List<Point>? {

        // Convert to grayscale
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        // Blur to reduce noise
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        // Detect edges using Canny
        val edges = Mat()
        Imgproc.Canny(gray, edges, 75.0, 200.0)

        // Find contours
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            edges,
            contours,
            hierarchy,
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        // Sort contours by area, descending
        contours.sortByDescending { Imgproc.contourArea(it) }

        var docContour: MatOfPoint2f? = null

        for (contour in contours) {
            val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.02 * peri, true)

            // Look for a 4-point contour (document shape)
            if (approx.total() == lookFor.toLong()) {
                docContour = approx
                break
            }
        }

        gray.release()
        edges.release()
        //src.release()

        return docContour?.toList()
    }
}