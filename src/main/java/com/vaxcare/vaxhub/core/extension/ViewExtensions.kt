/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.extension

import android.app.Activity
import android.graphics.Outline
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewOutlineProvider
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.Constant
import com.vaxcare.vaxhub.core.ui.OnSingleClickListener
import dagger.hilt.android.internal.managers.ViewComponentManager

/**
 * Sets the background color for an element given a primary color res
 *
 * @param primaryColorRes
 */
fun View.setBackgroundColorRes(
    @ColorRes primaryColorRes: Int
) {
    val primaryColor =
        ContextCompat.getColor(context, primaryColorRes)
    this.setBackgroundColor(primaryColor)
}

fun View.setOnSingleClickListener(listener: View.OnClickListener) {
    setOnClickListener(OnSingleClickListener(listener))
}

fun View.setOnSingleClickListener(listener: (View) -> Unit) {
    setOnClickListener(OnSingleClickListener(listener))
}

fun View.isTouchPointInView(x: Int, y: Int): Boolean {
    val location = IntArray(2)
    getLocationOnScreen(location)
    val left = location[0]
    val top = location[1]
    val right = left + measuredWidth
    val bottom = top + measuredHeight
    return y in top..bottom && x >= left && x <= right
}

fun View.clipOutlineCornerRadius(radius: Float = context.resources.getDimension(R.dimen.dp_20)) {
    outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View?, outline: Outline?) {
            if (view != null && outline != null) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
    }
    clipToOutline = true
}

fun View.animateFadeIn() {
    apply {
        alpha = 0f
        animate().setStartDelay(Constant.DELAY_MIDDLE).alpha(1.0f).start()
        isVisible = true
    }
}

fun View.getActivity(): Activity? {
    var currentContext = context
    when (currentContext) {
        is ContextThemeWrapper -> {
            while (currentContext is ContextThemeWrapper) {
                if (currentContext is Activity) {
                    return currentContext
                }
                currentContext = currentContext.baseContext as ContextThemeWrapper
            }
        }

        is ViewComponentManager.FragmentContextWrapper -> {
            return currentContext.baseContext as Activity
        }
    }

    return null
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}
