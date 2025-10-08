/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import com.vaxcare.vaxhub.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
class TrayLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {
    private lateinit var trayView: View
    private var offset = 0
    private var listeners = mutableListOf<TrayListener>()

    var shouldInterceptTouch: ((event: MotionEvent) -> Boolean)? = null

    private val dragCallback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return child == trayView
        }

        override fun clampViewPositionHorizontal(
            child: View,
            left: Int,
            dx: Int
        ): Int = 0

        override fun clampViewPositionVertical(
            child: View,
            top: Int,
            dy: Int
        ): Int {
            return max(-child.height, min(top, 0))
        }

        override fun onViewDragStateChanged(state: Int) {
            if (state == ViewDragHelper.STATE_IDLE) {
                offset = trayView.height + trayView.top
                isTrayOpened = trayView.top == 0
                listeners.forEach { listener ->
                    if (isTrayOpened) {
                        listener.onTrayOpened()
                    } else {
                        listener.onTrayClosed()
                    }
                }
            }
        }

        override fun onEdgeDragStarted(edgeFlags: Int, pointerId: Int) {
            if (isTrayEnabled) {
                dragHelper.captureChildView(trayView, pointerId)
                listeners.forEach { listener ->
                    listener.onTrayStartDrag()
                }
            }
        }

        override fun onViewReleased(
            releasedChild: View,
            xVelocity: Float,
            yVelocity: Float
        ) {
            val finalTop = when {
                yVelocity > 0 -> 0
                yVelocity < 0 -> -trayView.height
                abs(releasedChild.top) > trayView.height / 2 -> -trayView.height
                else -> 0
            }
            dragHelper.settleCapturedViewAt(0, finalTop)
            invalidate()
        }
    }

    private var dragHelper: ViewDragHelper = ViewDragHelper.create(this, 1.0F, dragCallback)

    var isTrayEnabled = true
    var isTrayOpened = false

    init {
        dragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_TOP)
        dragHelper.edgeSize = context.resources.getDimension(R.dimen.dp_160).toInt()
    }

    fun closeTray() {
        if (isTrayOpened) {
            dragHelper.smoothSlideViewTo(trayView, 0, -trayView.height)
            invalidate()
        }
    }

    fun addTrayListener(listener: TrayListener?) {
        if (listener == null) {
            return
        }
        listeners.add(listener)
    }

    fun removeListener(listener: TrayListener?) {
        if (listener == null) {
            return
        }
        listeners.remove(listener)
    }

    override fun computeScroll() {
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            when (i) {
                0 -> {
                    child.layout(0, 0, child.measuredWidth, child.measuredHeight)
                }
                1 -> {
                    trayView = child
                    child.layout(0, -child.measuredHeight + offset, child.measuredWidth, offset)
                }
                else -> {
                    child.layout(right - child.measuredWidth, 0, right, child.measuredHeight)
                }
            }
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val interceptForDrag = dragHelper.shouldInterceptTouchEvent(event)
        var interceptForTap = false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isTrayOpened && shouldInterceptTouch?.invoke(event) == true
                ) {
                    interceptForTap = true
                }
            }
        }
        return interceptForDrag || interceptForTap
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        dragHelper.processTouchEvent(event)
        return true
    }
}

interface TrayListener {
    fun onTrayOpened()

    fun onTrayClosed()

    fun onTrayStartDrag()
}
