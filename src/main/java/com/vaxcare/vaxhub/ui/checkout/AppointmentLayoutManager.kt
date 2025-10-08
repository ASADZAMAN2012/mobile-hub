/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.content.Context
import android.graphics.PointF
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R

class AppointmentLayoutManager(val context: Context) : LinearLayoutManager(context) {
    override fun smoothScrollToPosition(
        recyclerView: RecyclerView?,
        state: RecyclerView.State?,
        position: Int
    ) {
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int = SNAP_TO_START

            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? =
                this@AppointmentLayoutManager.computeScrollVectorForPosition(targetPosition)

            override fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
                val offset = context.resources.getDimension(R.dimen.dp_60).toInt()
                return super.calculateDyToMakeVisible(view, snapPreference) - offset
            }
        }
        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }
}
