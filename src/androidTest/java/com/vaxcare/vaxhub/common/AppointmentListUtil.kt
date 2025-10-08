/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.matchers.withDrawable
import com.vaxcare.vaxhub.common.matchers.withDrawableWithTintColor
import com.vaxcare.vaxhub.common.matchers.withTextIgnoringCase
import com.vaxcare.vaxhub.core.extension.toOrdinalDate
import com.vaxcare.vaxhub.data.TestPatients
import org.hamcrest.Matchers
import java.time.LocalDateTime
import javax.inject.Inject

class AppointmentListUtil @Inject constructor() : TestUtilBase() {
    private val context by lazy { ApplicationProvider.getApplicationContext<Context>() }
    private val preloadContainerId = R.id.preload_container
    private val rightIcon2Id = R.id.right_icon2
    private val calendarValueId = R.id.calendar_value
    private val dateTimeId = R.id.date_time
    private val recylcerViewId = R.id.recycler_view
    private val emptyListMessage = R.id.empty_message
    private val addFabId = R.id.fab_add
    private val lookupFabId = R.id.fab_lookup

    // List Item IDs
    private val checkedOutBgId = R.id.checked_out_bg
    private val patientNameId = R.id.patient_name
    private val patientLastNameId = R.id.patient_last_name
    private val patientFirstNameId = R.id.patient_first_name
    private val medDTagId = R.id.med_d_tag

    /**
     * Wait for the Appointment list data to be loaded.
     */
    fun waitAppointmentLoadingFinish() {
        IntegrationUtil.waitForElementToGone(
            Espresso.onView(ViewMatchers.withId(preloadContainerId)),
            "Preload Container",
            IntegrationUtil.WAIT_TIME_FOR_LOAD
        )
    }

    /**
     * Refresh the appointment list.
     */
    fun refreshAppointmentList() {
        IntegrationUtil.simpleClick(Espresso.onView(ViewMatchers.withId(rightIcon2Id)))
    }

    /**
     * Navigate to the appointment list search screen
     *
     */
    fun tapMagnifyingGlassIconToSearch() {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            Espresso.onView(ViewMatchers.withId(lookupFabId)),
            "TapFabLookupToSearch",
            IntegrationUtil.WAIT_TIME_FOR_LOAD
        )
    }

    /**
     * Navigate to the appointment list add screen
     *
     */
    fun tapPurplePlusIconToAdd() {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            Espresso.onView(ViewMatchers.withId(addFabId)),
            "TapFabLookupToSearch",
            IntegrationUtil.WAIT_TIME_DEFAULT
        )
    }

    /**
     * Get the number of patients in the list based on patient name
     *
     * @param testPatient Test Patient used in test
     */
    fun tapFirstElementInAppointmentListByPatientName(testPatient: TestPatients) {
        IntegrationUtil.clickFirstElementInAppointmentListByPatientName(
            ViewMatchers.withId(recylcerViewId),
            "Patients grid",
            testPatient
        )
    }

    /**
     * Get the number of patients in the list based on patient name
     *
     * @param testPatient Test Patient used in test
     */
    fun tapLastElementInAppointmentListByPatientName(testPatient: TestPatients) {
        IntegrationUtil.clickLastElementInAppointmentListByPatientName(
            ViewMatchers.withId(recylcerViewId),
            "Patients grid",
            testPatient
        )
    }

    /**
     * Verify texts displayed on 'Select Patient' screen.
     */
    fun verifyToAppointmentListFragmentScreen(needToRefresh: Boolean = false) {
        val toolbarTitleText = context.resources.getString(R.string.patient_select_patient)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)

        if (needToRefresh) {
            IntegrationUtil.waitForElementToDisplayedAndClick(Espresso.onView(ViewMatchers.withId(rightIcon2Id)))
        }

        IntegrationUtil.waitForElementToGone(
            Espresso.onView(ViewMatchers.withId(preloadContainerId)),
            "Preload Container",
            IntegrationUtil.WAIT_TIME_FOR_LOAD
        )

        val isShowEmpty = IntegrationUtil.waitElementToViewMatcher(
            Espresso.onView(ViewMatchers.withId(emptyListMessage)),
            ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)
        )
        if (!isShowEmpty) {
            IntegrationUtil.waitForElementToDisplayed(
                Espresso.onView(ViewMatchers.withId(recylcerViewId)),
                "RecyclerView",
                IntegrationUtil.WAIT_TIME_DEFAULT
            )
        }
    }

    /**
     * because an appt is created on portal and it takes couple of seconds to fetch eligibilty.
     * will reload if eligibility is not back from portal
     *
     * @param testPatient
     * @param canReloadMaxTimes
     */
    fun checkReloadForAppointmentListIfEligibilityNotBack(testPatient: TestPatients, canReloadMaxTimes: Int = 1) {
        var reloadTime = 0
        while (reloadTime <= canReloadMaxTimes) {
            val isTimeUp = IntegrationUtil.waitForEligibilityHasBackInAppointmentList(
                ViewMatchers.withId(recylcerViewId),
                testPatient,
            )
            if (isTimeUp && reloadTime < canReloadMaxTimes) {
                refreshAppointmentList()
                waitAppointmentLoadingFinish()
                verifyToAppointmentListFragmentScreen()
            } else {
                break
            }
            reloadTime++
        }
    }

    /**
     * Verify that you land on the schedule grid for the current date of service
     */
    fun verifyScheduleGridForCurrentDate() {
        val dateText = LocalDateTime.now().toOrdinalDate(" ")
        IntegrationUtil.verifyTextDisplayed(ViewMatchers.withId(dateTimeId), dateText)
        // calendar icon should display current day
        val calendarText = LocalDateTime.now().dayOfMonth.toString()
        IntegrationUtil.verifyTextDisplayed(ViewMatchers.withId(calendarValueId), calendarText)
    }

    /**
     * Verify a checked out patient appears and visit is highlighted purple
     *
     * @param testPatient Test Patient used in test
     */
    fun verifyCheckedOutPatientVisitHighlightedPurple(testPatient: TestPatients) {
        IntegrationUtil.verifyAndScrollToElementInRecyclerView(
            ViewMatchers.withId(recylcerViewId),
            "Patients grid",
            ViewMatchers.hasDescendant(
                Matchers.allOf(
                    Matchers.allOf(
                        ViewMatchers.withId(checkedOutBgId),
                        ViewMatchers.isDisplayed()
                    ),
                    ViewMatchers.hasSibling(
                        ViewMatchers.hasDescendant(
                            Matchers.allOf(
                                ViewMatchers.withId(patientNameId),
                                withTextIgnoringCase(testPatient.completePatientName)
                            )
                        )
                    )
                )
            ),
            "TestPatient"
        )
    }

    /**
     * On app grid, verify purple stock pill with text- ‘PRIVATE' gets displayed.
     */
    fun verifyPurpleStockPillWithTextPrivate(testPatient: TestPatients) {
        val textPrivateMatcher = Matchers.allOf(
            ViewMatchers.hasDescendant(
                Matchers.allOf(
                    ViewMatchers.withText("PRIVATE"),
                    withDrawableWithTintColor(
                        R.drawable.bg_rounded_corner_purple,
                        R.color.list_purple
                    )
                )
            ),
            ViewMatchers.hasDescendant(
                Matchers.allOf(
                    ViewMatchers.withId(patientNameId),
                    Matchers.not(ViewMatchers.isDisplayed())
                )
            ),
            ViewMatchers.hasDescendant(
                Matchers.allOf(
                    ViewMatchers.withId(patientLastNameId),
                    ViewMatchers.isDisplayed()
                )
            ),
            ViewMatchers.hasDescendant(
                Matchers.allOf(
                    ViewMatchers.withId(patientFirstNameId),
                    ViewMatchers.isDisplayed()
                )
            )
        )

        IntegrationUtil.checkAppointmentListItemByPatientName(
            ViewMatchers.withId(recylcerViewId),
            "Appointment List",
            testPatient,
            textPrivateMatcher
        )
    }

    /**
     * On app grid, verify purple stock pill with text- ‘PRIVATE' gets displayed.
     */
    fun verifyGrayMEDDTag(testPatient: TestPatients) {
        val grayMEDDTagMatcher = ViewMatchers.hasDescendant(
            Matchers.allOf(
                ViewMatchers.withId(medDTagId),
                ViewMatchers.withText("MED D"),
                withDrawable(
                    R.drawable.bg_rounded_corner_gray
                )
            )
        )
        IntegrationUtil.checkAppointmentListItemByPatientName(
            ViewMatchers.withId(recylcerViewId),
            "Appointment List",
            testPatient,
            grayMEDDTagMatcher
        )
    }
}
