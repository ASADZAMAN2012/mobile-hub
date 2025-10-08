/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import com.vaxcare.vaxhub.R

class CalendarUtil : TestUtilBase() {
    private val context by lazy { ApplicationProvider.getApplicationContext<Context>() }

    /**
     * Click Calendar icon on scheduler page
     */
    fun clickCalendarIcon() {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            Espresso.onView(ViewMatchers.withId(R.id.calendar_item_container)),
            "Calendar Icon"
        )
    }

    /**
     * Click Calendar Back navigation button
     */
    fun clickCalendarBackButton() {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            Espresso.onView(ViewMatchers.withId(R.id.navigate_back)),
            "Calendar Back navigation button"
        )
    }

    /**
     * Click Calendar Forward navigation button
     */
    fun clickCalendarForwardButton() {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            Espresso.onView(ViewMatchers.withId(R.id.navigate_forward)),
            "Calendar Forward navigation button"
        )
    }

    /**
     * Waiting for the loading box to disappear.
     */
    fun waitForLoadingGone() {
        IntegrationUtil.waitForElementToGone(
            Espresso.onView(ViewMatchers.withId(R.id.preload_container)),
            "Preload container",
            IntegrationUtil.WAIT_TIME_FOR_LOAD
        )
    }

    /**
     * Selected a random date in calendar.
     */
    fun selectedRandomDateInCalendar(position: Int) {
        waitForLoadingGone()
        val toolbarTitleText = context.resources.getString(R.string.patient_select_patient)
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
        clickCalendarIcon()
        IntegrationUtil.waitForCalendarViewElementToAppearAndClick(
            ViewMatchers.withId(R.id.rv_month),
            position = position
        )
    }
}
