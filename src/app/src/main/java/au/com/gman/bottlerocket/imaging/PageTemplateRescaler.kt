package au.com.gman.bottlerocket.imaging

import android.graphics.PointF
import android.util.Log
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import org.opencv.calib3d.Calib3d
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import javax.inject.Inject

class PageTemplateRescaler @Inject constructor() : IPageTemplateRescaler {

    companion object {
        private const val TAG = "PageTemplateRescaler"
    }

    override fun calculatePageBounds(
        qrBoxIdeal: RocketBoundingBox,
        pageBoxIdeal: RocketBoundingBox,
        rotationAngle: Float
    ): RocketBoundingBox {

        val qrSize = 1.0

        // Source: axis-aligned square (what we want to map FROM)
        val srcPts = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(qrSize.toDouble(), 0.0),
            Point(qrSize.toDouble(), qrSize.toDouble()),
            Point(0.0, qrSize.toDouble())
        )

        // Destination: actual QR quadrilateral (what we map TO)
        val dstPts = MatOfPoint2f(
            Point(qrBoxIdeal.topLeft.x.toDouble(), qrBoxIdeal.topLeft.y.toDouble()),
            Point(qrBoxIdeal.topRight.x.toDouble(), qrBoxIdeal.topRight.y.toDouble()),
            Point(qrBoxIdeal.bottomRight.x.toDouble(), qrBoxIdeal.bottomRight.y.toDouble()),
            Point(qrBoxIdeal.bottomLeft.x.toDouble(), qrBoxIdeal.bottomLeft.y.toDouble())
        )

        // Use perspective transform (homography) with 4 points
//        val homography = Calib3d.findHomography(srcPts, dstPts, 0, 0.0)
        val homography = Calib3d.findHomography(srcPts, dstPts)

        Log.d(TAG, "Homography matrix:\n${homography.dump()}")

        // Scale page template by qrSize
        val pageInNormalizedSpace = MatOfPoint2f(
            Point(pageBoxIdeal.topLeft.x.toDouble() * qrSize, pageBoxIdeal.topLeft.y.toDouble() * qrSize),
            Point(pageBoxIdeal.topRight.x.toDouble() * qrSize, pageBoxIdeal.topRight.y.toDouble() * qrSize),
            Point(pageBoxIdeal.bottomRight.x.toDouble() * qrSize, pageBoxIdeal.bottomRight.y.toDouble() * qrSize),
            Point(pageBoxIdeal.bottomLeft.x.toDouble() * qrSize, pageBoxIdeal.bottomLeft.y.toDouble() * qrSize)
        )

        // Apply perspective transform
        val transformedPageMat = MatOfPoint2f()
        org.opencv.core.Core.perspectiveTransform(pageInNormalizedSpace, transformedPageMat, homography)

        val transformedPoints = transformedPageMat.toArray()

        Log.d(TAG, "Page after perspective transform:")
        transformedPoints.forEachIndexed { i, pt ->
            Log.d(TAG, "  [$i]: (${pt.x}, ${pt.y})")
        }

        return RocketBoundingBox(
            topLeft = PointF(transformedPoints[0].x.toFloat(), transformedPoints[0].y.toFloat()),
            topRight = PointF(transformedPoints[1].x.toFloat(), transformedPoints[1].y.toFloat()),
            bottomRight = PointF(transformedPoints[2].x.toFloat(), transformedPoints[2].y.toFloat()),
            bottomLeft = PointF(transformedPoints[3].x.toFloat(), transformedPoints[3].y.toFloat())
        )
    }
}
