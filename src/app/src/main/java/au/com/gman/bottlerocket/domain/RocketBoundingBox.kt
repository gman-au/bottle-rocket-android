package au.com.gman.bottlerocket.domain

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.toPointF
import kotlin.math.atan2

data class RocketBoundingBox (
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
) {
    constructor(source: RocketBoundingBox) : this(
        source.copy().topLeft,
        source.copy().topRight,
        source.copy().bottomRight,
        source.copy().bottomLeft
    )

    constructor(
        topLeftX: Float, topLeftY: Float,
        topRightX: Float, topRightY: Float,
        bottomRightX: Float, bottomRightY: Float,
        bottomLeftX: Float, bottomLeftY: Float
    ) : this(
        PointF(topLeftX, topLeftY),
        PointF(topRightX, topRightY),
        PointF(bottomRightX, bottomRightY),
        PointF(bottomLeftX, bottomLeftY)
    )

    constructor(rect: Rect) : this(
        PointF(rect.left.toFloat(), rect.top.toFloat()),
        PointF(rect.right.toFloat(), rect.top.toFloat()),
        PointF(rect.right.toFloat(), rect.bottom.toFloat()),
        PointF(rect.left.toFloat(), rect.bottom.toFloat())
    )

    constructor(rect: RectF) : this(
        PointF(rect.left, rect.top),
        PointF(rect.right, rect.top),
        PointF(rect.right, rect.bottom),
        PointF(rect.left, rect.bottom)
    )

    constructor(floats: FloatArray) : this(
        floats[0],
        floats[1],
        floats[2],
        floats[3],
        floats[4],
        floats[5],
        floats[6],
        floats[7]
    )

    constructor(points: Array<Point>) : this(
        points[0].toPointF(),
        points[1].toPointF(),
        points[2].toPointF(),
        points[3].toPointF()
    )

    constructor(points: Array<PointF>) : this(
        points[0],
        points[1],
        points[2],
        points[3]
    )

    override fun toString(): String = buildString {
        appendLine("[1]: ${topLeft.x}, ${topLeft.y}")
        appendLine("[2]: ${topRight.x}, ${topRight.y}")
        appendLine("[3]: ${bottomRight.x}, ${bottomRight.y}")
        appendLine("[4]: ${bottomLeft.x}, ${bottomLeft.y}")
    }

}

fun RocketBoundingBox.scale(scaleX: Float, scaleY: Float): RocketBoundingBox {
    return RocketBoundingBox(
        topLeft = PointF(topLeft.x * scaleX, topLeft.y * scaleY),
        topRight = PointF(topRight.x * scaleX, topRight.y * scaleY),
        bottomRight = PointF(bottomRight.x * scaleX, bottomRight.y * scaleY),
        bottomLeft = PointF(bottomLeft.x * scaleX, bottomLeft.y * scaleY)
    )
}

fun RocketBoundingBox.toPointArray(): Array<PointF> {
    return arrayOf(topLeft, topRight, bottomRight, bottomLeft)
}

fun RocketBoundingBox.toPath() : Path {
    val path = Path()

    path.moveTo(topLeft.x, topLeft.y)
    path.lineTo(topRight.x, topRight.y)
    path.lineTo(bottomRight.x, bottomRight.y)
    path.lineTo(bottomLeft.x, bottomLeft.y)
    path.close()

    return path
}

fun RocketBoundingBox.calculateRotationAngle(): Float {
    val deltaX = (topRight.x - topLeft.x)
    val deltaY = (topRight.y - topLeft.y)

    val angleRadians = atan2(deltaY, deltaX)
    val angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()

    return angleDegrees
}

fun RocketBoundingBox.applyRotation(angle: Float, pivot: PointF? = null): RocketBoundingBox {
    val actualPivot = pivot ?: PointF(
        (topLeft.x + bottomRight.x) / 2f,
        (topLeft.y + bottomRight.y) / 2f
    )

    val matrix = Matrix()
    matrix.setRotate(angle, actualPivot.x, actualPivot.y)

    val points = floatArrayOf(
        topLeft.x, topLeft.y,
        topRight.x, topRight.y,
        bottomRight.x, bottomRight.y,
        bottomLeft.x, bottomLeft.y
    )

    matrix.mapPoints(points)

    return RocketBoundingBox(points)
}

fun RocketBoundingBox.normalize(): RocketBoundingBox {
    // Find the minimum x and y (the offset)
    val minX = minOf(topLeft.x, topRight.x, bottomRight.x, bottomLeft.x)
    val minY = minOf(topLeft.y, topRight.y, bottomRight.y, bottomLeft.y)

    // Subtract the offset from all points
    return RocketBoundingBox(
        topLeft = PointF(topLeft.x - minX, topLeft.y - minY),
        topRight = PointF(topRight.x - minX, topRight.y - minY),
        bottomRight = PointF(bottomRight.x - minX, bottomRight.y - minY),
        bottomLeft = PointF(bottomLeft.x - minX, bottomLeft.y - minY)
    )
}