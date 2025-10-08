/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 */
package com.vaxcare.vaxhub.ui.checkout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.safeLet
import com.vaxcare.vaxhub.model.enums.DeleteActionType
import com.vaxcare.vaxhub.ui.checkout.viewholder.VaccineItemProductViewHolder
import java.lang.Float.min
import java.util.LinkedList
import java.util.Queue
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
abstract class CheckoutPatientSwipeHelper(
    context: Context,
    private val recyclerView: RecyclerView,
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    private val buttonWidth: Float = context.resources.getDimension(R.dimen.dp_150)
    private var thresholds: Thresholds? = null
    private var touchUp = false
    private var swipedPos = -1
    private var swipeType: SwipeType = SwipeType.None(buttonWidth * 0.5f, 0f)
    private val recoverQueue: Queue<Int>
    private var initX: Int? = null
    private var displacement = 0
    private var touchDownPointInView: Boolean = false
    private var initialTouchdown: Point = Point(-1, -1)

    private val onTouchListener = OnTouchListener { _, motionEvent ->
        val point = Point(motionEvent.rawX.toInt(), motionEvent.rawY.toInt())
        touchUp = motionEvent.action == MotionEvent.ACTION_UP
        safeLet(thresholds, initX) { thresh, initialX ->
            val deltaX = point.x.toFloat() - initialX.toFloat()
            swipeType = evaluateFinalSwipeTypeByDelta(
                deltaX = deltaX - displacement,
                thresh = thresh,
                closestMargin = touchUp
            )
            recyclerView.findChildViewUnder(
                initialTouchdown.x.toFloat(),
                initialTouchdown.y.toFloat()
            )?.let { view ->
                (recyclerView.getChildViewHolder(view) as? VaccineItemProductViewHolder)?.apply {
                    if (swipeState != SwipeState.LOCK) {
                        if (touchDownPointInView) {
                            customDraw(
                                deltaX - displacement,
                                itemView.findViewById(R.id.view_foreground)
                            )
                            if (touchUp &&
                                swipeType is SwipeType.FullSwiped &&
                                motionEvent.y in thresh.heightThreshold
                            ) {
                                onFullSwipe(DeleteActionType.FULL_SWIPE)
                            }
                        } else {
                            if (touchUp) {
                                val upPos = recyclerView.layoutManager?.getPosition(view) ?: -1
                                if (swipedPos == upPos) {
                                    onActionDelete()
                                }
                            }
                        }
                    }
                }
            }
        }

        false
    }

    /**
     * Cross reference delta with thresholds and return the appropriate SwipeType
     *
     * @param deltaX - delta
     * @param thresh - thresholds (we can probably use the member var instead of passing it in)
     * @param closestMargin - if this is true, we need to do a fuzzy match to prevent locking nowhere
     * @return - appropriate SwipeType
     */
    private fun evaluateFinalSwipeTypeByDelta(
        deltaX: Float,
        thresh: Thresholds,
        closestMargin: Boolean = false
    ): SwipeType {
        if (closestMargin) {
            val none = SwipeType.None(buttonWidth * 0.5f, 0f)
            if (deltaX > thresh.halfSwipeThreshold.endInclusive) {
                return none
            }
            // values ?:
            return when (
                listOf(
                    thresh.halfSwipeThreshold.start,
                    thresh.fullSwipeThreshold.start,
                    thresh.fullSwipeThreshold.endInclusive
                ).minByOrNull { abs(deltaX - it) }
            ) {
                thresh.halfSwipeThreshold.start -> SwipeType.HalfSwiped(
                    buttonWidth * 0.5f,
                    -buttonWidth
                )

                thresh.fullSwipeThreshold.start, thresh.fullSwipeThreshold.endInclusive -> SwipeType.FullSwiped(
                    (thresh.foregroundWidth) * 0.5f,
                    -thresh.foregroundWidth
                )

                else -> none
            }
        }

        return when (deltaX) {
            in thresh.halfSwipeThreshold -> SwipeType.HalfSwiped(buttonWidth * 0.5f, -buttonWidth)
            in thresh.fullSwipeThreshold -> SwipeType.FullSwiped(
                thresh.foregroundWidth * 0.5f,
                -thresh.foregroundWidth
            )

            0f -> SwipeType.None(buttonWidth * 0.5f, 0f)
            else -> SwipeType.Swiping(buttonWidth * 0.5f, 0f)
        }
    }

    /**
     * Method for mutating the translationX of the foreground
     *
     * @param dX - raw deltaX from touch point
     * @param foregroundView - foreground view to mutate
     *
     * note: we are clamping the translation when the user has stopped touching the view or the
     * swipe type is "HalfSwiped" - thus snapping the translationX
     */
    private fun customDraw(dX: Float, foregroundView: View) {
        val translationX =
            if (touchUp || swipeType is SwipeType.HalfSwiped) {
                swipeType.maxTranslation
            } else {
                min(
                    dX,
                    0f
                )
            }
        foregroundView.translationX = translationX
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val holder = viewHolder as? VaccineItemProductViewHolder
        if (!touchDownPointInView || holder?.swipeState == SwipeState.LOCK) {
            return
        }
        val pos = viewHolder.bindingAdapterPosition
        if (swipedPos != pos) recoverQueue.add(swipedPos)
        swipedPos = pos
        if (swipeType is SwipeType.FullSwiped) {
            holder?.apply {
                swipeState = SwipeState.IDLE
                onFullSwipe(DeleteActionType.FULL_SWIPE)
            }
        }

        recoverSwipedItem()
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return swipeType.threshold
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 0.1f * defaultValue
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return 5.0f * defaultValue
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val pos = viewHolder.bindingAdapterPosition
        if (pos < 0) {
            swipedPos = pos
            return
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            (viewHolder as? VaccineItemProductViewHolder)?.apply {
                if (swipeState != SwipeState.LOCK) {
                    swipeState = SwipeState.SWIPING
                }
            }
        }
    }

    @Synchronized
    private fun recoverSwipedItem() {
        while (!recoverQueue.isEmpty()) {
            val pos = recoverQueue.poll() ?: -1
            if (pos > -1) {
                recyclerView.adapter?.notifyItemChanged(pos)
            }
        }
    }

    private fun attachSwipe() {
        val itemTouchHelper = ItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    init {
        this.recyclerView.setOnTouchListener(onTouchListener)

        /**
         * ItemTouch listener
         */
        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, motionEvent: MotionEvent): Boolean {
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    initX = rv.findChildViewUnder(motionEvent.x, motionEvent.y)?.let { view ->
                        val holder = rv.getChildViewHolder(view) as? VaccineItemProductViewHolder
                        holder?.let {
                            val foregroundView =
                                holder.itemView.findViewById<View>(R.id.view_foreground)

                            displacement = if (holder.swipeState == SwipeState.SWIPING) {
                                abs(foregroundView.translationX.toInt())
                            } else {
                                0
                            }
                            swipedPos = rv.layoutManager?.getPosition(view) ?: -1

                            val width = recyclerView.width.toFloat()
                            thresholds = Thresholds(
                                foregroundWidth = width,
                                heightThreshold = (view.top.toFloat())..(view.bottom.toFloat()),
                                halfSwipeThreshold = -(buttonWidth * 1.15f)..-(buttonWidth * 0.6f),
                                fullSwipeThreshold = -width..-(width * 0.5f)
                            )
                            swipeType = SwipeType.None(buttonWidth * 0.5f, 0f)
                            touchDownPointInView =
                                motionEvent.rawX < foregroundView.right - displacement
                            if (touchDownPointInView) {
                                initialTouchdown =
                                    Point(motionEvent.x.toInt(), motionEvent.y.toInt())
                            }

                            motionEvent.rawX.toInt()
                        }
                    }
                }

                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) = Unit

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
        })

        recoverQueue = object : LinkedList<Int>() {
            override fun add(element: Int): Boolean {
                return if (contains(element)) false else super.add(element)
            }
        }

        attachSwipe()
    }

    /**
     * Work around class to get over limitation by the Android API
     *
     * @property foregroundWidth - the width of the foreground we are swiping
     * @property halfSwipeThreshold - range of the threshold for "half swipe"
     * @property fullSwipeThreshold - range of the threshold for "full swipe"
     */
    data class Thresholds(
        val foregroundWidth: Float,
        val heightThreshold: ClosedFloatingPointRange<Float>,
        val halfSwipeThreshold: ClosedFloatingPointRange<Float>,
        val fullSwipeThreshold: ClosedFloatingPointRange<Float>
    )

    /**
     * SwipeType class for identifying swipe behavior
     *
     * @property threshold - the threshold to apply for getSwipeThreshold
     * @property maxTranslation - the maxValue to clamp the translationX in the view
     */
    sealed class SwipeType(var threshold: Float, val maxTranslation: Float) {
        class None(swipeThreshold: Float, clamp: Float) : SwipeType(swipeThreshold, clamp)

        class Swiping(swipeThreshold: Float, clamp: Float) : SwipeType(swipeThreshold, clamp)

        class HalfSwiped(swipeThreshold: Float, clamp: Float) : SwipeType(swipeThreshold, clamp)

        class FullSwiped(swipeThreshold: Float, clamp: Float) : SwipeType(swipeThreshold, clamp)
    }

    enum class SwipeState {
        IDLE,
        SWIPING,
        LOCK
    }
}
