package au.com.gman.bottlerocket.interfaces

import org.opencv.core.Mat
import org.opencv.core.Point

interface IEdgeDetector {
    fun detectEdges(src: Mat, lookFor: Int): List<Point>?
}