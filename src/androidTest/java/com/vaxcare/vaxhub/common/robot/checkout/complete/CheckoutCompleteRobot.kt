/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.checkout.complete

import android.content.res.Resources
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.robot.BaseTestRobot
import com.vaxcare.vaxhub.data.TestPatients

class CheckoutCompleteRobot : BaseTestRobot() {
    fun verifyTitle() {
        waitForViewToAppear(R.id.checkout_complete_title, 2)
        verifyTextResourceOnView(
            viewResId = R.id.checkout_complete_title,
            textResId = R.string.patient_checkout_complete
        )
    }

    fun verifyPatientName(testPatient: TestPatients) {
        waitAndVerifyTextOnView(
            viewResId = R.id.checkout_complete_patient_name,
            textToVerify = testPatient.completePatientName,
            userFriendlyName = "Patient Name",
            ignoreCase = true
        )
    }

    fun verifyNumberOfShotsAdministered(shotTotal: Int, resources: Resources) {
        val expectedString =
            resources.getQuantityString(R.plurals.shot_administered, shotTotal, shotTotal)
        waitAndVerifyTextOnView(
            viewResId = R.id.patient_shot_administered,
            textToVerify = expectedString,
            userFriendlyName = "Shot Administered Text"
        )
    }

    fun tapCheckoutAnotherPatient() {
        waitForViewToAppearAndPerformAction(
            viewInteraction = Espresso.onView(withId(R.id.check_out_another)),
            userFriendlyName = "Checkout Another Button",
            action = click()
        )
    }
}

fun checkoutCompleteScreen(block: CheckoutCompleteRobot.() -> Unit) = CheckoutCompleteRobot().apply(block)
