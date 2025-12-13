package au.com.gman.bottlerocket
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.domain.toPath

class PageCaptureOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint().apply {
        color = context.getColor(R.color.debug_text)
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private var pageBoundingBox: RocketBoundingBox? = null
    private var qrCodeBoundingBox: RocketBoundingBox? = null

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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        pageBoundingBox?.let { canvas.drawPath(it.toPath(), paint) }
        qrCodeBoundingBox?.let { canvas.drawPath(it.toPath(), paint) }
    }
}
