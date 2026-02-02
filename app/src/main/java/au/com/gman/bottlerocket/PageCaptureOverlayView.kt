package au.com.gman.bottlerocket

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import au.com.gman.bottlerocket.domain.CaptureStatusEnum
import au.com.gman.bottlerocket.domain.IndicatorBox
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

    private val paintCaptureStatusRedBorder =
        Paint()
            .apply {
                color = context.getColor(R.color.capture_status_red_border)
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }

    private val paintCaptureStatusRedFill =
        Paint()
            .apply {
                color = context.getColor(R.color.capture_status_red_transparent)
                style = Paint.Style.FILL
            }

    private val paintCaptureStatusBlueBorder =
        Paint()
            .apply {
                color = context.getColor(R.color.capture_status_blue_border)
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }

    private val paintCaptureStatusBlueFill =
        Paint()
            .apply {
                color = context.getColor(R.color.capture_status_blue_transparent)
                style = Paint.Style.FILL
            }

    private val paintStatusText =
        TextPaint()
            .apply {
                color = context.getColor(R.color.white)
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 2f
            }

    private val paintStatusUnmatchedCode =
        TextPaint()
            .apply {
                color = context.getColor(R.color.capture_status_unmatched_code)
                style = Paint.Style.FILL_AND_STROKE
                strokeWidth = 2f
                textSize = 8.0f
            }

    private var pageBoundingBox: RocketBoundingBox? = null
    private var indicatorBoxes: List<IndicatorBox?> = emptyList()

    private var unmatchedQrCode: String? = null

    fun setPageOverlayBox(box: RocketBoundingBox?) {
        pageBoundingBox = box
        // Invalidate the view to trigger a redraw
        postInvalidate()
    }

    fun setIndicatorBoxes(boxes: List<IndicatorBox?>) {
        indicatorBoxes = boxes
        // Invalidate the view to trigger a redraw
        postInvalidate()
    }

    fun setUnmatchedQrCode(qrCode: String? = null) {
        unmatchedQrCode = qrCode
        // Invalidate the view to trigger a redraw
        postInvalidate()
    }

    private fun drawCenteredText(
        canvas: Canvas,
        box: RocketBoundingBox?,
        paint: TextPaint,
        text: String
    ) {
        if (box == null) return
        if (measuredFontSize == null) return

        // Get the bounds of the area you are drawing within
        val bounds: Rect = box.toRect()

        // Set horizontal alignment to Center
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = measuredFontSize!!
        paint.setTypeface(typeface)

        // Calculate X position: the horizontal center of the bounds
        val xPos = bounds.centerX()

        // Calculate Y position: the vertical center minus half the distance from baseline to the true center
        val yPos = ((bounds.centerY()) - ((paint.descent() + paint.ascent()) / 2))

        // Draw the text
        canvas.drawText(text, xPos.toFloat(), yPos, paint)
    }

    private fun drawWrappedText(
        canvas: Canvas,
        box: RocketBoundingBox?,
        paint: TextPaint,
        text: String
    ) {
        if (box == null) return
        if (measuredFontSize == null) return

        // Get the bounds of the area you are drawing within
        val bounds: Rect = box.toRect()

        // Set horizontal alignment to Center
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = (measuredFontSize!!) * 0.5F
        paint.setTypeface(typeface)

        // Use StaticLayout.Builder for API 23+
        val staticLayout =
            StaticLayout
                .Builder
                .obtain(text, 0, text.length, paint, bounds.width())
                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .build()

        // Draw the text
        canvas
            .save()

        canvas
            .translate(
                bounds.left.toFloat() + (staticLayout.width / 2),
                bounds.top.toFloat() - (staticLayout.height * 1.5F)
            )

        staticLayout
            .draw(canvas)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (measuredFontSize == null) {
            measuredFontSize = this.measuredHeight / 32.0F
        }

        val pageBorderColor = when (steadyFrameIndicator.getStatus()) {
            CaptureStatusEnum.CAPTURING -> paintCaptureStatusGreenBorder
            CaptureStatusEnum.HOLD_STEADY -> paintCaptureStatusAmberBorder
            CaptureStatusEnum.NOT_FOUND -> paintCaptureStatusAmberBorder
            CaptureStatusEnum.PROCESSING -> paintCaptureStatusBlueBorder
            CaptureStatusEnum.OUT_OF_BOUNDS -> paintCaptureStatusRedBorder
        }

        val pageFillColor = when (steadyFrameIndicator.getStatus()) {
            CaptureStatusEnum.CAPTURING -> paintCaptureStatusGreenFill
            CaptureStatusEnum.HOLD_STEADY -> paintCaptureStatusAmberFill
            CaptureStatusEnum.NOT_FOUND -> paintCaptureStatusAmberFill
            CaptureStatusEnum.PROCESSING -> paintCaptureStatusBlueFill
            CaptureStatusEnum.OUT_OF_BOUNDS -> paintCaptureStatusRedFill
        }

        pageBoundingBox?.let { canvas.drawPath(it.toPath(), pageBorderColor) }
        pageBoundingBox?.let { canvas.drawPath(it.toPath(), pageFillColor) }

        indicatorBoxes.forEachIndexed { index, it ->
            if (it?.box != null) {
                val indicatorBorderColor = when (it.status) {
                    CaptureStatusEnum.CAPTURING -> paintCaptureStatusGreenBorder
                    CaptureStatusEnum.HOLD_STEADY -> paintCaptureStatusAmberBorder
                    CaptureStatusEnum.NOT_FOUND -> paintCaptureStatusAmberBorder
                    CaptureStatusEnum.PROCESSING -> paintCaptureStatusBlueBorder
                    CaptureStatusEnum.OUT_OF_BOUNDS -> paintCaptureStatusRedBorder
                }
                canvas.drawPath(it.box.toPath(), indicatorBorderColor)
                unmatchedQrCode?.let {
                    val unmatchedStatusWithCode = "Unmatched QR Code:\r\n\r\n${it}"
                    drawWrappedText(
                        canvas,
                        indicatorBoxes[index]?.box,
                        paintStatusUnmatchedCode,
                        unmatchedStatusWithCode
                    )
                }
            }
        }

        val fillBox = when (steadyFrameIndicator.getStatus()) {
            CaptureStatusEnum.CAPTURING -> pageBoundingBox?.fillFromBottom(steadyFrameIndicator.getPercentage())
            CaptureStatusEnum.HOLD_STEADY -> null
            CaptureStatusEnum.NOT_FOUND -> null
            CaptureStatusEnum.PROCESSING -> pageBoundingBox?.fillFromBottom(steadyFrameIndicator.getPercentage())
            CaptureStatusEnum.OUT_OF_BOUNDS -> null
        }

        fillBox?.let { canvas.drawPath(it.toPath(), pageFillColor) }


        if (steadyFrameIndicator.getStatus() == CaptureStatusEnum.OUT_OF_BOUNDS) {
            drawWrappedText(
                canvas,
                pageBoundingBox,
                paintStatusText,
                steadyFrameIndicator.getStatusMessage()
            )
        } else {
            drawCenteredText(
                canvas,
                pageBoundingBox,
                paintStatusText,
                steadyFrameIndicator.getStatusMessage()
            )
        }
    }
}
