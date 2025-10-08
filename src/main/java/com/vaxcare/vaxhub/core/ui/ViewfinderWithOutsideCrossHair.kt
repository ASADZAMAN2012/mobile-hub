/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 */
package com.vaxcare.vaxhub.core.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.vaxcare.vaxhub.R

/**
 * viewfinder window
 */
class ViewfinderWithOutsideCrossHair @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private var crossHairHorizontalLength = resources.getDimension(R.dimen.dp_50)
    private var crossHairVerticalLength = resources.getDimension(R.dimen.dp_36)
    private var crossHairStrokeWidth = resources.getDimension(R.dimen.dp_6)
    private var crossHairColor = ContextCompat.getColor(context, R.color.default_inner_color)

    private val paint by lazy {
        Paint().apply {
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    private val frame = RectF()

    /**
     * Draw the viewfinder on the canvas, add a background frame to shrink the size of the viewfinder
     * by the size of the cross hair stroke so it appears they are outside of the viewfinder
     *
     * @param canvas
     */
    public override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        frame.set(0f, 0f, width, height)
        paint.color = resources.getColor(R.color.background_color)

        // top
        canvas.drawRect(0f, 0f, width, 0f + crossHairStrokeWidth, paint)

        // left
        canvas.drawRect(0f, 0f, crossHairStrokeWidth, height + 1, paint)

        // right
        canvas.drawRect(width - crossHairStrokeWidth + 1, 0f, width + 1, height + 1, paint)

        // bottom
        canvas.drawRect(0f, height - crossHairStrokeWidth + 1, width + 1, height + 1, paint)

        drawFrameCrossHairs(canvas, frame)
    }

    /**
     * Draw the cross hairs around the viewfinder
     *
     * @param canvas
     * @param frame
     */
    private fun drawFrameCrossHairs(canvas: Canvas, frame: RectF) {
        paint.color = crossHairColor
        paint.strokeWidth = crossHairStrokeWidth

        // Left Top (vertical)
        canvas.drawLine(
            frame.left + crossHairStrokeWidth,
            frame.top + crossHairStrokeWidth,
            frame.left + crossHairStrokeWidth,
            (frame.top + crossHairVerticalLength) + crossHairStrokeWidth,
            paint
        )

        // Left Top (horizontal)
        canvas.drawLine(
            frame.left + crossHairStrokeWidth,
            frame.top + crossHairStrokeWidth,
            (frame.left + crossHairHorizontalLength) + crossHairStrokeWidth,
            frame.top + crossHairStrokeWidth,
            paint
        )

        // Right Top (vertical)
        canvas.drawLine(
            frame.right - crossHairStrokeWidth,
            frame.top + crossHairStrokeWidth,
            frame.right - crossHairStrokeWidth,
            (frame.top + crossHairVerticalLength) + crossHairStrokeWidth,
            paint
        )

        // Right Top (horizontal)
        canvas.drawLine(
            (frame.right - crossHairHorizontalLength) - crossHairStrokeWidth,
            frame.top + crossHairStrokeWidth,
            frame.right - crossHairStrokeWidth,
            frame.top + crossHairStrokeWidth,
            paint
        )

        // Left Bottom (vertical)
        canvas.drawLine(
            frame.left + crossHairStrokeWidth,
            (frame.bottom - crossHairVerticalLength) - crossHairStrokeWidth,
            frame.left + crossHairStrokeWidth,
            frame.bottom - crossHairStrokeWidth,
            paint
        )

        // Left Bottom (horizontal)
        canvas.drawLine(
            frame.left + crossHairStrokeWidth,
            frame.bottom - crossHairStrokeWidth,
            (frame.left + crossHairHorizontalLength) + crossHairStrokeWidth,
            frame.bottom - crossHairStrokeWidth,
            paint
        )

        // Right Bottom (vertical)
        canvas.drawLine(
            frame.right - crossHairStrokeWidth,
            (frame.bottom - crossHairVerticalLength) - crossHairStrokeWidth,
            frame.right - crossHairStrokeWidth,
            frame.bottom - crossHairStrokeWidth,
            paint
        )

        // Right Bottom (horizontal)
        canvas.drawLine(
            (frame.right - crossHairHorizontalLength) - crossHairStrokeWidth,
            frame.bottom - crossHairStrokeWidth,
            frame.right - crossHairStrokeWidth,
            frame.bottom - crossHairStrokeWidth,
            paint
        )
    }
}
