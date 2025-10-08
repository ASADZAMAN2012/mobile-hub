/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 */
package com.vaxcare.vaxhub.core

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A deeper explanation can be found at [stackoverflow](https://stackoverflow.com/questions/7417123/android-how-to-adjust-layout-in-full-screen-mode-when-softkeyboard-is-visible)
 */
class AndroidBug5497Workaround private constructor(activity: Activity) {
    private var childOfContent: View? = null
    private var usableHeightPrevious = 0
    private var frameLayoutParams: FrameLayout.LayoutParams? = null
    private val listener: OnGlobalLayoutListener = OnGlobalLayoutListener {
        possiblyResizeChildOfContent()
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        // For more information, see https://issuetracker.google.com/issues/36911528
        // To use this class, simply invoke assistActivity() on an Activity that already has its content view set.
        fun assistActivity(activity: Activity) = AndroidBug5497Workaround(activity)
    }

    init {
        val content = activity.findViewById<FrameLayout>(android.R.id.content)
        childOfContent = content.getChildAt(0)
        childOfContent?.viewTreeObserver?.addOnGlobalLayoutListener(listener)
        frameLayoutParams = childOfContent?.layoutParams as FrameLayout.LayoutParams
    }

    private fun possiblyResizeChildOfContent() {
        val usableHeightNow = computeUsableHeight()
        if (usableHeightNow != usableHeightPrevious) {
            childOfContent?.let { child ->
                val usableHeightSansKeyboard = child.rootView.height
                val heightDifference = usableHeightSansKeyboard - usableHeightNow
                if (heightDifference > usableHeightSansKeyboard / 4) {
                    // keyboard probably just became visible
                    frameLayoutParams?.height = usableHeightSansKeyboard - heightDifference
                } else {
                    // keyboard probably just became hidden
                    frameLayoutParams?.height = usableHeightSansKeyboard
                }
                child.requestLayout()
                usableHeightPrevious = usableHeightNow
            }
        }
    }

    private fun computeUsableHeight(): Int {
        val r = Rect()
        childOfContent?.getWindowVisibleDisplayFrame(r)
        return r.bottom - r.top
    }

    fun stopAssistingActivity() {
        scope.launch(Dispatchers.Main) {
            delay(500L)
            childOfContent?.viewTreeObserver?.removeOnGlobalLayoutListener(listener)
            childOfContent = null
            frameLayoutParams = null
        }
    }
}
