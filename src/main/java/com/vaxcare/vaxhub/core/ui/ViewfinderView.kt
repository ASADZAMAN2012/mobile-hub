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
class ViewfinderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    // viewfinder inner Width
    private var innerWidth = 0F

    // viewfinder inner Height
    private var innerHeight = 0F

    // view inner horizontal length
    private var lengthHorizontal = 0F

    // view inner vertical length
    private var lengthVertical = 0F

    // stroke width
    private var strokeWidth = 0F

    // view inner color
    private var innerCornerColor = 0

    private var showMask = true

    private var maskColor: Int = ContextCompat.getColor(context, R.color.viewfinder_mask)

    private val paint by lazy {
        Paint().apply {
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }
    private val frame = RectF()

    /**
     * Initialize the size of the inner frame
     *
     * @param context
     * @param attrs
     */
    private fun initInnerRect(context: Context, attrs: AttributeSet?) {
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.ViewfinderView)

        innerWidth = styledAttributes.getDimension(
            R.styleable.ViewfinderView_inner_width,
            resources.displayMetrics.widthPixels.toFloat()
        )
        innerHeight = styledAttributes.getDimension(
            R.styleable.ViewfinderView_inner_height,
            resources.getDimension(R.dimen.default_inner_height)
        )
        innerCornerColor = styledAttributes.getColor(
            R.styleable.ViewfinderView_inner_corner_color,
            ContextCompat.getColor(context, R.color.default_inner_color)
        )
        lengthHorizontal = styledAttributes.getDimension(
            R.styleable.ViewfinderView_inner_corner_length_horizontal,
            resources.getDimension(R.dimen.default_inner_corner_length_horizontal)
        )
        lengthVertical = styledAttributes.getDimension(
            R.styleable.ViewfinderView_inner_corner_length_vertical,
            resources.getDimension(R.dimen.default_inner_corner_length_vertical)
        )
        strokeWidth = styledAttributes.getDimension(
            R.styleable.ViewfinderView_inner_stroke_width,
            resources.getDimension(R.dimen.default_inner_stroke_width)
        )
        maskColor = styledAttributes.getColor(
            R.styleable.ViewfinderView_mask_color,
            ContextCompat.getColor(context, R.color.viewfinder_mask)
        )

        showMask = styledAttributes.getBoolean(
            R.styleable.ViewfinderView_show_mask,
            true
        )
        styledAttributes.recycle()
    }

    public override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()
        val xMid = width / 2
        val yMid = height / 2
        val leftOffset = xMid - innerWidth / 2
        val topOffset = yMid - innerHeight / 2

        frame.set(
            leftOffset,
            topOffset,
            leftOffset + innerWidth,
            topOffset + innerHeight
        )

        if (showMask) {
            // Draw the exterior (i.e. outside the framing rect) darkened
            paint.color = maskColor
            // top
            canvas.drawRect(
                0f,
                0f,
                width,
                frame.top,
                paint
            )

            // left
            canvas.drawRect(
                0f,
                frame.top,
                frame.left,
                frame.bottom + 1,
                paint
            )

            // right
            canvas.drawRect(
                frame.right + 1,
                frame.top,
                width,
                frame.bottom + 1,
                paint
            )

            // bottom
            canvas.drawRect(
                0f,
                frame.bottom + 1,
                width,
                height,
                paint
            )
        }

        drawFrameBounds(canvas, frame)
    }

    /**
     * Draw frame border
     *
     * @param canvas
     * @param frame
     */
    private fun drawFrameBounds(canvas: Canvas, frame: RectF) {
        paint.color = innerCornerColor
        paint.strokeWidth = strokeWidth

        // Left Top (vertical)
        canvas.drawLine(
            frame.left,
            frame.top,
            frame.left,
            (frame.top + lengthVertical),
            paint
        )

        // Left Top (horizontal)
        canvas.drawLine(
            frame.left,
            frame.top,
            (frame.left + lengthHorizontal),
            frame.top,
            paint
        )

        // Right Top (vertical)
        canvas.drawLine(
            frame.right,
            frame.top,
            frame.right,
            (frame.top + lengthVertical),
            paint
        )

        // Right Top (horizontal)
        canvas.drawLine(
            (frame.right - lengthHorizontal),
            frame.top,
            frame.right,
            frame.top,
            paint
        )

        // Left Bottom (vertical)
        canvas.drawLine(
            frame.left,
            (frame.bottom - lengthVertical),
            frame.left,
            frame.bottom,
            paint
        )

        // Left Bottom (horizontal)
        canvas.drawLine(
            frame.left,
            frame.bottom,
            (frame.left + lengthHorizontal),
            frame.bottom,
            paint
        )

        // Right Bottom (vertical)
        canvas.drawLine(
            frame.right,
            (frame.bottom - lengthVertical),
            frame.right,
            frame.bottom,
            paint
        )

        // Right Bottom (horizontal)
        canvas.drawLine(
            (frame.right - lengthHorizontal),
            frame.bottom,
            frame.right,
            frame.bottom,
            paint
        )
    }

    init {
        initInnerRect(context, attrs)
    }
}
