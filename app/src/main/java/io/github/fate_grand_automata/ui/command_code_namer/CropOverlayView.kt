package io.github.fate_grand_automata.ui.command_code_namer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import kotlin.math.roundToInt

/**
 * Displays a bitmap at whatever width this view is laid out to (height follows to keep the
 * bitmap's own aspect ratio) with a fixed-size, draggable crop box overlaid on top. All crop
 * math is done in the bitmap's own pixel space, not this view's on-screen (density-scaled)
 * pixel space, so [currentCropRect] is directly usable to crop the *source* bitmap regardless
 * of how large this view is drawn on any particular device.
 */
class CropOverlayView(
    context: Context,
    private val cropSize: Int
) : View(context) {
    private var bitmap: Bitmap? = null

    /** bitmapPixels * displayScale == this view's own local (on-screen) pixels. */
    private var displayScale = 1f

    /** Crop box top-left, in the *source bitmap's* pixel space. */
    private var cropX = 0f
    private var cropY = 0f

    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var dragging = false

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val boxFillPaint = Paint().apply {
        color = Color.argb(70, 0, 230, 90)
        style = Paint.Style.FILL
    }
    private val boxStrokePaint = Paint().apply {
        color = Color.rgb(0, 230, 90)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    fun setBitmap(bmp: Bitmap) {
        bitmap = bmp
        cropX = clampX((bmp.width - cropSize) / 2f)
        cropY = clampY((bmp.height - cropSize) / 2f)
        requestLayout()
        invalidate()
    }

    fun currentCropRect(): Rect {
        val left = cropX.roundToInt()
        val top = cropY.roundToInt()
        return Rect(left, top, left + cropSize, top + cropSize)
    }

    private fun clampX(x: Float): Float {
        val bmp = bitmap ?: return 0f
        val max = maxOf(0f, (bmp.width - cropSize).toFloat())
        return x.coerceIn(0f, max)
    }

    private fun clampY(y: Float): Float {
        val bmp = bitmap ?: return 0f
        val max = maxOf(0f, (bmp.height - cropSize).toFloat())
        return y.coerceIn(0f, max)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val bmp = bitmap
        if (bmp == null) {
            setMeasuredDimension(0, 0)
            return
        }

        val availableWidth = MeasureSpec.getSize(widthMeasureSpec).takeIf { it > 0 } ?: bmp.width
        displayScale = availableWidth / bmp.width.toFloat()
        val displayHeight = (bmp.height * displayScale).roundToInt()

        setMeasuredDimension(availableWidth, displayHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = bitmap ?: return

        canvas.drawBitmap(bmp, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), bitmapPaint)

        val left = cropX * displayScale
        val top = cropY * displayScale
        val right = (cropX + cropSize) * displayScale
        val bottom = (cropY + cropSize) * displayScale

        canvas.drawRect(left, top, right, bottom, boxFillPaint)
        canvas.drawRect(left, top, right, bottom, boxStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bitmap == null || displayScale <= 0f) return false

        val touchX = event.x / displayScale
        val touchY = event.y / displayScale

        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragging = touchX in cropX..(cropX + cropSize) && touchY in cropY..(cropY + cropSize)
                if (dragging) {
                    dragOffsetX = touchX - cropX
                    dragOffsetY = touchY - cropY
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                dragging
            }

            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                cropX = clampX(touchX - dragOffsetX)
                cropY = clampY(touchY - dragOffsetY)
                invalidate()
                true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
                true
            }

            else -> false
        }
    }
}
