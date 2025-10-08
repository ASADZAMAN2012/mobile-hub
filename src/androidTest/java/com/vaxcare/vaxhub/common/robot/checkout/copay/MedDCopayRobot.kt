/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.checkout.copay

import android.content.res.Resources
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.CheckOutUtil
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.matchers.withDrawable
import com.vaxcare.vaxhub.common.matchers.withTextIgnoringCase
import com.vaxcare.vaxhub.common.matchers.withTextOR
import com.vaxcare.vaxhub.common.robot.BaseTestRobot
import com.vaxcare.vaxhub.model.MedDCheckResponse
import org.hamcrest.Matchers.allOf
import java.math.RoundingMode

class MedDCopayRobot : BaseTestRobot() {
    fun waitAndVerifyResultsTitle() {
        waitForProgressBarToGone()
        verifyToolbarTitle(R.string.med_d_check_title, 5)
    }

    fun verifyResultsHeaderText() {
        verifyTextResourceOnView(
            R.id.results_header_text,
            R.string.med_d_check_checked_prompt
        )
    }

    fun verifyAndFillInSSNIfNeeded() {
        val needsSsn = IntegrationUtil.isElementToViewMatcher(
            onView(withText(R.string.med_d_check_prompt)),
            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )

        if (needsSsn) {
            verifyToolbarTitle(R.string.med_d_copay_check)
            val hasMbiField = IntegrationUtil.isElementToViewMatcher(
                onView(withText(R.string.med_d_check_mbi_or_ssn)),
                withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
            )
            if (hasMbiField) {
                fillInSsnMbiField("123121234")
            } else {
                fillInSsnStart("123")
                fillInSsnMid("12")
                fillInSsnEnd("1234")
            }
            tapNextButton()
        }

        waitForViewCondition(onView(withId(R.id.fab_next)), 8) { it.isEnabled }
    }

    private fun fillInSsnStart(text: String) {
        waitForViewToAppearAndPerformAction(
            resId = R.id.ssn_start,
            userFriendlyName = "SSN Start",
            action = ViewActions.typeText(text)
        )
    }

    private fun fillInSsnMid(text: String) {
        waitForViewToAppearAndPerformAction(
            resId = R.id.ssn_mid,
            userFriendlyName = "SSN Middle",
            action = ViewActions.typeText(text)
        )
    }

    private fun fillInSsnEnd(text: String) {
        waitForViewToAppearAndPerformAction(
            resId = R.id.ssn_end,
            userFriendlyName = "SSN End",
            action = ViewActions.typeText(text)
        )
    }

    private fun fillInSsnMbiField(text: String) {
        waitForViewToAppearAndPerformAction(
            resId = R.id.input_value,
            userFriendlyName = "MbiOrSsn",
            action = ViewActions.typeText(text)
        )
    }

    fun tapNextButton() {
        IntegrationUtil.waitForElementToEnabled(
            onView(withId(R.id.fab_next)),
            secondsToWait = IntegrationUtil.WAIT_TIME_FOR_LOAD
        )
        tapButtonId(R.id.fab_next)
    }

    fun tapCloseButton() {
        tapButtonId(R.id.toolbar_icon)
    }

    fun verifyCopayResults(copays: List<MedDCheckResponse.Copay>, resources: Resources) {
        if (copays.isEmpty()) {
            IntegrationUtil.verifyAndScrollToElementInRecyclerView(
                withId(R.id.rv_med_d_results),
                "Copays grid",
                allOf(
                    withId(R.id.med_d_vaccine_name),
                    withTextOR(CheckOutUtil.MED_D_ZOSTER, CheckOutUtil.MED_D_TDAP)
                ),
                "${CheckOutUtil.MED_D_ZOSTER} or ${CheckOutUtil.MED_D_TDAP}",
            )
        }
        copays.forEach { copay ->
            val copayName = if (copay.productName == null) {
                copay.antigen
            } else {
                "${copay.antigen} (${copay.productName})"
            }
            val copayAmount = resources.getString(
                R.string.med_d_check_copay,
                copay.copay.setScale(2, RoundingMode.HALF_UP).toDouble()
            )
            verifyViewsOnScreen(
                2,
                allOf(
                    withId(R.id.med_d_vaccine_name),
                    withTextIgnoringCase(copayName)
                ),
                hasSibling(
                    allOf(
                        withId(R.id.med_d_vaccine_price),
                        withTextIgnoringCase(copayAmount)
                    )
                ),
                hasSibling(
                    allOf(
                        withId(R.id.med_d_vaccine_icon),
                        withDrawable(R.drawable.ic_presentation_prefilled_syringe)
                    )
                )
            )
        }
    }
}

fun medDCopayScreen(block: MedDCopayRobot.() -> Unit) = MedDCopayRobot().apply(block)
