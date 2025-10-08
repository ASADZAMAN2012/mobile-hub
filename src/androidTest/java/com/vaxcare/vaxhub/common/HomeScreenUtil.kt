/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.vaxcare.vaxhub.EntryPointHelper.lazyEntryPoint
import com.vaxcare.vaxhub.HiltEntryPointInterface
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.robot.home.adminAccessPanel
import com.vaxcare.vaxhub.common.robot.home.adminDetailsScreen
import com.vaxcare.vaxhub.common.robot.home.adminLoginScreen
import com.vaxcare.vaxhub.common.robot.home.homeScreen
import com.vaxcare.vaxhub.common.robot.home.splashScreen
import com.vaxcare.vaxhub.data.TestPartners
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.hamcrest.Matchers.allOf
import org.junit.Assert

@EntryPoint
@InstallIn(ActivityComponent::class)
interface HomeScreenUtilEntryPoint : HiltEntryPointInterface {
    fun storageUtil(): StorageUtil
}

class HomeScreenUtil : TestUtilBase() {
    private val testWorkManagerHelper = TestWorkManagerHelper()
    private val defaultBogusPin = "1337"

    /**
     * Enter the PartnerID and Clinic ID information on Hub
     *
     * @param testPartner Test Partner used in test
     */
    fun setupPartnerAndClinic(testPartner: TestPartners = TestPartners.RprdCovidPartner) {
        when {
            isViewDisplayed(onView(withText(R.string.bridge_required_title))) -> {
                // End the test flow
                Assert.assertFalse("Please install bridge app first!", true)
                IntegrationUtil.waitForElementToAppearAndClick(
                    onView(withText("OK")),
                    "Text 'OK' "
                )
            }

            else -> {
                if (isViewDisplayed(onView(withId(R.id.splash_logo)))) {
                    enterPartnerAndClinicID(testPartner)
                }
            }
        }
    }

    private fun isViewDisplayed(viewInteraction: ViewInteraction): Boolean =
        IntegrationUtil.isElementDisplayed(viewInteraction)

    /**
     * Once correct Partner and Clinic ID are entered, verify the following
     - Progress Image displayed
     - Edit button gets displayed next to PartnerID and ClinicID
     - ID Mismatch NOT displayed
     - Click Close button and verify Partner name and Clinic name are displayed.
     *
     * @param testPartner Test Partner used in test
     */
    fun verifySuccessfulLoginFlow(testPartner: TestPartners) {
        waitForProgressBarContainerToGone()
        verifyEditButtonGetsDisplayedNextToPartnerIDAndClinicID()
        // ID Mismatch not displayed
        IntegrationUtil.verifyElementsNotPresentOnPage(
            2,
            allOf(
                withId(R.id.partner_id_error_label),
                withId(R.id.clinic_id_error_label)
            )
        )
        // Click Close button and verify Partner name and Clinic name are displayed.
        clickCloseButtonNextToVaxCareAdmin()
        IntegrationUtil.waitForElementToAppear(
            onView(withText(testPartner.partnerName)),
            "PartnerName",
            10
        )
        IntegrationUtil.verifyTextDisplayed(withId(R.id.partner_name), testPartner.partnerName)
        IntegrationUtil.verifyTextDisplayed(withId(R.id.clinic_name), testPartner.clinicName)
    }

    /**
     * wait for Load Progress to Gone
     */
    private fun waitForProgressBarContainerToGone() {
        IntegrationUtil.waitForElementToAppear(
            elementToCheck = onView(withId(R.id.loading)),
            userFriendlyName = "LoadProgress"
        )
        IntegrationUtil.waitForElementToNotVisible(
            elementToCheck = onView(withId(R.id.text_view)),
            userFriendlyName = "Setup Complete"
        )
    }

    /**
     * Once Incorrect Partner and Clinic ID are entered, verify the following
     - Edit button gets displayed next to PartnerID and ClinicID
     - ID Mismatch is displayed
     * Click Close button
     */
    fun verifyUnsuccessfulLoginFlow() {
        verifyEditButtonGetsDisplayedNextToPartnerIDAndClinicID()
        // ID Mismatch is displayed
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            withId(R.id.partner_id_error_label),
            withId(R.id.clinic_id_error_label)
        )
        clickCloseButtonNextToVaxCareAdmin()
    }

    /**
     * Clicks on Close button next to 'VaxCare Admin' after setting up Partner and Clinic
     * Waits for 'PartnerNameField' to appear after clicking on close button
     */
    private fun clickCloseButtonNextToVaxCareAdmin() {
        IntegrationUtil.waitForElementToAppearAndClick(
            onView(withId(R.id.toolbar_icon)),
            "CloseButtonNextTo'VaxcareAdmin'",
            10
        )
    }

    /**
     * Verify Edit button gets displayed next to PartnerID and ClinicID
     */
    private fun verifyEditButtonGetsDisplayedNextToPartnerIDAndClinicID() {
        // Edit button gets displayed next to PartnerID and ClinicID
        IntegrationUtil.verifyElementsPresentOnPage(
            2,
            withId(R.id.partner_id_edit),
            withId(R.id.clinic_id_edit)
        )
    }

    /**
     * Sets up Partner and Clinic
     *
     * @param testPartner Partner to login as. Default value is SecondBaptist
     *
     * @return the Partner the method logged in as
     */
    fun loginAsTestPartner(testPartner: TestPartners = TestPartners.RprdCovidPartner): TestPartners {
        homeScreen {
            splashScreen {
                verifySplashScreenDisplayed()
                swipeDownAdminAccessPanel()
            }
            Thread.sleep(5000)
            adminAccessPanel {
                tapAdminLoginButton()
            }

            adminLoginScreen {
                inputPassword(testPartner.adminPwd)
                tapLogin()
            }

            adminDetailsScreen {
                enterPartnerId(testPartner.partnerID)
                enterClinicId(testPartner.clinicID)
                waitForSetupComplete()
                testWorkManagerHelper.resetAllWorkers()
                verifyEditButtonsDisplayed()
                verifyErrorText(false)
                tapCloseButton()
            }
        }
        return testPartner
    }

    /**
     * Logs into Hub by entering correct Admin Pwd
     * Set up the Hub by entering correct partner and Clinic ID
     *
     * @param testPartner Test Partner used in the test
     */
    private fun enterPartnerAndClinicID(testPartner: TestPartners) {
        IntegrationUtil.swipeDown(onView(withId(R.id.container)))
        IntegrationUtil.waitForElementToAppearAndClick(
            onView(withId(R.id.admin_access_action)),
            "AdminAccessLoginButton",
            5
        )
        verifyToAdminLoginFragmentScreen()
        IntegrationUtil.typeTextWithCloseSoftKeyboard(onView(withId(R.id.password_input)), testPartner.adminPwd)
        IntegrationUtil.waitForElementToAppearAndClick(
            onView(
                allOf(
                    withId(R.id.btn_login),
                    isEnabled()
                )
            ),
            "AdminLogin Button"
        )
        verifyToAdminDetailsFragmentScreen()
        IntegrationUtil.simpleClick(onView(withId(R.id.btn_enter_partner_id)))
        enterKeyPad(testPartner.partnerID)
        IntegrationUtil.simpleClick(onView(withId(R.id.btn_enter_clinic_id)))
        enterKeyPad(testPartner.clinicID)
        IntegrationUtil.verifyElementsGoneOnPage(
            30,
            withId(R.id.partner_id_progress_bar),
            withId(R.id.clinic_id_progress_bar)
        )
    }

    /**
     * Tap to unlock and PIN IN to Checkout List UI
     *
     * @param testPartner
     */
    fun tapHomeScreenAndPinIn(testPartner: TestPartners, validateUserSyncText: Boolean = false) {
        IntegrationUtil.waitForElementToDisplayedAndClick(
            onView(withId(R.id.tap_to_unlock)),
            "Tap to Unlock"
        )

        IntegrationUtil.waitForElementToDisplayed(onView(withId(R.id.lock_keypad)))
        if (validateUserSyncText) {
            validateUserSyncErrorText()
        }

        clickButtonsOnKeyPad(testPartner.pin)
    }

    private fun validateUserSyncErrorText(bogusPin: String = defaultBogusPin) {
        val entryPoint: HomeScreenUtilEntryPoint by lazyEntryPoint()
        entryPoint.storageUtil().clearUserSync()

        clickButtonsOnKeyPad(bogusPin)
        IntegrationUtil.waitForElementToAppearFailIfNotFound(
            elementToCheck = withId(R.id.error_label),
            userFriendlyName = "pin in error label"
        )
        IntegrationUtil.waitForElementToAppearFailIfNotFound(
            elementToCheck = withId(R.id.sync_label),
            userFriendlyName = "sync users text"
        )
    }

    /**
     * Verify texts displayed on AdminLoginFragment screen.
     */
    fun verifyToAdminLoginFragmentScreen() {
        val toolbarTitleText =
            ApplicationProvider.getApplicationContext<Context>().resources.getString(
                R.string.admin_fragment_header_vaxcare_admin_login
            )
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
    }

    /**
     * Verify texts displayed on AdminDetailsFragment screen.
     */
    fun verifyToAdminDetailsFragmentScreen() {
        val toolbarTitleText =
            ApplicationProvider.getApplicationContext<Context>().resources.getString(
                R.string.admin_fragment_header_vaxcare_admin
            )
        IntegrationUtil.verifyDestinationScreenByToolbarTitle(toolbarTitleText)
    }

    /**
     * Verify that you land on the home screen
     * @param testPartner
     */
    fun verifyLandOnHomeScreen(testPartner: TestPartners) {
        IntegrationUtil.waitForElementToAppear(
            onView(withText(testPartner.partnerName)),
            "PartnerName"
        )
        IntegrationUtil.verifyTextDisplayed(withId(R.id.partner_name), testPartner.partnerName)
        IntegrationUtil.verifyTextDisplayed(withId(R.id.clinic_name), testPartner.clinicName)
    }
}
