package au.com.gman.bottlerocket.extensions

import android.graphics.Point
import android.graphics.PointF
import au.com.gman.bottlerocket.domain.RocketBoundingBox

fun PointF.createFallbackSquare(): RocketBoundingBox {
    val centerX = this.x / 2f
    val centerY = this.y / 2f

    val halfSize =
        minOf(this.x, this.y) * 0.25f // 50% of viewport = 25% from center

    return RocketBoundingBox(
        topLeft = PointF(centerX - halfSize, centerY - halfSize),
        topRight = PointF(centerX + halfSize, centerY - halfSize),
        bottomRight = PointF(centerX + halfSize, centerY + halfSize),
        bottomLeft = PointF(centerX - halfSize, centerY + halfSize)
    )
}

fun List<PointF>.orderPointsClockwise(): Array<PointF> {
    val sorted = this.sortedBy { it.y }
    val top = sorted.take(2).sortedBy { it.x }
    val bottom = sorted.takeLast(2).sortedBy { it.x }

    return arrayOf(
        top[0],      // topLeft
        top[1],      // topRight
        bottom[1],   // bottomRight
        bottom[0]    // bottomLeft
    )
}

fun toPointArray(points: Array<out Point>?): Array<Point> {
    return points?.toList()?.toTypedArray() ?: arrayOf()
}

fun PointF.isOutOfBounds(
    bounds: PointF
): Boolean {
    return this.x < 0 ||
            this.x > bounds.x ||
            this.y < 0 ||
            this.y > bounds.y
}