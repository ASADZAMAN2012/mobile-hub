/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.checkout.summary

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.robot.BaseTestRobot
import org.hamcrest.CoreMatchers.not

class CollectPaymentInfoRobot : BaseTestRobot() {
    fun verifyTitle() {
        verifyToolbarTitle(R.string.med_d_copay_title)
    }

    /**
     * This should only be called if partner has the correct prefill FF on
     */
    fun verifyPhonePrefilled() {
        verifyAnyTextOnView(R.id.med_d_copay_add_phone_start)
        verifyAnyTextOnView(R.id.med_d_copay_add_phone_mid)
        verifyAnyTextOnView(R.id.med_d_copay_add_phone_end)
    }

    fun enterCardNumber(number: String) {
        enterText(R.id.med_d_copay_card_number, number)
    }

    /**
     * CC expiration: "MMM / YYYY"
     * January or 01 : JAN
     * 24 or 2024 : 2024
     */
    fun enterCardExpiration(month: String, year: String) {
        IntegrationUtil.simpleClick(
            onView(withId(R.id.med_d_copay_expiration_month)),
            "CC Exp Month"
        )
        IntegrationUtil.waitUIWithDelayed(1000)
        clickBottomDialogItem(month)
        IntegrationUtil.waitUIWithDelayed(1000)
        clickBottomDialogItem(year)
    }

    fun enterNameOnCard(name: String) {
        enterText(R.id.med_d_copay_card_name, name)
    }

    fun enterPhoneNumber(
        start: String,
        mid: String,
        end: String
    ) {
        enterText(R.id.med_d_copay_add_phone_start, start)
        IntegrationUtil.waitUIWithDelayed(500)
        enterText(R.id.med_d_copay_add_phone_mid, mid)
        IntegrationUtil.waitUIWithDelayed(500)
        enterText(R.id.med_d_copay_add_phone_end, end)
    }

    fun enterEmailAddress(email: String) {
        enterText(R.id.med_d_copay_email, email)
    }

    fun tapSavePaymentInfo() {
        IntegrationUtil.waitUIWithDelayed(500)
        tapButtonId(R.id.med_d_copay_save)
    }

    fun scrollDown() {
        onView(withId(R.id.scroll_view)).perform(ViewActions.swipeUp())
        waitForSeconds(1)
    }

    private fun enterText(resId: Int, text: String) {
        onView(withId(resId)).perform(typeText(text), closeSoftKeyboard())
    }

    private fun verifyAnyTextOnView(resId: Int) {
        onView(withId(resId)).check(matches(not(withText(""))))
    }
}

fun collectPaymentScreen(block: CollectPaymentInfoRobot.() -> Unit) = CollectPaymentInfoRobot().apply(block)
