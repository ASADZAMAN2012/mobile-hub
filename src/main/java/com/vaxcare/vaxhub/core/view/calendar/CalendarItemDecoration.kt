/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view.calendar

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R

/**
 * Make sure all items are spread inside
 */
class CalendarItemDecoration(val context: Context) : RecyclerView.ItemDecoration() {
    private val itemWidth = context.resources.getDimension(R.dimen.dp_52)
    private val itemHeight = context.resources.getDimension(R.dimen.dp_52)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val totalWidth = parent.measuredWidth
        val totalHeight = parent.measuredHeight

        val totalDisplacement = (totalWidth - itemWidth * CalendarPicker.COLUMN_SIZE) / CalendarPicker.COLUMN_SIZE
        val displacementPerColumn = totalDisplacement / (CalendarPicker.COLUMN_SIZE - 1)
        val columnIndex = position.rem(CalendarPicker.COLUMN_SIZE)
        val left = columnIndex * displacementPerColumn

        val isLastRow = position >= CalendarPicker.COLUMN_SIZE * (CalendarPicker.WEEK_SIZE - 1)
        val bottom = (totalHeight - itemHeight * CalendarPicker.WEEK_SIZE) / CalendarPicker.WEEK_SIZE

        outRect.set(left.toInt(), 0, 0, if (isLastRow) 0 else bottom.toInt())
    }
}
