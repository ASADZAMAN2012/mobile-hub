/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.checkout.summary

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.simpleClick
import com.vaxcare.vaxhub.common.matchers.CheckSummaryListMatcher
import com.vaxcare.vaxhub.data.PaymentModals
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
import com.vaxcare.vaxhub.model.MedDCheckResponse
import org.hamcrest.Matcher

class CheckoutSummaryRobot : BaseCheckoutSummaryRobot() {
    override val recyclerViewResId: Int = R.id.rv_vaccines

    override fun verifyTitle() {
        verifyToolbarTitle(R.string.summary)
    }

    fun tapCheckoutButton() {
        IntegrationUtil.clickOnNotFullyVisibleElement(
            elementToCheck = onView(withId(R.id.checkout_btn)),
            userFriendlyName = "Checkout Button"
        )
    }

    fun verifyCopaySubTotalAndAddedDose(
        testPartners: TestPartners,
        testPatient: TestPatients,
        testProducts: List<TestProducts>,
        testSites: TestSites,
        medDCheckResponse: MedDCheckResponse? = null
    ) {
        val copayValue =
            medDCheckResponse?.let { getCopayTotalValue(medDCheckResponse, testProducts) }
        onView(withId(recyclerViewResId)).check(
            matches(
                CheckSummaryListMatcher(
                    testPartners,
                    testPatient,
                    testProducts,
                    testSites,
                    copayValue
                ) as Matcher<in View>
            )
        )
    }

    fun tapCollectPaymentInfo() {
        IntegrationUtil.delayedClick(withId(R.id.collect_payment_info))
    }

    class CopayDialogRobot {
        init {
            IntegrationUtil.waitForElementToAppear(
                elementToCheck = onView(
                    withText(R.string.med_d_copay_dialog_title)
                ).inRoot(isDialog()),
                userFriendlyName = "Copay Required Dialog",
                secondsToWait = 5
            )
        }

        fun tapCashOrCheck() {
            simpleClick(onView(withText(PaymentModals.PaymentCashOrCheck.display)))
        }

        fun tapDebitOrCredit() {
            simpleClick(onView(withText(PaymentModals.PaymentDebitOrCredit().display)))
        }
    }
}

fun checkoutDosesSummaryScreen(block: CheckoutSummaryRobot.() -> Unit) = CheckoutSummaryRobot().apply(block)

fun CheckoutSummaryRobot.copayDialogRequiredDialog(block: CheckoutSummaryRobot.CopayDialogRobot.() -> Unit) =
    apply { CheckoutSummaryRobot.CopayDialogRobot().apply(block) }
