/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.content.Context
import android.util.AttributeSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.Behavior.DragCallback

/**
 * Class extending AppBarLayout and implementing a function to enable/disable drag to scroll
 */
class VaxAppBarLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppBarLayout(context, attrs) {
    /**
     * @param newValue true to enable drag to scroll in the App Bar Layout; false to disable
     */
    fun setAppBarDragging(newValue: Boolean) {
        val params = layoutParams as CoordinatorLayout.LayoutParams
        val behavior = Behavior()
        behavior.setDragCallback(object : DragCallback() {
            override fun canDrag(appBarLayout: AppBarLayout): Boolean {
                return newValue
            }
        })
        params.behavior = behavior
    }
}
