package au.com.gman.bottlerocket

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import au.com.gman.bottlerocket.domain.CaptureStatusEnum
import au.com.gman.bottlerocket.domain.RocketBoundingBox
import au.com.gman.bottlerocket.extensions.fillFromBottom
import au.com.gman.bottlerocket.extensions.toPath
import au.com.gman.bottlerocket.extensions.toRect
import au.com.gman.bottlerocket.interfaces.ISteadyFrameIndicator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PageCaptureOverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    @Inject
    lateinit var steadyFrameIndicator: ISteadyFrameIndicator

    private var typeface: Typeface? = ResourcesCompat.getFont(context, R.font.jet_brains_mono)

    private var measuredFontSize: Float? = null

    private val paintCaptureStatusGreenBorder =
        Paint()
            .apply {
                color = context.getColor(R.color.capture_status_green_border)
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }

    private val paintCaptureStatusGreenFill =
        Paint()
            .apply {
                color = context.getColor(R.color.capture_status_green_transparent)
                style = Paint.Style.FILL
            }

    private val paintCaptureStatusAmberBorder =
        Paint()
            .apply {
                color = context.getColor(R.color.capture_status_amber_border)
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }

    private val paintCaptureStatusAmberFill =
        Paint()
            .apply {
                color = context.getColor(R.color.capture_status_amber_transparent)
                style = Paint.Style.FILL
            }

    private val paintStatusText =
        Paint()
            .apply {
                color = context.getColor(R.color.white)
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 2f
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

    private fun drawCenter(canvas: Canvas, box: RocketBoundingBox?, paint: Paint, text: String) {
        if (box == null) return
        if (measuredFontSize == null) return

        // Get the bounds of the area you are drawing within
        val bounds: Rect = box.toRect()


        // Set horizontal alignment to Center
        paint.setTextAlign(Paint.Align.CENTER)
        paint.setTextSize(measuredFontSize!!)
        paint.setTypeface(typeface)

        // Calculate X position: the horizontal center of the bounds
        val xPos = bounds.centerX()

        // Calculate Y position: the vertical center minus half the distance from baseline to the true center
        val yPos = ((bounds.centerY()) - ((paint.descent() + paint.ascent()) / 2))

        // Draw the text
        canvas.drawText(text, xPos.toFloat(), yPos, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (measuredFontSize == null) {
            measuredFontSize = this.measuredHeight / 32.0F;
        }

        val borderColor = when (steadyFrameIndicator.getStatus()) {
            CaptureStatusEnum.CAPTURING -> paintCaptureStatusGreenBorder
            CaptureStatusEnum.HOLD_STEADY -> paintCaptureStatusAmberBorder
            CaptureStatusEnum.NOT_FOUND -> paintCaptureStatusAmberBorder
            CaptureStatusEnum.PROCESSING -> paintCaptureStatusAmberBorder
        }

        val fillColor = when (steadyFrameIndicator.getStatus()) {
            CaptureStatusEnum.CAPTURING -> paintCaptureStatusGreenFill
            CaptureStatusEnum.HOLD_STEADY -> paintCaptureStatusAmberFill
            CaptureStatusEnum.NOT_FOUND -> paintCaptureStatusAmberFill
            CaptureStatusEnum.PROCESSING -> paintCaptureStatusAmberFill
        }

        pageBoundingBox?.let { canvas.drawPath(it.toPath(), borderColor) }
        pageBoundingBox?.let { canvas.drawPath(it.toPath(), fillColor) }
        qrCodeBoundingBox?.let { canvas.drawPath(it.toPath(), borderColor) }

        val fillBox = when (steadyFrameIndicator.getStatus()) {
            CaptureStatusEnum.CAPTURING -> pageBoundingBox?.fillFromBottom(steadyFrameIndicator.getPercentage())
            CaptureStatusEnum.HOLD_STEADY -> null
            CaptureStatusEnum.NOT_FOUND -> null
            CaptureStatusEnum.PROCESSING -> pageBoundingBox?.fillFromBottom(steadyFrameIndicator.getPercentage())
        }

        fillBox?.let { canvas.drawPath(it.toPath(), fillColor) }

        drawCenter(
            canvas,
            pageBoundingBox,
            paintStatusText,
            steadyFrameIndicator.getStatusMessage()
        )
    }
}
