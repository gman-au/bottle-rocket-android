package au.com.gman.bottlerocket.domain

import android.graphics.PointF

data class ScaleAndOffset(
    val scale: PointF,
    val offset: PointF
) {
    override fun toString(): String = buildString {
        appendLine("[Scale: ${scale.x}F, ${scale.y}F],")
        appendLine("[Offset: ${offset.x}F, ${offset.y}F]")
    }
}
