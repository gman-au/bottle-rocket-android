package au.com.gman.bottlerocket.domain

import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.toPointF

data class RocketBoundingBox (
    val topLeft: PointF,
    val topRight: PointF,
    val bottomRight: PointF,
    val bottomLeft: PointF
) {
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

fun RocketBoundingBox.scale(scaleX: Float, scaleY: Float): FloatArray {
    return floatArrayOf(
        topLeft.x * scaleX, topLeft.y * scaleY,
        topRight.x * scaleX, topRight.y * scaleY,
        bottomRight.x * scaleX, bottomRight.y * scaleY,
        bottomLeft.x * scaleX, bottomLeft.y * scaleY
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