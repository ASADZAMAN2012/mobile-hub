/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.toLocalTimeString
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.min

class AppointmentListDecoration(val context: Context) : RecyclerView.ItemDecoration() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val transparent = Color.TRANSPARENT
    private val bgColor = context.getColor(R.color.background_color)
    private val colorText = context.getColor(R.color.primary_black)
    private val colorDivider = context.getColor(R.color.primary_light_gray)
    var timestamp: List<LocalDateTime> = mutableListOf()
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorText
        textSize = context.resources.getDimension(R.dimen.sp_32)
        typeface = ResourcesCompat.getFont(context, R.font.graphik_regular)
    }
    private val textPaintBold = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorText
        textSize = context.resources.getDimension(R.dimen.sp_32)
        typeface = ResourcesCompat.getFont(context, R.font.graphik_semi_bold)
    }
    private val radiusOfListViewBackground = context.resources.getDimension(R.dimen.dp_20)
    private val marginForFirstItem = context.resources.getDimension(R.dimen.dp_80)
    private val marginForItem = context.resources.getDimension(R.dimen.dp_112)
    private val marginForInBetweenItems = context.resources.getDimension(R.dimen.dp_4)

    override fun onDrawOver(
        canvas: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val count = parent.childCount
        if (count == 0 || timestamp.isEmpty()) {
            return
        }
        drawTimestampIndex(canvas, parent)
        paint.color = bgColor
        drawStickyTimestampIndex(canvas, parent)
    }

    override fun onDraw(
        canvas: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val count = parent.childCount
        if (count == 0 || timestamp.isEmpty()) {
            return
        }
        val firstChild = parent.getChildAt(0)
        val left = firstChild.left.toFloat()
        val right = firstChild.right.toFloat()
        val top = 0.toFloat()
        val bottom = parent.height.toFloat()
        val radius = radiusOfListViewBackground
        paint.color = transparent
        // rounded white background
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
        drawDivider(canvas, parent)
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val count = parent.childCount
        if (count == 0 || timestamp.isEmpty()) {
            return
        }
        val position = parent.getChildAdapterPosition(view)
        if (position == -1) {
            return
        }
        val currentDate = timestamp.safeGet(position)

        val left = 0
        var top = 0
        if (position == 0) {
            top = marginForFirstItem.toInt()
        } else if (currentDate != timestamp.safeGet(position - 1)) {
            top = marginForItem.toInt()
        }
        val bottom = marginForInBetweenItems.toInt()
        val right = 0
        outRect.set(left, top, right, bottom)
    }

    private fun drawTimestampIndex(canvas: Canvas, parent: RecyclerView) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val adapterPosition = parent.getChildAdapterPosition(child)
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return
            }
            val currentDate = timestamp.safeGet(adapterPosition)
            val currentTime = currentDate.toLocalTimeString().toLowerCase(Locale.getDefault())

            if (adapterPosition != 0 && currentDate != timestamp.safeGet(adapterPosition - 1)) {
                val x = context.resources.getDimension(R.dimen.dp_85)
                val baseline =
                    child.top.toFloat() - context.resources.getDimension(R.dimen.dp_60) - textPaint.fontMetrics.top
                canvas.drawText(currentTime, x, baseline, textPaint)
            }
        }
    }

    private fun drawDivider(canvas: Canvas, parent: RecyclerView) {
        for (i in 0 until parent.childCount - 1) {
            val child = parent.getChildAt(i)
            val newLeft = (child.left + child.paddingLeft).toFloat()
            val newRight = (child.right - child.paddingRight).toFloat()
            val newTop = child.bottom.toFloat() + context.resources.getDimension(R.dimen.dp_4)
            val newBottom = newTop + context.resources.getDimension(R.dimen.dp_4)
            paint.color = colorDivider
            canvas.drawRect(newLeft, newTop, newRight, newBottom, paint)
        }
    }

    private fun drawStickyTimestampIndex(canvas: Canvas, parent: RecyclerView) {
        val layoutManager = parent.layoutManager as LinearLayoutManager
        val firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition()
        if (firstVisiblePosition != RecyclerView.NO_POSITION) {
            val firstPosition = if (firstVisiblePosition == 0) 0 else (firstVisiblePosition - 1)
            val firstVisibleChildView =
                parent.findViewHolderForAdapterPosition(firstPosition)?.itemView

            val currentDate =
                if (firstVisibleChildView != null &&
                    firstVisibleChildView.top > context.resources.getDimension(R.dimen.dp_60) &&
                    firstPosition > 1
                ) {
                    timestamp.safeGet(firstPosition - 1)
                } else {
                    timestamp.safeGet(firstPosition)
                }

            val currentTime =
                currentDate.toLocalTimeString().toLowerCase(Locale.getDefault())

            var y = -textPaint.fontMetrics.top
            if (firstVisibleChildView != null && firstPosition != 0) {
                val top =
                    firstVisibleChildView.top - context.resources.getDimension(R.dimen.dp_60)
                if (y > top && top > 0) {
                    y = top
                }
            }

            val rectY = if (y < -textPaint.fontMetrics.top) {
                textPaint.fontMetrics.top + y
            } else {
                0f
            }

            canvas.drawRect(
                0f,
                rectY,
                context.resources.displayMetrics.widthPixels.toFloat(),
                textPaint.fontMetrics.bottom - textPaint.fontMetrics.top + rectY,
                paint
            )
            canvas.drawText(
                currentTime,
                context.resources.getDimension(R.dimen.dp_85),
                y,
                textPaintBold
            )
        }
    }

    private fun <T : Any> List<T>.safeGet(position: Int): T {
        return get(min(position, size - 1))
    }
}
