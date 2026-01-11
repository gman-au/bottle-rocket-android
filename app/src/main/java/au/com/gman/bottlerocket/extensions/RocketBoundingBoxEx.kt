package au.com.gman.bottlerocket.extensions

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.graphics.Rect
import androidx.core.graphics.toPointF
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.ScaleAndOffset
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun RocketBoundingBox.aggressiveSmooth(
    previous: RocketBoundingBox?,
    smoothFactor: Float = 0.7f,
    maxJumpThreshold: Float = 200f // Completely reject frames with jumps exceeding this
): RocketBoundingBox {
    if (previous == null) return this

    // Check if ANY corner has an unreasonable jump (likely homography instability)
    val jumps = listOf(
        distance(topLeft, previous.topLeft),
        distance(topRight, previous.topRight),
        distance(bottomRight, previous.bottomRight),
        distance(bottomLeft, previous.bottomLeft)
    )

    val maxJump = jumps.maxOrNull() ?: 0f

    // If ANY corner jumps too far, completely reject this frame
    if (maxJump > maxJumpThreshold) {
        return previous
    }

    // Normal smoothing for valid frames
    return RocketBoundingBox(
        topLeft = PointF(
            topLeft.x * (1f - smoothFactor) + previous.topLeft.x * smoothFactor,
            topLeft.y * (1f - smoothFactor) + previous.topLeft.y * smoothFactor
        ),
        topRight = PointF(
            topRight.x * (1f - smoothFactor) + previous.topRight.x * smoothFactor,
            topRight.y * (1f - smoothFactor) + previous.topRight.y * smoothFactor
        ),
        bottomRight = PointF(
            bottomRight.x * (1f - smoothFactor) + previous.bottomRight.x * smoothFactor,
            bottomRight.y * (1f - smoothFactor) + previous.bottomRight.y * smoothFactor
        ),
        bottomLeft = PointF(
            bottomLeft.x * (1f - smoothFactor) + previous.bottomLeft.x * smoothFactor,
            bottomLeft.y * (1f - smoothFactor) + previous.bottomLeft.y * smoothFactor
        )
    )
}

fun RocketBoundingBox.scaleUpWithOffset(scaleAndOffset: ScaleAndOffset): RocketBoundingBox {
    val offsetInSourceSpace = PointF(
        -scaleAndOffset.offset.x / scaleAndOffset.scale.x,
        -scaleAndOffset.offset.y / scaleAndOffset.scale.y
    )

    return RocketBoundingBox(
        topLeft = PointF(
            (topLeft.x - offsetInSourceSpace.x) * scaleAndOffset.scale.x,
            (topLeft.y - offsetInSourceSpace.y) * scaleAndOffset.scale.y,
        ),
        topRight = PointF(
            (topRight.x - offsetInSourceSpace.x) * scaleAndOffset.scale.x,
            (topRight.y - offsetInSourceSpace.y) * scaleAndOffset.scale.y,
        ),
        bottomRight = PointF(
            (bottomRight.x - offsetInSourceSpace.x) * scaleAndOffset.scale.x,
            (bottomRight.y - offsetInSourceSpace.y) * scaleAndOffset.scale.y,
        ),
        bottomLeft = PointF(
            (bottomLeft.x - offsetInSourceSpace.x) * scaleAndOffset.scale.x,
            (bottomLeft.y - offsetInSourceSpace.y) * scaleAndOffset.scale.y,
        )
    )
}

fun RocketBoundingBox.toRect(): Rect {
    return Rect(
        topLeft.x.toInt(), topLeft.y.toInt(),
        bottomRight.x.toInt(), bottomRight.y.toInt()
    )
}

fun RocketBoundingBox.toPath(): Path {
    val path = Path()

    path.moveTo(topLeft.x, topLeft.y)
    path.lineTo(topRight.x, topRight.y)
    path.lineTo(bottomRight.x, bottomRight.y)
    path.lineTo(bottomLeft.x, bottomLeft.y)
    path.close()

    return path
}

fun RocketBoundingBox.fillFromBottom(fillPercentage: Float): RocketBoundingBox {
    val clampedPercentage = fillPercentage.coerceIn(0f, 1f)

    val newTopLeft = PointF(
        bottomLeft.x + (topLeft.x - bottomLeft.x) * clampedPercentage,
        bottomLeft.y + (topLeft.y - bottomLeft.y) * clampedPercentage
    )

    val newTopRight = PointF(
        bottomRight.x + (topRight.x - bottomRight.x) * clampedPercentage,
        bottomRight.y + (topRight.y - bottomRight.y) * clampedPercentage
    )

    return RocketBoundingBox(
        topLeft = newTopLeft,
        topRight = newTopRight,
        bottomRight = bottomRight,
        bottomLeft = bottomLeft
    )
}

fun RocketBoundingBox.round(): RocketBoundingBox {
    return RocketBoundingBox(
        topLeft = Point(topLeft.x.roundToInt(), topLeft.y.roundToInt()).toPointF(),
        topRight = Point(topRight.x.roundToInt(), topRight.y.roundToInt()).toPointF(),
        bottomRight = Point(bottomRight.x.roundToInt(), bottomRight.y.roundToInt()).toPointF(),
        bottomLeft = Point(bottomLeft.x.roundToInt(), bottomLeft.y.roundToInt()).toPointF()
    )
}

fun toPointArray(points: Array<out Point>?): Array<Point> {
    return points?.toList()?.toTypedArray() ?: arrayOf()
}


fun RocketBoundingBox.toApiString() : String = buildString {
    append("${topLeft.x},${topLeft.y},")
    append("${topRight.x},${topRight.y},")
    append("${bottomRight.x},${bottomRight.y},")
    append("${bottomLeft.x},${bottomLeft.y}")
}

fun RocketBoundingBox.isOutOfBounds(
    bounds: PointF
): Boolean {
    return this.topLeft.isOutOfBounds(bounds) ||
            this.topRight.isOutOfBounds(bounds) ||
            this.bottomRight.isOutOfBounds(bounds) ||
            this.bottomLeft.isOutOfBounds(bounds)
}

fun RocketBoundingBox.matchQrToOverlayTransform(pageOverlay: RocketBoundingBox) : RocketBoundingBox {
    val topWidth = distance(pageOverlay.topLeft, pageOverlay.topRight)
    val bottomWidth = distance(pageOverlay.bottomLeft, pageOverlay.bottomRight)
    val leftHeight = distance(pageOverlay.topLeft, pageOverlay.bottomLeft)
    val rightHeight = distance(pageOverlay.topRight, pageOverlay.bottomRight)

    val avgWidth = (topWidth + bottomWidth) / 2f
    val avgHeight = (leftHeight + rightHeight) / 2f

    val maxDimension = 2048
    val scale = if (avgWidth > avgHeight) {
        maxDimension / avgWidth
    } else {
        maxDimension / avgHeight
    }

    val outputWidth = (avgWidth * scale).toInt()
    val outputHeight = (avgHeight * scale).toInt()

    val srcCorners = floatArrayOf(
        pageOverlay.topLeft.x, pageOverlay.topLeft.y,
        pageOverlay.topRight.x, pageOverlay.topRight.y,
        pageOverlay.bottomRight.x, pageOverlay.bottomRight.y,
        pageOverlay.bottomLeft.x, pageOverlay.bottomLeft.y
    )

    val dstCorners = floatArrayOf(
        0f, 0f,
        outputWidth.toFloat(), 0f,
        outputWidth.toFloat(), outputHeight.toFloat(),
        0f, outputHeight.toFloat()
    )

    val transformMatrix = Matrix()
    transformMatrix.setPolyToPoly(srcCorners, 0, dstCorners, 0, 4)

    // Transform the QR box using the same matrix
    val qrBoxTransformed = this.let { qrBox ->
        val points = floatArrayOf(
            qrBox.topLeft.x, qrBox.topLeft.y,
            qrBox.topRight.x, qrBox.topRight.y,
            qrBox.bottomRight.x, qrBox.bottomRight.y,
            qrBox.bottomLeft.x, qrBox.bottomLeft.y
        )
        transformMatrix.mapPoints(points)

        RocketBoundingBox(
            topLeft = PointF(points[0], points[1]),
            topRight = PointF(points[2], points[3]),
            bottomRight = PointF(points[4], points[5]),
            bottomLeft = PointF(points[6], points[7])
        )
    }

    return qrBoxTransformed
}

fun PointF.isOutOfBounds(
    bounds: PointF
): Boolean {
    return this.x < 0 ||
            this.x > bounds.x ||
            this.y < 0 ||
            this.y > bounds.y
}

private fun distance(p1: PointF, p2: PointF): Float {
    val dx = p1.x - p2.x
    val dy = p1.y - p2.y
    return sqrt(dx * dx + dy * dy)
}
