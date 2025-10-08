/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.appointmentlist

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.PatientUtil
import com.vaxcare.vaxhub.common.RiskIconConstant
import com.vaxcare.vaxhub.common.matchers.AppointmentEligibilityIconAndTagMatcher
import com.vaxcare.vaxhub.common.robot.BaseTestRobot
import com.vaxcare.vaxhub.data.TestBackground
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestStockPill
import com.vaxcare.vaxhub.model.CallToAction
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import timber.log.Timber

class AppointmentListRobot : BaseTestRobot() {
    private val patientUtil = PatientUtil()

    fun verifyTitleAndAppointmentList() {
        verifyToolbarTitle(R.string.patient_select_patient)
        waitForProgressBarToGone(R.id.preload_container)
        val isListEmpty = waitForElementToMatch(
            Espresso.onView(withId(recyclerViewResId)),
            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )

        if (!isListEmpty) {
            waitForViewToAppear(recyclerViewResId)
        }
    }

    fun manuallyRunMedDCheck(appointmentId: Int) {
        runBlocking { patientUtil.runMedDCheck(appointmentId) }
    }

    fun searchForCreatedAppointment(testPatient: TestPatients, maxRefreshAttempts: Int) {
        var sentinel = 0
        do {
            assert(sentinel++ < maxRefreshAttempts) {
                "Appointment for ${testPatient.completePatientName} was never found after $maxRefreshAttempts attempts"
            }
            tapRefreshAppointmentsButton()
        } while (!isPatientInList(testPatient))
    }

    fun tapRefreshAppointmentsButton() {
        waitForViewToAppearAndPerformAction(
            resId = R.id.right_icon2,
            userFriendlyName = "refresh button",
            action = click()
        )
        waitForProgressBarToGone(R.id.preload_container)
    }

    private fun isPatientInList(testPatient: TestPatients): Boolean =
        IntegrationUtil.isEligibilityHasBackInAppointmentList(
            recyclerView = withId(recyclerViewResId),
            testPatient = testPatient
        )

    fun verifyCorrectMedDCallToActionInAppointmentData(appointmentId: String, expectedCallToAction: CallToAction) {
        val appointmentData = patientUtil.getAppointmentById(appointmentId)
        val callToAction =
            appointmentData?.toAppointment()?.getMedDCta()
        assert(expectedCallToAction == callToAction) {
            "Expected to verify the callToAction-$expectedCallToAction " +
                "in medicarePartDRiskDetermination object. But was $callToAction"
        }
        Timber.d("Verified appt-$appointmentId has expected medD Call To Action-$expectedCallToAction")
    }

    fun verifyAppointmentIconAndTags(
        testPatient: TestPatients,
        expectedRiskIcon: RiskIconConstant,
        stockPillList: List<TestStockPill> = emptyList(),
        itemBackground: TestBackground = TestBackground.None
    ) {
        val matcher = AppointmentEligibilityIconAndTagMatcher(
            eligibility = expectedRiskIcon,
            testStockPillList = stockPillList,
            testBackground = itemBackground
        ) as Matcher<View>

        IntegrationUtil.checkAppointmentListItemByPatientName(
            recyclerView = withId(R.id.recycler_view),
            recyclerViewName = "Appointment List",
            testPatients = testPatient,
            matcher = matcher
        )
    }

    fun tapFirstAppointmentWithPatient(testPatient: TestPatients) {
        val indexFound = scrollRecyclerViewUntilCondition(false) { index ->
            IntegrationUtil.checkNameInAppointmentList(
                withId(recyclerViewResId),
                testPatient,
                index
            )
        }

        assert(indexFound != -1) {
            "Patient not found in appointment list!"
        }

        waitForViewToAppearAndPerformAction(
            resId = recyclerViewResId,
            userFriendlyName = "Appointment List Item",
            action = actionOnItemAtPosition<RecyclerView.ViewHolder>(indexFound, click())
        )
    }

    fun tapAddAppointmentButton() {
        waitForViewToAppearAndPerformAction(
            resId = R.id.fab_add,
            userFriendlyName = "Add Appointment Button",
            action = click()
        )
    }

    fun waitForEligibilityToSettle() {
        val secondsToWait = if (BuildConfig.BUILD_TYPE == "local") {
            1
        } else {
            15
        }

        waitForSeconds(secondsToWait)
    }

    /**
     * Gets the AppointmentId from a given TestPatient's name
     */
    fun getAppointmentIdFromPatientName(testPatient: TestPatients): Int =
        runBlocking {
            patientUtil.getAppointmentList().firstOrNull {
                it.patient.lastName + it.patient.firstName == testPatient.lastName + testPatient.lastName
            }?.id ?: 0
        }
}

fun appointmentListScreen(block: AppointmentListRobot.() -> Unit) = AppointmentListRobot().apply(block)
