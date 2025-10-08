/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.checkout

import android.view.View
import androidx.core.view.isVisible
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.CheckOutUtil
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.RiskIconConstant
import com.vaxcare.vaxhub.common.matchers.withDrawable
import com.vaxcare.vaxhub.common.matchers.withDrawableWithTintColorByInstrumentation
import com.vaxcare.vaxhub.common.matchers.withTextIgnoringCase
import com.vaxcare.vaxhub.common.robot.BaseTestRobot
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
import com.vaxcare.vaxhub.model.MedDCheckResponse
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf

class CheckoutRobot : BaseTestRobot() {
    override val recyclerViewResId: Int = R.id.rv_vaccines
    private val checkoutUtil = CheckOutUtil()

    fun verifyTitle() {
        verifyToolbarTitle(R.string.scan_doses)
    }

    fun verifyAppointmentInfo(appointmentInfo: AppointmentInfo) {
        waitForSeconds(1)
        verifyPatientFirstAndLastName(appointmentInfo.testPatient)
        verifyRiskIcon(appointmentInfo.riskIconConstant)
        verifyEligibilityText(appointmentInfo.eligibilityText)
        verifyResponsibilityText(appointmentInfo.responsibilityText)
        verifyPayerText(appointmentInfo.payerText)
        verifyEligibilityCTAText(appointmentInfo.eligibilityCtaText)
        verifyMedDCTAText(appointmentInfo.medDCtaText)
        if (!appointmentInfo.hasMedDRan) {
            verifyRunCopayText()
        }
        verifyPatientInfoMedDTag(appointmentInfo.isMedDTagShow)
    }

    fun verifyPatientFirstAndLastName(testPatient: TestPatients) {
        val patientFullName = testPatient.lastName.plus(", ").plus(testPatient.firstName)
        onView(withId(R.id.patient_name))
            .check(matches(withTextIgnoringCase(patientFullName)))
    }

    fun verifyRiskIcon(riskIconConstant: RiskIconConstant) {
        val riskIconMatcher: Matcher<View> = allOf(
            withDrawableWithTintColorByInstrumentation(
                riskIconConstant
            ),
            ViewMatchers.isDisplayed()
        )

        onView(withId(R.id.eligibility_icon)).check(matches(riskIconMatcher))
    }

    fun verifyEligibilityText(text: String) {
        if (text.isNotEmpty()) {
            waitAndVerifyTextOnView(R.id.eligibility_text, text, "Eligibility Text")
        }
    }

    fun verifyResponsibilityText(text: String) {
        if (text.isNotEmpty()) {
            waitAndVerifyTextOnView(R.id.responsibility_text, text, "Responsibility Text")
        }
    }

    fun verifyPayerText(text: String) {
        if (text.isNotEmpty()) {
            waitAndVerifyTextOnView(R.id.payer_text, text, "Payer Text")
        }
    }

    fun verifyEligibilityCTAText(text: String) {
        if (text.isNotEmpty()) {
            waitAndVerifyTextOnView(R.id.eligibility_cta, text, "Eligibility CTA Text")
        }
    }

    fun verifyMedDCTAText(text: String) {
        if (text.isNotEmpty()) {
            waitAndVerifyTextOnView(R.id.med_d_cta, text, "Med D CTA Text")
        }
    }

    fun verifyRunCopayText() {
        verifyTextResourceOnView(R.id.run_copay_btn, R.string.run_copay_check)
    }

    fun verifyPatientInfoMedDTag(isShown: Boolean) {
        val medTagMatcher = if (isShown) {
            allOf(
                withText(R.string.med_d),
                withDrawable(R.drawable.bg_rounded_corner_gray),
                ViewMatchers.isDisplayed()
            )
        } else {
            Matchers.not(ViewMatchers.isDisplayed())
        }

        onView(withId(R.id.med_d_tag)).check(matches(medTagMatcher))
    }

    fun tapRunCopayCheckButton() {
        tapButtonId(R.id.run_copay_btn)
    }

    fun tapCloseAppointmentButton() {
        waitForViewToAppearAndPerformAction(
            viewInteraction = onView(withId(R.id.toolbar_icon)),
            userFriendlyName = "Close Checkout Button",
            additionalTime = 0,
            action = click()
        )
    }

    fun verifyMedDCopaysDisplayed(copays: List<MedDCheckResponse.Copay>) {
        assert(copays.isNotEmpty()) {
            "Expected to check MedDCoPay are displayed on Scan Doses. but copays is empty"
        }

        copays.forEach {
            val viewId = when (it.antigen) {
                MedDVaccines.RSV.value -> R.id.rsvCopay
                MedDVaccines.TDAP.value -> R.id.tdapCopay
                MedDVaccines.ZOSTER.value -> R.id.zosterCopay
                else -> 0
            }
            if (viewId != 0) {
                verifyViewsOnScreen(
                    3,
                    allOf(
                        withId(viewId),
                        withText(it.antigen),
                        hasSibling(withText("$${it.copay}"))
                    )
                )
            }
        }
    }

    fun addDose(testProduct: TestProducts) {
        checkoutUtil.performDoseActions(DoseAction.AddDose(testProduct))
    }

    fun selectSiteForDose(testProduct: TestProducts, site: TestSites) {
        checkoutUtil.performDoseActions(DoseAction.EditDose(testProduct, site))
    }

    fun verifyProductGrid(testProducts: List<TestProducts>, site: TestSites) {
        checkoutUtil.verifyDoseAddList(
            testProducts = testProducts,
            testSites = site
        )
    }

    fun tapArrowToCheckoutSummary() {
        val view = onView(withId(R.id.fab_next))
        waitForViewCondition(view) {
            it.isVisible && it.isEnabled
        }
        IntegrationUtil.simpleClick(view, "ArrowButton")
    }

    class UnorderedDoseDialogRobot : BaseTestRobot() {
        fun tapKeepDoseDialogButton() {
            IntegrationUtil.confirmAlertDialog(
                processingTextResId = R.string.orders_unordered_dose_prompt_message,
                userFriendlyName = "Unordered Dose Dialog",
                selectedStringResId = R.string.orders_unordered_dose_prompt_yes
            )
        }
    }

    class CopayRequiredDialogRobot : BaseTestRobot() {
        fun verifyCopayDialog() {
            confirmDialog(false)
        }

        fun tapRunCopayDialogButton() {
            confirmDialog(true)
        }

        private fun confirmDialog(tapToDismiss: Boolean) {
            IntegrationUtil.confirmAlertDialog(
                titleTextResId = R.string.med_d_review_copay_dialog_header,
                processingTextResId = R.string.med_d_review_copay_dialog_body,
                userFriendlyName = "Requires a Copay modal dialog",
                positiveTintColor = R.color.primary_purple,
                positiveViewResId = R.id.button_run,
                selectedStringResId = if (tapToDismiss) R.string.med_d_review_copay_dialog_run else null
            )
        }

        fun verifyViewDisableWhenBehindDialog() {
            val disableViewSet = mutableSetOf<Boolean>()
            try {
                onView(withId(R.id.patient_checkout_lot_search_btn)).perform(ViewActions.click())
            } catch (e: Exception) {
                if (e is NoMatchingViewException) {
                    disableViewSet.add(false)
                }
            }
            try {
                onView(withId(R.id.fab_next)).perform(ViewActions.click())
            } catch (e: Exception) {
                if (e is NoMatchingViewException) {
                    disableViewSet.add(false)
                }
            }
            assert(disableViewSet.size == 1 && disableViewSet.contains(false)) {
                "Expected search button and next button are not clickable/disabled.. But it was enabled."
            }
        }
    }
}

/**
 * Object representing expected values to see in the PatientInfoDrawer
 */
data class AppointmentInfo(
    val testPatient: TestPatients,
    val riskIconConstant: RiskIconConstant,
    val eligibilityText: String,
    val responsibilityText: String,
    val payerText: String,
    val eligibilityCtaText: String,
    val medDCtaText: String,
    val isMedDTagShow: Boolean,
    val hasMedDRan: Boolean
)

fun checkoutDosesScreen(block: CheckoutRobot.() -> Unit) = CheckoutRobot().apply(block)

fun unorderedDoseDialog(block: CheckoutRobot.UnorderedDoseDialogRobot.() -> Unit) =
    CheckoutRobot.UnorderedDoseDialogRobot().apply(block)

fun copayRequiredDialog(block: CheckoutRobot.CopayRequiredDialogRobot.() -> Unit) =
    CheckoutRobot.CopayRequiredDialogRobot().apply(block)
