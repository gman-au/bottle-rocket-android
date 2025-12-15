package au.com.gman.bottlerocket
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.toPath
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PageCaptureOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paintRed =
        Paint()
            .apply {
                color = context.getColor(R.color.debug_text)
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }

    private val paintBlue =
            Paint()
                .apply {
                    color = context.getColor(R.color.blue)
                    style = Paint.Style.STROKE
                    strokeWidth = 6f
                }

    private var pageBoundingBox: RocketBoundingBox? = null
    private var qrCodeBoundingBox: RocketBoundingBox? = null

    private var imageReferenceBox: RocketBoundingBox? = null

    private var previewReferenceBox: RocketBoundingBox? = null

    fun setPageOverlayBox(box: RocketBoundingBox?) {
        pageBoundingBox = box
        // Invalidate the view to trigger a redraw
        postInvalidate()
    }

    fun setQrOverlayPath(box: RocketBoundingBox?) {
        qrCodeBoundingBox = box
        // Invalidate the view to trigger a redraw
        postInvalidate()
    }

    fun setImageReferenceBox(box: RocketBoundingBox?) {
        imageReferenceBox = box
        // Invalidate the view to trigger a redraw
        //postInvalidate()
    }

    fun setPreviewReferenceBox(box: RocketBoundingBox?) {
        previewReferenceBox = box
        // Invalidate the view to trigger a redraw
        //postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pageBoundingBox?.let { canvas.drawPath(it.toPath(), paintBlue) }
        qrCodeBoundingBox?.let { canvas.drawPath(it.toPath(), paintRed) }
        //imageReferenceBox?.let { canvas.drawPath(it.toPath(), paintBlue) }
        //previewReferenceBox?.let { canvas.drawPath(it.toPath(), paintBlue) }
    }
}
