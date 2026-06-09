package au.com.gman.bottlerocket.interfaces

import au.com.gman.bottlerocket.domain.RocketBoundingBox
import org.opencv.core.Mat

interface IScribzeeMarkerDetector {
    fun findScribzeeMarkers(
        src: Mat,
        imageWidth: Int,
        imageHeight: Int
    ): List<RocketBoundingBox>
}