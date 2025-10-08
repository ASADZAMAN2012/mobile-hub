/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common.robot.home

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.PatientUtil
import com.vaxcare.vaxhub.common.robot.BaseTestRobot
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import org.hamcrest.Matchers
import org.junit.Assert
import timber.log.Timber

class HomeRobot : BaseTestRobot() {
    private val homeScreenUtil = HomeScreenUtil()
    private val patientUtil = PatientUtil()

    fun verifySplashScreenDisplayed() {
        when {
            IntegrationUtil.isElementDisplayed(onView(withText(R.string.bridge_required_title))) -> {
                // End the test flow
                Assert.assertFalse("Please install bridge app first!", true)
                IntegrationUtil.waitForElementToAppearAndClick(
                    onView(withText("OK")),
                    "Text 'OK' "
                )
            }

            else -> waitForAndVerifyViewToAppear(
                element = withId(R.id.splash_logo),
                userFriendlyName = "splash screen"
            )
        }
    }

    fun createAppointmentWithPatient(patient: TestPatients): String =
        try {
            patientUtil.getAppointmentIdByCreateTestPatient(patient, 0)
        } catch (e: Exception) {
            Timber.tag("TestStep").d("Exception POST Appt: $e\n continuing flow...")
            assert(e.message?.contains("timeout") == true)
            ""
        }

    fun tapBackgroundToOpenAppointmentList() {
        waitForViewToAppearAndPerformAction(
            resId = R.id.container,
            userFriendlyName = "root",
            additionalTime = 1,
            action = ViewActions.click()
        )
    }

    fun pinInUser(pin: String) {
        clickButtonsOnKeyPad(pin)
    }

    /**
     * Access admin access panel
     */
    fun swipeDownAdminAccessPanel() {
        waitForViewToAppearAndPerformAction(
            resId = R.id.container,
            userFriendlyName = "root",
            action = ViewActions.swipeDown()
        )
    }

    /**
     * Performs the location sync with incoming partner
     */
    @Deprecated("Please try to use the HomeScreenUtil equivalent")
    fun loginAsPartner(partner: TestPartners = TestPartners.RprdCovidPartner) {
        homeScreenUtil.loginAsTestPartner(partner)
    }
}

/**
 * Initialize the Home flow (login, etc.) and apply a block of HomeRobot functions
 */
fun homeScreen(block: HomeRobot.() -> Unit) = HomeRobot().apply(block)

fun HomeRobot.splashScreen(block: HomeRobot.() -> Unit) = apply(block)

class AdminAccessPanelRobot : BaseTestRobot() {
    fun tapAdminLoginButton() {
        waitForViewToAppearAndPerformAction(
            resId = R.id.admin_access_action,
            userFriendlyName = "AdminLoginButton",
            action = ViewActions.click()
        )
    }
}

class AdminLoginRobot : BaseTestRobot() {
    fun inputPassword(password: String) {
        waitForViewToAppearAndPerformAction(
            resId = R.id.password_input,
            userFriendlyName = "PasswordView",
            action = ViewActions.typeText(password)
        )
    }

    fun tapLogin() {
        tapButtonId(R.id.btn_login)
    }
}

class AdminDetailsRobot : BaseTestRobot() {
    fun enterPartnerId(partnerId: String) {
        waitForViewToAppearAndPerformAction(
            resId = R.id.btn_enter_partner_id,
            userFriendlyName = "EnterPartnerIdButton",
            action = ViewActions.click()
        )
        enterKeyPad(partnerId)
    }

    fun enterClinicId(clinicId: String) {
        waitForViewToAppearAndPerformAction(
            resId = R.id.btn_enter_clinic_id,
            userFriendlyName = "EnterClinicIdButton",
            action = ViewActions.click()
        )
        enterKeyPad(clinicId)
    }

    fun waitForSetupComplete() {
        waitForViewToGone(
            resId = R.id.partner_id_progress_bar,
            userFriendlyName = "partnerId Progress Bar"
        )
        waitForProgressBarToGone()
        waitForViewToAppearWithTextResource(R.string.admin_fragment_setup_complete)
    }

    fun verifyEditButtonsDisplayed() {
        waitForAndVerifyViewToAppear(
            element = withId(R.id.partner_id_edit),
            userFriendlyName = "PartnerIdEdit",
            timeoutInSeconds = 8
        )
        waitForAndVerifyViewToAppear(
            element = withId(R.id.clinic_id_edit),
            userFriendlyName = "ClinicIdEdit",
            timeoutInSeconds = 8
        )
    }

    fun verifyErrorText(isDisplayed: Boolean) {
        val visibilityMatcher = if (isDisplayed) {
            ViewMatchers.isDisplayed()
        } else {
            Matchers.not(ViewMatchers.isDisplayed())
        }
        ViewAssertions.matches(
            Matchers.allOf(
                withId(R.id.partner_id_error_label),
                visibilityMatcher
            )
        )
    }

    fun tapCloseButton() {
        waitForViewToAppearAndPerformAction(
            resId = R.id.toolbar_icon,
            userFriendlyName = "BackButton",
            additionalTime = 1,
            action = ViewActions.click()
        )
    }
}

fun adminAccessPanel(block: AdminAccessPanelRobot.() -> Unit) = AdminAccessPanelRobot().apply(block)

fun adminLoginScreen(block: AdminLoginRobot.() -> Unit) = AdminLoginRobot().apply(block)

fun adminDetailsScreen(block: AdminDetailsRobot.() -> Unit) = AdminDetailsRobot().apply(block)
