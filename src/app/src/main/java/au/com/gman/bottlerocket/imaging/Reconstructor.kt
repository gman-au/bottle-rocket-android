package au.com.gman.bottlerocket.imaging

import android.graphics.PointF
import android.graphics.Rect
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Reconstructs stable corner points using:
 * - Stable center from bounding box
 * - Actual dimensions from detected corner points (preserves perspective)
 * - Rotation angle from corner points
 */
fun Rect.reconstructStableCornersWithPerspective(
    originalCorners: RocketBoundingBox,
    rotationAngleDegrees: Float
): RocketBoundingBox {
    // Use stable center from bounding box
    val centerX = exactCenterX()
    val centerY = exactCenterY()

    // Calculate actual dimensions from the detected corner points (not the bounding box!)
    // This preserves the perspective-corrected dimensions
    val topEdgeLength = sqrt(
        (originalCorners.topRight.x - originalCorners.topLeft.x).let { it * it } +
                (originalCorners.topRight.y - originalCorners.topLeft.y).let { it * it }
    )
    val leftEdgeLength = sqrt(
        (originalCorners.bottomLeft.x - originalCorners.topLeft.x).let { it * it } +
                (originalCorners.bottomLeft.y - originalCorners.topLeft.y).let { it * it }
    )

    // Use the actual dimensions (half for offset from center)
    val halfWidth = topEdgeLength / 2f
    val halfHeight = leftEdgeLength / 2f

    // Convert rotation to radians
    val angleRad = Math.toRadians(rotationAngleDegrees.toDouble())
    val cosAngle = cos(angleRad).toFloat()
    val sinAngle = sin(angleRad).toFloat()

    // Define corners in local space (relative to center)
    val localCorners = listOf(
        PointF(-halfWidth, -halfHeight), // top-left
        PointF(halfWidth, -halfHeight),  // top-right
        PointF(halfWidth, halfHeight),   // bottom-right
        PointF(-halfWidth, halfHeight)   // bottom-left
    )

    // Rotate and translate corners to world space
    val rotatedCorners = localCorners.map { local ->
        PointF(
            centerX + (local.x * cosAngle - local.y * sinAngle),
            centerY + (local.x * sinAngle + local.y * cosAngle)
        )
    }

    return RocketBoundingBox(
        topLeft = rotatedCorners[0],
        topRight = rotatedCorners[1],
        bottomRight = rotatedCorners[2],
        bottomLeft = rotatedCorners[3]
    )
}

/**
 * Blends the stable center position with the original corner positions.
 * Preserves the exact shape/dimensions while stabilizing the center point.
 *
 * RECOMMENDED: This approach maintains perspective while reducing jitter.
 */
fun RocketBoundingBox.stabilizeCenterOnly(
    boundingBox: Rect,
    blendFactor: Float = 0.8f // How much to trust the bounding box center
): RocketBoundingBox {
    // Calculate original center
    val originalCenterX = (topLeft.x + bottomRight.x) / 2f
    val originalCenterY = (topLeft.y + bottomRight.y) / 2f

    // Stable center from bounding box
    val stableCenterX = boundingBox.exactCenterX()
    val stableCenterY = boundingBox.exactCenterY()

    // Blend the centers
    val blendedCenterX = originalCenterX * (1f - blendFactor) + stableCenterX * blendFactor
    val blendedCenterY = originalCenterY * (1f - blendFactor) + stableCenterY * blendFactor

    // Calculate offset to apply
    val offsetX = blendedCenterX - originalCenterX
    val offsetY = blendedCenterY - originalCenterY

    // Apply offset to all corners (preserves shape, just shifts position)
    return RocketBoundingBox(
        topLeft = PointF(topLeft.x + offsetX, topLeft.y + offsetY),
        topRight = PointF(topRight.x + offsetX, topRight.y + offsetY),
        bottomRight = PointF(bottomRight.x + offsetX, bottomRight.y + offsetY),
        bottomLeft = PointF(bottomLeft.x + offsetX, bottomLeft.y + offsetY)
    )
}

/**
 * Temporal smoothing - averages with previous frame.
 * CRITICAL: Apply this AFTER homography transforms to stabilize amplified jitter.
 */
fun RocketBoundingBox.temporalSmooth(
    previous: RocketBoundingBox?,
    smoothFactor: Float = 0.5f // Increase default for post-homography stabilization
): RocketBoundingBox {
    if (previous == null) return this

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

/**
 * Aggressive stabilization for post-homography results.
 * Uses exponential moving average with complete outlier rejection.
 */
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
        return previous // Return previous frame unchanged
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

/**
 * Alternative: Multi-frame buffer with median filtering.
 * Maintains a rolling buffer and uses the median position for each corner.
 * This provides the most stable results but adds latency.
 */
class RocketBoundingBoxMedianFilter(private val bufferSize: Int = 5) {
    private val buffer = mutableListOf<RocketBoundingBox>()

    fun add(box: RocketBoundingBox): RocketBoundingBox {
        buffer.add(box)
        if (buffer.size > bufferSize) {
            buffer.removeAt(0)
        }

        if (buffer.size < 2) {
            return box
        }

        // Calculate median for each corner coordinate
        return RocketBoundingBox(
            topLeft = PointF(
                buffer.map { it.topLeft.x }.sorted()[buffer.size / 2],
                buffer.map { it.topLeft.y }.sorted()[buffer.size / 2]
            ),
            topRight = PointF(
                buffer.map { it.topRight.x }.sorted()[buffer.size / 2],
                buffer.map { it.topRight.y }.sorted()[buffer.size / 2]
            ),
            bottomRight = PointF(
                buffer.map { it.bottomRight.x }.sorted()[buffer.size / 2],
                buffer.map { it.bottomRight.y }.sorted()[buffer.size / 2]
            ),
            bottomLeft = PointF(
                buffer.map { it.bottomLeft.x }.sorted()[buffer.size / 2],
                buffer.map { it.bottomLeft.y }.sorted()[buffer.size / 2]
            )
        )
    }

    fun reset() {
        buffer.clear()
    }
}

private fun distance(p1: PointF, p2: PointF): Float {
    val dx = p1.x - p2.x
    val dy = p1.y - p2.y
    return sqrt(dx * dx + dy * dy)
}

// ============================================================================
// OLD METHODS (kept for reference, but not recommended for perspective accuracy)
// ============================================================================

/**
 * Reconstructs stable corner points from a bounding box and rotation angle.
 * WARNING: This uses bounding box dimensions which don't account for perspective.
 * Use reconstructStableCornersWithPerspective or stabilizeCenterOnly instead.
 */
@Deprecated("Use reconstructStableCornersWithPerspective or stabilizeCenterOnly instead")
fun Rect.reconstructStableCorners(
    rotationAngleDegrees: Float
): RocketBoundingBox {
    val centerX = exactCenterX()
    val centerY = exactCenterY()
    val width = width().toFloat()
    val height = height().toFloat()
    val halfWidth = width / 2f
    val halfHeight = height / 2f

    val angleRad = Math.toRadians(rotationAngleDegrees.toDouble())
    val cosAngle = cos(angleRad).toFloat()
    val sinAngle = sin(angleRad).toFloat()

    val localCorners = listOf(
        PointF(-halfWidth, -halfHeight),
        PointF(halfWidth, -halfHeight),
        PointF(halfWidth, halfHeight),
        PointF(-halfWidth, halfHeight)
    )

    val rotatedCorners = localCorners.map { local ->
        PointF(
            centerX + (local.x * cosAngle - local.y * sinAngle),
            centerY + (local.x * sinAngle + local.y * cosAngle)
        )
    }

    return RocketBoundingBox(
        topLeft = rotatedCorners[0],
        topRight = rotatedCorners[1],
        bottomRight = rotatedCorners[2],
        bottomLeft = rotatedCorners[3]
    )
}

/**
 * @deprecated Use stabilizeCenterOnly instead for better perspective preservation
 */
@Deprecated("Use stabilizeCenterOnly instead")
fun RocketBoundingBox.blendCorners(
    boundingBox: Rect,
    rotationAngleDegrees: Float,
    blendFactor: Float = 0.7f
): RocketBoundingBox {
    val reconstructed = boundingBox.reconstructStableCorners(rotationAngleDegrees)

    return RocketBoundingBox(
        topLeft = PointF(
            topLeft.x * (1f - blendFactor) + reconstructed.topLeft.x * blendFactor,
            topLeft.y * (1f - blendFactor) + reconstructed.topLeft.y * blendFactor
        ),
        topRight = PointF(
            topRight.x * (1f - blendFactor) + reconstructed.topRight.x * blendFactor,
            topRight.y * (1f - blendFactor) + reconstructed.topRight.y * blendFactor
        ),
        bottomRight = PointF(
            bottomRight.x * (1f - blendFactor) + reconstructed.bottomRight.x * blendFactor,
            bottomRight.y * (1f - blendFactor) + reconstructed.bottomRight.y * blendFactor
        ),
        bottomLeft = PointF(
            bottomLeft.x * (1f - blendFactor) + reconstructed.bottomLeft.x * blendFactor,
            bottomLeft.y * (1f - blendFactor) + reconstructed.bottomLeft.y * blendFactor
        )
    )
}