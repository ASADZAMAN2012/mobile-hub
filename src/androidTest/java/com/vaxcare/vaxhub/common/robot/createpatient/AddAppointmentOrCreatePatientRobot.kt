/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.createpatient

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.WAIT_TIME_DEFAULT
import com.vaxcare.vaxhub.common.IntegrationUtil.Companion.WAIT_TIME_FOR_LOAD
import com.vaxcare.vaxhub.common.matchers.withTextIgnoringCase
import com.vaxcare.vaxhub.common.robot.BaseTestRobot
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.data.TestPatients
import org.hamcrest.Matchers

class AddAppointmentOrCreatePatientRobot : BaseTestRobot() {
    fun verifyTitleAndCreateButton() {
        verifyToolbarTitle(R.string.add_appointment_title)
        waitForAndVerifyViewToAppear(
            element = withId(R.id.button_createNewPatient),
            userFriendlyName = "Create New Patient Button"
        )
    }

    fun searchForPatientAndClick(patient: TestPatients) {
        onView(withId(R.id.editText_searchAppointment)).perform(typeText(patient.firstName))

        waitForViewToGone(
            resId = R.id.frameLayout_loading,
            userFriendlyName = "Loading",
            additionalTime = WAIT_TIME_FOR_LOAD
        )
        IntegrationUtil.waitForElementInRecyclerView(
            recyclerView = withId(R.id.recyclerView_patientsFound),
            userFriendlyName = "Patient Search Result",
            elemInRecyclerView = Matchers.allOf(
                withId(R.id.patient_name),
                withTextIgnoringCase(patient.completePatientName)
            )
        )
        waitForSeconds(WAIT_TIME_DEFAULT)
        IntegrationUtil.scrollToElementInRecyclerViewAndClick(
            recyclerView = withId(R.id.recyclerView_patientsFound),
            recyclerViewName = "Patient Search Results",
            elemInRecyclerView = Matchers.allOf(
                withId(R.id.patient_last_name),
                withTextIgnoringCase("${patient.lastName.captureName()},")
            ),
            friendlyName = "Test Patient"
        )
    }

    fun tapCreatePatientButton() {
        waitForViewToAppearAndPerformAction(
            resId = R.id.button_createNewPatient,
            userFriendlyName = "Create New Patient Button",
            action = click()
        )
    }

    fun tapIUnderstandButton() {
        waitForViewToAppearAndPerformAction(
            resId = R.id.button_ok,
            userFriendlyName = "I Understand button",
            action = click()
        )
    }
}

fun addAppointmentOrCreatePatientScreen(block: AddAppointmentOrCreatePatientRobot.() -> Unit) =
    AddAppointmentOrCreatePatientRobot().apply(block)
