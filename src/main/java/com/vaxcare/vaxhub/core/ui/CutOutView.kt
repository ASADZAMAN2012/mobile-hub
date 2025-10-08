/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/
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
 * The goal is to paint the entire background with a "cutout".
 * Defaults to a full view sized cutout: 0x, 0y, full width, full height
 */
class CutOutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    // viewfinder inner Width
    private var cutoutWidth = 0f

    // viewfinder inner Height
    private var cutoutHeight = 0f

    // view inner horizontal length
    private var cutoutStart = 0f

    // view inner vertical length
    private var cutoutTop = 0f

    private var voidColor = ContextCompat.getColor(context, R.color.cutout_mask)

    private val paint by lazy {
        Paint().apply {
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }
    private val frame = RectF()

    /**
     * Initialize the size of the cutout
     *
     * @param context
     * @param attrs
     */
    private fun initView(context: Context, attrs: AttributeSet?) {
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.CutOutView)

        cutoutWidth = styledAttributes.getDimension(
            R.styleable.CutOutView_cutout_width,
            resources.displayMetrics.widthPixels.toFloat()
        )

        cutoutHeight = styledAttributes.getDimension(
            R.styleable.CutOutView_cutout_height,
            resources.displayMetrics.heightPixels.toFloat()
        )

        cutoutStart = styledAttributes.getDimension(
            R.styleable.CutOutView_cutout_padding_start,
            0f
        )

        cutoutTop = styledAttributes.getDimension(
            R.styleable.CutOutView_cutout_padding_top,
            0f
        )

        voidColor = styledAttributes.getColor(
            R.styleable.CutOutView_background_color,
            ContextCompat.getColor(context, R.color.cutout_mask)
        )
        styledAttributes.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        frame.set(
            cutoutStart,
            cutoutTop,
            cutoutStart + cutoutWidth,
            cutoutTop + cutoutHeight
        )
    }

    public override fun onDraw(canvas: Canvas) {
        val width = width.toFloat()
        val height = height.toFloat()

        paint.color = voidColor
        // top
        canvas.drawRect(
            0f,
            0f,
            width,
            frame.top,
            paint
        )

        // start
        canvas.drawRect(
            0f,
            frame.top,
            frame.left,
            frame.bottom,
            paint
        )

        // end
        canvas.drawRect(
            frame.right,
            frame.top,
            width,
            frame.bottom,
            paint
        )

        // bottom
        canvas.drawRect(0f, frame.bottom, width, height, paint)
    }

    init {
        initView(context, attrs)
    }
}
