package au.com.gman.bottlerocket.qrCode

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.interfaces.IQrPositionalValidator
import javax.inject.Inject

class QrPositionalValidator @Inject constructor() : IQrPositionalValidator {
    override fun isBoxInsideBox(innerBox: RocketBoundingBox, outerBox: RocketBoundingBox): Boolean {
        // Check if all 4 corners of inner box are inside the outer box
        val innerPoints = listOf(
            innerBox.topLeft,
            innerBox.topRight,
            innerBox.bottomRight,
            innerBox.bottomLeft
        )

        return innerPoints.all { point ->
            isPointInsideQuadrilateral(point, outerBox)
        }
    }

    private fun isPointInsideQuadrilateral(point: PointF, quad: RocketBoundingBox): Boolean {
        // Use cross product method to check if point is inside quadrilateral
        val vertices = listOf(
            quad.topLeft,
            quad.topRight,
            quad.bottomRight,
            quad.bottomLeft
        )

        // Check if point is on the same side of all edges
        var sign: Boolean? = null

        for (i in vertices.indices) {
            val v1 = vertices[i]
            val v2 = vertices[(i + 1) % vertices.size]

            val cross = (v2.x - v1.x) * (point.y - v1.y) - (v2.y - v1.y) * (point.x - v1.x)
            val currentSign = cross >= 0

            if (sign == null) {
                sign = currentSign
            } else if (sign != currentSign) {
                return false
            }
        }

        return true
    }
}