package au.com.gman.bottlerocket
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class PageCaptureOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private var boundingBox: Path? = null

    fun setOverlayPath(path: Path?) {
        boundingBox = path
        // Invalidate the view to trigger a redraw
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        boundingBox?.let { canvas.drawPath(it, paint) }
    }
}
