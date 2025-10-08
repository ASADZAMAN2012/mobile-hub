/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.createpatient

import androidx.test.espresso.matcher.ViewMatchers.withId
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.matchers.withTextIgnoringCase
import com.vaxcare.vaxhub.common.robot.BaseTestRobot
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.data.TestPatients
import org.hamcrest.Matchers

class CurbsideConfirmPatientInfoRobot : BaseTestRobot() {
    fun verifyTitleAndInfo(patient: TestPatients) {
        verifyToolbarTitle(R.string.patient_confirm_patient_info)
        waitForViewToGone(
            resId = R.id.loading,
            userFriendlyName = "Loading"
        )

        verifyViewsOnScreen(
            secondsToWait = 2,
            Matchers.allOf(
                withId(R.id.patient_name),
                withTextIgnoringCase(patient.completePatientName)
            ),
            Matchers.allOf(
                withId(R.id.patient_dob),
                withTextIgnoringCase(patient.dateOfBirth.toLocalDateString("MM/dd/yyyy"))
            ),
            // we are not checking other fields since testPatients are all using the default
        )
    }
}

fun curbsideConfirmPatientInfoScreen(block: CurbsideConfirmPatientInfoRobot.() -> Unit) =
    CurbsideConfirmPatientInfoRobot().apply(block)
