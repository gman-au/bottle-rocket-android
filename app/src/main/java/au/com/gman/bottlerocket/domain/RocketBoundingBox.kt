package au.com.gman.bottlerocket.domain

import android.graphics.Point
import android.graphics.PointF


import android.graphics.RectF
import androidx.core.graphics.toPointF
import au.com.gman.bottlerocket.extensions.toPointArray
import android.graphics.Rect
import org.opencv.core.Rect as cvRect


data class RocketBoundingBox(
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

    constructor(rect: cvRect) : this(
        PointF(rect.tl().x.toFloat(), rect.tl().y.toFloat()),
        PointF(rect.br().x.toFloat(), rect.tl().y.toFloat()),
        PointF(rect.br().x.toFloat(), rect.br().y.toFloat()),
        PointF(rect.tl().x.toFloat(), rect.br().y.toFloat()),
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

    constructor(points: Array<out Point>?) : this(
        toPointArray(points)[0].toPointF(),
        toPointArray(points)[1].toPointF(),
        toPointArray(points)[2].toPointF(),
        toPointArray(points)[3].toPointF()
    )

    constructor(points: Array<PointF>) : this(
        points[0],
        points[1],
        points[2],
        points[3]
    )

    override fun toString(): String = buildString {
        appendLine("${topLeft.x}F, ${topLeft.y}F,")
        appendLine("${topRight.x}F, ${topRight.y}F,")
        appendLine("${bottomRight.x}F, ${bottomRight.y}F,")
        appendLine("${bottomLeft.x}F, ${bottomLeft.y}F")
    }

}

