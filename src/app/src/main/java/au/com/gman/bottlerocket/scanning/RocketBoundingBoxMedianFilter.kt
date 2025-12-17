package au.com.gman.bottlerocket.scanning

import android.graphics.PointF
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.interfaces.IRocketBoundingBoxMedianFilter
import javax.inject.Inject

class RocketBoundingBoxMedianFilter @Inject constructor() : IRocketBoundingBoxMedianFilter {

    private val buffer = mutableListOf<RocketBoundingBox>()

    private val bufferSize: Int = 5

    override fun add(box: RocketBoundingBox): RocketBoundingBox {
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

    override fun reset() {
        buffer.clear()
    }
}