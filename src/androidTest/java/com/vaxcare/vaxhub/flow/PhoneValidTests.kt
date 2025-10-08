/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow

import android.os.Bundle
import android.view.View
import androidx.hilt.work.HiltWorkerFactory
import androidx.navigation.Navigation.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.matchers.withDrawable
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.ui.Main
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PhoneValidTests : TestsBase() {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val testWorkManagerHelper = TestWorkManagerHelper()

    private lateinit var scenario: ActivityScenario<Main>

    @Before
    fun beforeTests() {
        hiltRule.inject()
        testWorkManagerHelper.startAllWorkers(workerFactory)
        scenario = ActivityScenario.launch(Main::class.java)
        scenario.onActivity {
            findNavController(
                it,
                R.id.nav_host
            ).navigate(
                R.id.patientCollectPhoneFragment,
                Bundle().apply { putParcelable("data", PatientCollectData()) }
            )
        }
    }

    @Test
    fun testCheckPhoneValid() {
        onView(withId(R.id.patient_add_phone_start)).perform(
            typeText("111"),
            closeSoftKeyboard()
        )
        onView(withId(R.id.patient_add_phone_mid)).perform(
            typeText("222"),
            closeSoftKeyboard()
        )
        // check the wrong input
        onView(withId(R.id.patient_add_phone_end)).perform(
            typeText("666")
        )
        IntegrationUtil.waitUIWithDelayed()
        // check input area background and text displayed.
        onView(
            withId(R.id.layoutCollectPhone)
        ).check(matches(withDrawable(R.drawable.bg_rounded_corner_lightest_yellow)))
        onView(withId(R.id.patient_add_phone_invalid)).check(matches(isDisplayed()))
        // check button enable/disable
        onView(withId(R.id.continueBtn)).check { view, _ ->
            Assert.assertFalse(view.isEnabled)
        }
        // check the correct input
        onView(withId(R.id.patient_add_phone_end)).perform(clearText())
        onView(withId(R.id.patient_add_phone_end)).perform(
            typeText("5555")
        )
        Thread.sleep(2000)
        // check input area background and text displayed.
        onView(withId(R.id.layoutCollectPhone)).check(matches(withDrawable(R.drawable.bg_rounded_corner_white)))
        onView(withId(R.id.patient_add_phone_invalid)).check { view, _ ->
            Assert.assertFalse(view.visibility == View.VISIBLE)
        }
        // check button enable/disable
        onView(withId(R.id.continueBtn)).check { view, _ ->
            Assert.assertTrue(view.isEnabled)
        }
    }
}
