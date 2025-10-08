/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.checkout.reviewdoses

import androidx.core.view.isVisible
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.robot.BaseTestRobot

class ReviewDosesRobot : BaseTestRobot() {
    fun selectUnorderedReasonDoseNotAppearingForAll() {
        // displace for footer view
        val itemCount = getItemCountFromRecyclerView() - 1
        for (index in 0 until itemCount) {
            setUnorderedDoseReason(R.string.orders_unordered_dose_reason_order_not_appearing)
        }
    }

    fun tapArrowToCheckoutSummary() {
        val view = onView(ViewMatchers.withId(R.id.fab_next))
        waitForViewCondition(view) {
            it.isVisible && it.isEnabled
        }
        IntegrationUtil.simpleClick(view, "ArrowButton")
    }

    private fun setUnorderedDoseReason(resId: Int) {
        onView(withId(R.id.recycler_view)).perform(ViewActions.swipeUp())
        onView(withIndex(withText(R.string.orders_review_set_reason))).perform(click())
        IntegrationUtil.waitUIWithDelayed(1000)
        IntegrationUtil.clickBottomDialogItem(resId)
    }
}

fun reviewDosesScreen(block: ReviewDosesRobot.() -> Unit) = ReviewDosesRobot().apply(block)
