/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.checkout.summary

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.matchers.CheckPaymentSummaryListMatcher
import com.vaxcare.vaxhub.data.PaymentModals
import com.vaxcare.vaxhub.data.TestCards
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.model.MedDCheckResponse
import org.hamcrest.Matcher

class PaymentSummaryRobot : BaseCheckoutSummaryRobot() {
    override val recyclerViewResId: Int = R.id.rv_vaccines

    override fun verifyTitle() {
        verifyToolbarTitle(R.string.med_d_summary_title)
    }

    fun tapSignatureCaptureButton() {
        tapButtonId(R.id.fab_next)
    }

    fun verifyCopaySubTotalAndAddedDose(
        testPatient: TestPatients,
        testProducts: List<TestProducts>,
        paymentModal: PaymentModals,
        medDCheckResponse: MedDCheckResponse,
        cardInfo: TestCards? = null
    ) {
        val copayValue = getCopayTotalValue(medDCheckResponse, testProducts)
        onView(withId(recyclerViewResId)).check(
            matches(
                CheckPaymentSummaryListMatcher(
                    testPatients = testPatient,
                    testProducts = testProducts,
                    copayValue = copayValue,
                    paymentModals = paymentModal,
                    cardInfo = cardInfo
                ) as Matcher<in View>
            )
        )
    }
}

fun paymentSummaryScreen(block: PaymentSummaryRobot.() -> Unit) = PaymentSummaryRobot().apply(block)
