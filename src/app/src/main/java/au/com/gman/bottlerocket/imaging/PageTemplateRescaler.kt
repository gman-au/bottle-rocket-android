package au.com.gman.bottlerocket.imaging

import android.graphics.Matrix
import android.graphics.PointF
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.normalize
import au.com.gman.bottlerocket.domain.toFloatArray
import au.com.gman.bottlerocket.interfaces.IPageTemplateRescaler
import javax.inject.Inject

class PageTemplateRescaler @Inject constructor() : IPageTemplateRescaler {
    override fun calculatePageBounds(
        qrBoxIdeal: RocketBoundingBox,    // Raw barcode corners (camera space)
        qrBoxActual: RocketBoundingBox,   // Scaled result (screen space)
        pageBoxIdeal: RocketBoundingBox   // Template offsets
    ): RocketBoundingBox {

        // Step 1: Normalize the IDEAL QR to get its shape/size
        val normalizedQrIdeal = qrBoxIdeal.normalize()

        // Step 2: Calculate QR dimensions
        val qrWidth = normalizedQrIdeal.topRight.x - normalizedQrIdeal.topLeft.x
        val qrHeight = normalizedQrIdeal.bottomLeft.y - normalizedQrIdeal.topLeft.y

        // Step 3: Scale page template by QR dimensions
        val scaledPageIdeal = RocketBoundingBox(
            topLeft = PointF(pageBoxIdeal.topLeft.x * qrWidth, pageBoxIdeal.topLeft.y * qrHeight),
            topRight = PointF(
                pageBoxIdeal.topRight.x * qrWidth,
                pageBoxIdeal.topRight.y * qrHeight
            ),
            bottomRight = PointF(
                pageBoxIdeal.bottomRight.x * qrWidth,
                pageBoxIdeal.bottomRight.y * qrHeight
            ),
            bottomLeft = PointF(
                pageBoxIdeal.bottomLeft.x * qrWidth,
                pageBoxIdeal.bottomLeft.y * qrHeight
            )
        )

        // Step 4: Create transform from normalized ideal QR to actual QR (keeps position!)
        val matrix = Matrix()

        matrix
            .setPolyToPoly(
                normalizedQrIdeal.toFloatArray(), 0,
                qrBoxActual.toFloatArray(), 0,     // DON'T normalize this!
                4
            )

        // Step 5: Apply transform to scaled page
        val transformedPage = FloatArray(8)

        matrix
            .mapPoints(transformedPage, scaledPageIdeal.toFloatArray())

        return RocketBoundingBox(transformedPage)
    }
}
