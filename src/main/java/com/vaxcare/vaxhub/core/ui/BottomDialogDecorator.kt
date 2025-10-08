/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R

class BottomDialogDecorator(val context: Context) : RecyclerView.ItemDecoration() {
    private val height = context.resources.getDimension(R.dimen.dp_2)
    private val color = Color.parseColor("#fbfafa")
    private val paint = Paint()

    override fun onDrawOver(
        c: Canvas,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val left = context.resources.getDimension(R.dimen.dp_35)
        val right = parent.width - context.resources.getDimension(R.dimen.dp_35)
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = (child.bottom + params.bottomMargin).toFloat()
            val bottom = top + height

            paint.color = color
            c.drawRect(left, top, right, bottom, paint)
        }
    }
}
