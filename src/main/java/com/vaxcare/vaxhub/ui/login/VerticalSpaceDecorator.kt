/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.login

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R

class VerticalSpaceDecorator(
    val context: Context,
    heightDpDimenId: Int = R.dimen.dp_2,
    horizontalOffsetDpDimenId: Int = R.dimen.dp_35
) : RecyclerView.ItemDecoration() {
    private val height = context.resources.getDimension(heightDpDimenId)
    private val leftOffset = context.resources.getDimension(horizontalOffsetDpDimenId)
    private val color = context.getColor(R.color.primary_light_gray)
    private val paint = Paint()

    override fun onDrawOver(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val right = parent.width - leftOffset
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = (child.bottom + params.bottomMargin).toFloat()
            val bottom = top + height

            paint.color = color
            if (i == 0) {
                c.drawRect(leftOffset, 0f, right, height, paint)
            }
            c.drawRect(leftOffset, top, right, bottom, paint)
        }
    }
}
