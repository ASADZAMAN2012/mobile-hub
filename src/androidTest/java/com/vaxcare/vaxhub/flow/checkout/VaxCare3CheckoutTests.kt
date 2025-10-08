/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.checkout

import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.AppointmentListUtil
import com.vaxcare.vaxhub.common.CheckOutUtil
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.CREATE_PATIENT_APPOINTMENT
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.SELECT_APPOINTMENT
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.AddDose
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.EditDose
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.RemoveDose
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.PatientUtil
import com.vaxcare.vaxhub.common.RiskIconConstant
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.data.TestBackground
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
import com.vaxcare.vaxhub.data.TestStockPill
import com.vaxcare.vaxhub.flow.TestsBase
import com.vaxcare.vaxhub.mock.BaseMockDispatcher
import com.vaxcare.vaxhub.mock.model.CheckoutSession
import com.vaxcare.vaxhub.mock.model.MockRequest
import com.vaxcare.vaxhub.mock.util.usecase.checkout.CheckoutUseCases
import com.vaxcare.vaxhub.ui.PermissionsActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import javax.inject.Inject

@HiltAndroidTest
@LargeTest
@RunWith(AndroidJUnit4::class)
class VaxCare3CheckoutTests : TestsBase() {
    companion object {
        const val LOGIN_TESTS_DIRECTORY = "VaxCare3CheckoutTests/"
    }

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var storageUtil: StorageUtil

    @Inject
    lateinit var checkoutUseCases: CheckoutUseCases

    private val testWorkManagerHelper = TestWorkManagerHelper()
    private lateinit var scenario: ActivityScenario<PermissionsActivity>
    private val appointmentListUtil = AppointmentListUtil()
    private val checkOutUtil = CheckOutUtil()
    private val homeScreenUtil = HomeScreenUtil()
    private val patientUtil = PatientUtil()
    private val testSite = TestSites.RightArm
    private val testProductVaricella = TestProducts.Varicella

    @Before
    fun beforeTests() {
        hiltRule.inject()
        testWorkManagerHelper.startAllWorkers(workerFactory)
        scenario = ActivityScenario.launch(PermissionsActivity::class.java)
        storageUtil.clearLocalStorageAndDatabase()
    }

    @After
    fun afterTests() {
        storageUtil.clearLocalStorageAndDatabase()
        if (BuildConfig.BUILD_TYPE == "local") {
            mockServer.shutdown()
        }
    }

    @Test
    fun verifyStockPillOnAppointment_test() {
        val testPatient = TestPatients.QaRobotPatient()
        val testPartner = TestPartners.RprdCovidPartner

        registerMockServerDispatcher(
            VaxCare3CheckoutTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "verifyStockPillOnAppointment_test"
            )
        )
        homeScreenUtil.loginAsTestPartner(testPartner)
        // Create an appointment for ‘Mayah Miller’
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        IntegrationUtil.waitUIWithDelayed()
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.verifyPurpleStockPillWithTextPrivate(testPatient)
        IntegrationUtil.waitUIWithDelayed()
    }

    @Test
    fun verifyMedDTagOnMedDEligibleAppts_test() {
        val testPatient = TestPatients.MedDPatientForCopayRequired()
        val testPartner = TestPartners.RprdCovidPartner

        registerMockServerDispatcher(
            VaxCare3CheckoutTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "verifyMedDTagOnMedDEligibleAppts_test"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)

        // Create an appointment for ‘MedD Eligible’
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        IntegrationUtil.waitUIWithDelayed()
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        // Verify purple stock pill with text- ‘PRIVATE' gets displayed.
        appointmentListUtil.verifyPurpleStockPillWithTextPrivate(testPatient)
        // Verify MED D pill with text- ‘MED D' gets displayed.
        appointmentListUtil.verifyGrayMEDDTag(testPatient)
        IntegrationUtil.waitUIWithDelayed()
        // Tap the last item and verify ‘Med D Check Available’ dialog is displayed. Select ‘No Skip This’.
        appointmentListUtil.tapFirstElementInAppointmentListByPatientName(testPatient)
        // Verify you land on ‘Scan Doses’ screen.
        checkOutUtil.verifyToCheckoutPatientFragmentScreen()
        // Verify ‘MED D’ tag is displayed.
        checkOutUtil.verifyMedDTagDisplayedOnScanDosesScreen()
        IntegrationUtil.waitUIWithDelayed()
        // Close out of ‘Scan Doses’ screen
        checkOutUtil.tapCloseIconOnActionBarToCancelCheckout()
        appointmentListUtil.waitAppointmentLoadingFinish()
        // Refresh appt grid
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        // Verify purple stock pill with text- ‘PRIVATE' gets displayed.
        appointmentListUtil.verifyPurpleStockPillWithTextPrivate(testPatient)
        // Verify MED D pill with text- ‘MED D' gets displayed.
        appointmentListUtil.verifyGrayMEDDTag(testPatient)
        IntegrationUtil.waitUIWithDelayed()
    }

    @Ignore("Wrong Stock dialog causes this to fail")
    @Test
    fun verifyCollapsiblePatientInfoOnScanDosesScreen_test() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val testProduct = TestProducts.Varicella

        registerMockServerDispatcher(
            VaxCare3CheckoutTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "verifyCollapsiblePatientInfoOnScanDosesScreen_test"
            )
        )

        // 1. Create an appointment for ‘RiskFree Patient’ Test Patient- Tammy RiskFree
        homeScreenUtil.loginAsTestPartner(testPartner)
        // 1. Create an appointment for ‘MedD Eligible’ Test Patient-MedDPatientForCopayRequired
        // 2. Open Appt and verify you land on Scan Doses Screen.
        val appointmentId = checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = SELECT_APPOINTMENT
        )
        val appointmentData = patientUtil.getAppointmentById(appointmentId)
        assert(appointmentData != null) {
            "Expected to verify patient info in appointmentData object."
        }
        IntegrationUtil.waitUIWithDelayed()
        appointmentData?.toAppointment()?.let {
            // 3. Verify Patient Info and all highlighted fields are displayed. Verify (“Scan doses using the viewfinder.” is Displayed and proceed button is ‘not enabled and gray in color’.
            checkOutUtil.verifyPatientInfoOnScanDosesScreen(
                testPatient,
                it,
                hasDisplayViewfinder = true
            )
            checkOutUtil.verifyIsEnabledAndColorForProceedButton(isEnabled = false)
            // 4. Swipe Up Patient Info- Verify the following are
            //    a. Patient details - hidden
            //    b. Scan doses using the viewfinder.-displayed
            //    c. Proceed button disabled and color gray
            checkOutUtil.swipePatientInfoOnScanDosesScreen(isSwipeUp = true)
            checkOutUtil.verifyPatientInfoNotOnScanDosesScreen(
                testPatient,
                it,
                hasDisplayViewfinder = true
            )
            checkOutUtil.verifyIsEnabledAndColorForProceedButton(isEnabled = false)
            // 5. Swipe down- Verify the following are
            //    a. Patient Details - displayed
            //    b. Scan doses using the viewfinder.-displayed
            //    c. Proceed button disabled and color gray
            checkOutUtil.swipePatientInfoOnScanDosesScreen(isSwipeUp = false)
            checkOutUtil.verifyPatientInfoOnScanDosesScreen(
                testPatient,
                it,
                hasDisplayViewfinder = true
            )
            checkOutUtil.verifyIsEnabledAndColorForProceedButton(isEnabled = false)
            // 6. Add a dose - Varicella-J003535. Select - ‘Yes,Keep Dose’
            // Select a lot number that is not expired and within age indication (Varivax, J003535)
            checkOutUtil.performDoseActions(AddDose(testProduct))
            checkOutUtil.promptUnorderedDoseDialogToSelect("Yes, Keep Dose")
            // 7. Verify the following are:
            //    a. Patient details - hidden
            //    b. Scan doses using the viewfinder.-hidden
            //    c. Proceed button enabled and color is purple
            checkOutUtil.verifyPatientInfoNotOnScanDosesScreen(
                testPatient,
                it,
                hasDisplayViewfinder = false
            )
            checkOutUtil.verifyIsEnabledAndColorForProceedButton(isEnabled = true)
            // 8. Swipe down ‘Scan Doses’ text. Verify highlighted gets displayed again.
            //    a. Patient Details - displayed
            //    b. Scan doses using the viewfinder.-hidden
            //    c. Proceed button enabled and color is purple
            checkOutUtil.swipePatientInfoOnScanDosesScreen(isSwipeUp = false)
            checkOutUtil.verifyPatientInfoOnScanDosesScreen(
                testPatient,
                it,
                hasDisplayViewfinder = false
            )
            checkOutUtil.verifyIsEnabledAndColorForProceedButton(isEnabled = true)
            // 9. Swipe Up ‘Scan Doses’
            checkOutUtil.swipePatientInfoOnScanDosesScreen(isSwipeUp = true)
            // 10. Verify the following are
            //     a.Patient details - hidden
            //     b.Scan doses using the viewfinder.-hidden
            //     c.Proceed button enabled and color is purple
            checkOutUtil.verifyPatientInfoNotOnScanDosesScreen(
                testPatient,
                it,
                hasDisplayViewfinder = false
            )
            checkOutUtil.verifyIsEnabledAndColorForProceedButton(isEnabled = true)
        }
    }

    @Ignore("Issue tapping the continue button on the Checkout Summary screen")
    @Test
    fun removingDoseFromCheckout_test() {
        val testPatient = TestPatients.RiskFreePatientForEditCheckout()
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)
        val testProduct = TestProducts.Varicella

        // Create patient, appointment, add doses, and checkout
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = SELECT_APPOINTMENT
        )

        checkOutUtil.performDoseActions(AddDose(testProduct))
        checkOutUtil.promptUnorderedDoseDialogToSelect("Yes, Keep Dose")
        checkOutUtil.performDoseActions(EditDose(testProduct, testSite))
        checkOutUtil.tapArrowToCheckoutSummary()
        checkOutUtil.tapSetReasonAndSelectedReason()
        checkOutUtil.tapArrowToCheckoutSummary()
        IntegrationUtil.waitUIWithDelayed()

        // TEST FAILS HERE

        checkOutUtil.tapPatientCounselingCheckButton()
        checkOutUtil.verifyCheckoutCompleteScreen(testPatient)
        // Tap “Logout”
        checkOutUtil.tapLogoutButton()
        // Verify that you land on the home screen
        homeScreenUtil.verifyLandOnHomeScreen(testPartner)
        IntegrationUtil.waitUIWithDelayed()

        // Tap HomeScreen to pin in again, and remove a Dose from Checkout
        homeScreenUtil.tapHomeScreenAndPinIn(testPartner)
        IntegrationUtil.waitUIWithDelayed()
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        checkOutUtil.verifyAppointmentBackgroundForPrimaryPurple(testPatient)
        // Select a patient who has already received a vaccination
        checkOutUtil.scrollAppointmentListAndClickItem(testPatient.completePatientName)
        checkOutUtil.verifyToCheckoutFragmentScreenByTitle()
        checkOutUtil.performDoseActions(RemoveDose(testProduct))
        // Tap arrow to complete checkout
        checkOutUtil.tapArrowToCheckoutSummary()
        // Tap Patient Counseling check button
        checkOutUtil.tapCheckOutButton()
        // Select “Checkout Another Patient”
        checkOutUtil.tapCheckoutAnotherPatientButton()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // Verify that you land on the schedule grid for the current date of service
        appointmentListUtil.verifyScheduleGridForCurrentDate()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.RiskFreeIcon,
            testStockPillList = listOf(TestStockPill.PrivateStockPill),
            itemBackground = TestBackground.PrimaryWhite
        )
        IntegrationUtil.waitUIWithDelayed()
    }

    @Ignore("Issue tapping the No Insurance Card button after selecting insurance payer")
    @Test
    fun missingOrInvalidPayerSelectInsuranceFlow_test() {
        val testPatient = TestPatients.MissingPatientWithPayerInfo()
        val testPartner = TestPartners.RprdCovidPartner

        registerMockServerDispatcher(
            VaxCare3CheckoutTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "missingOrInvalidPayerSelectInsuranceFlow_test"
            )
        )

        // 1. Create an appointment for 'MissingPatientWithPayerInfo’ (Add this new patient)
        homeScreenUtil.loginAsTestPartner(testPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        // 2. Refresh the patient grid and verify ‘missing icon’ and stock pill Private are displayed on appt grid.
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.MissingInfoIcon,
            testStockPillList = listOf(TestStockPill.PrivateStockPill)
        )
        IntegrationUtil.waitUIWithDelayed()
        // 3. Open the appt and verify you are on Scan Doses Screen. Verify the messages and missing icon on the page.
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyCheckoutPatientInfoText(
            testPatient,
            riskIconConstant = RiskIconConstant.MissingInfoIcon,
            eligibilityText = "New Payer Info Required",
            responsibilityText = "VaxCare Bill",
            payerText = "Aetna",
            eligibilityCtaText = "INVALID PAYER INFO",
            isMedDTagShow = false
        )
        // 4. Add dose (J003535) and select all options shown on video.
        checkOutUtil.performDoseActions(AddDose(TestProducts.Varicella))
        checkOutUtil.confirmUnorderedDoseDialog()
        checkOutUtil.performDoseActions(EditDose(TestProducts.Varicella, testSite))
        checkOutUtil.tapArrowToCheckoutSummary()
        checkOutUtil.tapSetReasonAndSelectedReason()
        checkOutUtil.tapArrowToCheckoutSummary()
        // 5. When you are on ‘Edit Patient Info’ screen, verify the title - ‘Edit Patient Info’. Select Humana.
        checkOutUtil.verifyEditPatientInfoScreen()
        checkOutUtil.selectPayer("Humana")
        // TEST FAILS HERE
        // 6. Select ‘No Insurance Card’.
        patientUtil.selectNoInsuranceCardInEditPatientInfoScreen()
        // 7. Follow steps in the video and Checkout.
        IntegrationUtil.delayedClick(ViewMatchers.withId(R.id.button_ok))
        IntegrationUtil.waitUIWithDelayed()
        checkOutUtil.tapPatientCounselingCheckButton()
        IntegrationUtil.waitUIWithDelayed(5000)
        IntegrationUtil.delayedClick(ViewMatchers.withId(R.id.button_ok))
        IntegrationUtil.waitUIWithDelayed()
        homeScreenUtil.clickButtonsOnKeyPad(testPartner.pin)
        checkOutUtil.tapCheckOutButton()
        // 8. On complete screen click on 'checkout another patient' and verify you land on appt grid.
        checkOutUtil.tapCheckoutAnotherPatientButton()
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // 9. Verify patient apears checked out with purple background and missing icon.
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.MissingInfoIcon,
            testStockPillList = null,
            itemBackground = TestBackground.PrimaryPurple
        )
        // 10. Open the appt and verify you are on ‘Scan Doses’ screen.
        // 11. Verify ‘Humana’ is displayed now and you see ‘missing icon’. Also verify checked out vaccine is displayed.
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyCheckoutPatientInfoText(
            testPatient,
            RiskIconConstant.MissingInfoIcon,
            payerText = "Humana",
            isMedDTagShow = false
        )
        // verify checked out vaccine is displayed.
        checkOutUtil.verifyDoseAddList(listOf(testProductVaricella), testSite)
    }

    @Ignore("Issue tapping the No Insurance Card button after selecting insurance payer")
    @Test
    fun missingOrInvalidPayerNewInsuranceScreen_test() {
        val testPatient = TestPatients.MissingPatientWithAllPayerInfo()
        val testPartner = TestPartners.RprdCovidPartner

        registerMockServerDispatcher(
            VaxCare3CheckoutTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "missingOrInvalidPayerNewInsuranceScreen_test"
            )
        )

        // 1. Create an appointment for 'MissingPatientWithAllPayerInfo’ (Add this new patient)
        homeScreenUtil.loginAsTestPartner(testPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        // 2. Refresh the patient grid and verify ‘missing icon’ and stock pill Private are displayed on appt grid.
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.MissingInfoIcon,
            testStockPillList = listOf(TestStockPill.PrivateStockPill)
        )
        // 3. Open the appt and verify you are on Scan Doses Screen. Verify the messages and missing icon on the page and verify all highlighted info.
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        IntegrationUtil.waitUIWithDelayed()
        checkOutUtil.verifyCheckoutPatientInfoText(
            testPatient,
            riskIconConstant = RiskIconConstant.MissingInfoIcon,
            eligibilityText = "New Payer Info Required",
            responsibilityText = "VaxCare Bill",
            payerText = "Aetna",
            eligibilityCtaText = "INVALID PAYER INFO",
            isMedDTagShow = false
        )
        // 4. Add dose (J003535) and select all options shown on video.
        checkOutUtil.performDoseActions(AddDose(TestProducts.Varicella))
        checkOutUtil.confirmUnorderedDoseDialog()
        checkOutUtil.performDoseActions(EditDose(TestProducts.Varicella, testSite))
        checkOutUtil.tapArrowToCheckoutSummary()
        checkOutUtil.tapSetReasonAndSelectedReason()
        checkOutUtil.tapArrowToCheckoutSummary()
        // 5. When you are on ‘Edit Patient Information’ screen, verify the title - ‘Edit Patient Information’. Verify all the highlighted info.
        // 6. Click on ‘Yes’.
        testPatient.primaryMemberId?.let {
            checkOutUtil.verifyEditPatientInformationScreen(
                screenTitle = "Edit Patient Information",
                insuranceCollectTitle = "Does patient have new insurance?",
                currentInfoLabel = "Current Info",
                currentPayerName = "Aetna",
                currentPayerMemberId = "Member ID: ${testPatient.primaryMemberId.uppercase()}",
                currentPayerIssueLabel = "INVALID PAYER INFO",
                clickLabel = "Yes"
            )
        }
        // 7. Verify you are on ‘Edit Patient Info’ screen. verify the title - ‘Edit Patient Info’.
        checkOutUtil.verifyEditPatientInfoScreen()
        // 8. Select Insurance- ‘MedicareB (Flu and Pneumo Vaccines’.
        checkOutUtil.selectPayer("Medicare B (Flu, Pneumovax and Prevnar vaccines ONLY)")
        // 9. Select ‘No Insurance Card’.
        // TEST FAILS HERE
        patientUtil.selectNoInsuranceCardInEditPatientInfoScreen()
        // 10. Follow steps in the video and Checkout.
        IntegrationUtil.delayedClick(ViewMatchers.withId(R.id.button_ok))
        IntegrationUtil.waitUIWithDelayed()
        checkOutUtil.tapPatientCounselingCheckButton()
        IntegrationUtil.waitUIWithDelayed(5000)
        IntegrationUtil.delayedClick(ViewMatchers.withId(R.id.button_ok))
        IntegrationUtil.waitUIWithDelayed()
        homeScreenUtil.clickButtonsOnKeyPad(testPartner.pin)
        checkOutUtil.tapCheckOutButton()
        // 11. On complete screen click on 'checkout another patient' and verify you land on appt grid.
        checkOutUtil.tapCheckoutAnotherPatientButton()
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // 12.Verify patient appears checked out with purple background and missing icon.
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.MissingInfoIcon,
            testStockPillList = null,
            itemBackground = TestBackground.PrimaryPurple
        )
        // 13. Open the appt and verify you are on ‘Scan Doses’ screen.
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyCheckoutPatientInfoText(
            testPatient,
            RiskIconConstant.MissingInfoIcon,
            payerText = "Medicare B (Flu, Pneumovax and Prevnar vaccines ONLY)",
            isMedDTagShow = false
        )
        // verify checked out vaccine is displayed.
        checkOutUtil.verifyDoseAddList(listOf(testProductVaricella), testSite)
    }

    @Ignore("Test fails verifying stock tag on appointment list even when present")
    @Test
    fun checkInSelfPayPatientAndVerify_test() {
        val testPatient = TestPatients.SelfPayPatient()
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        // Test fails here
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.SelfPayIcon,
            testStockPillList = listOf(TestStockPill.ThreeOneSevenPill)
        )
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyCheckoutPatientInfoText(
            testPatient,
            riskIconConstant = RiskIconConstant.SelfPayIcon,
            eligibilityText = testPatient.eligibilityMessage,
            responsibilityText = "Self Pay",
            payerText = "Uninsured",
            eligibilityCtaText = "COVERAGE RESTRICTIONS APPLY OR PATIENT IS SET TO SELF-PAY",
            isMedDTagShow = false
        )
    }

    @Test
    fun checkInRiskFreePatientAndVerify_test() {
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val testPartner = TestPartners.RprdCovidPartner

        registerMockServerDispatcher(
            VaxCare3CheckoutTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "checkInRiskFreePatientAndVerify_test"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.RiskFreeIcon,
            testStockPillList = listOf(TestStockPill.PrivateStockPill)
        )
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyCheckoutPatientInfoText(
            testPatient,
            riskIconConstant = RiskIconConstant.RiskFreeIcon,
            eligibilityText = "Ready to Vaccinate",
            responsibilityText = "VaxCare Bill",
            payerText = "Aetna",
            isMedDTagShow = false
        )
    }

    @Ignore("Test fails verifying stock tag on appointment list even when present")
    @Test
    fun checkInPartnerBillPatientAndVerify_test() {
        val testPatient = TestPatients.PartnerBillPatient()
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        // Test fails here
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.PartnerBillIcon(),
            testStockPillList = listOf(TestStockPill.PrivateStockPill)
        )
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyCheckoutPatientInfoText(
            testPatient,
            riskIconConstant = RiskIconConstant.PartnerBillIcon(),
            eligibilityText = testPatient.eligibilityMessage,
            responsibilityText = "Partner Bill",
            payerText = "Humana",
            isMedDTagShow = false,
            eligibilityCtaText = "PAYER OR PLAN OUT OF NETWORK"
        )
    }

    @Ignore("Test fails verifying stock tag on appointment list even when present")
    @Test
    fun checkInVFCPatientAndVerify_test() {
        val testPatient = TestPatients.VFCPatient()
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)

        // Test should verify VFC stock, but patient creation endpoint
        // returns private stock for VFC patient
        // Test fails here
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.VFCPartnerBillIcon,
            testStockPillList = listOf(TestStockPill.VFCStockPill)
        )
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyCheckoutPatientInfoText(
            testPatient,
            riskIconConstant = RiskIconConstant.VFCPartnerBillIcon,
            eligibilityText = testPatient.eligibilityMessage,
            responsibilityText = "No Pay",
            payerText = "Humana",
            isMedDTagShow = false,
            eligibilityCtaText = "VFC PATIENT"
        )
    }

    @Test
    fun checkInMedDPatientAndVerify_test() {
        val testPatient = TestPatients.MedDPatientForCopayRequired()
        val testPartner = TestPartners.RprdCovidPartner

        registerMockServerDispatcher(
            VaxCare3CheckoutTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "checkInMedDPatientAndVerify_test"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.RiskFreeIcon,
            testStockPillList = listOf(TestStockPill.PrivateStockPill, TestStockPill.MedDTag)
        )
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyCheckoutPatientInfoText(
            testPatient,
            riskIconConstant = RiskIconConstant.RiskFreeIcon,
            eligibilityText = "Ready to Vaccinate",
            responsibilityText = "VaxCare Bill",
            payerText = "Medicare B (Flu, Pneumovax and Prevnar vaccines ONLY)",
            isMedDTagShow = true,
            isRunCopayCheckVisible = true,
            eligibilityCtaText = "LIMITED COVERAGE: FLU, PNEUMONIA, AND COVID ONLY",
            medDCatText = "MED D: RUN COPAY CHECK TO DETERMINE PATIENT RESPONSIBILITY"
        )
    }

    @Test
    fun checkInMissingDemoPatientAndVerify_test() {
        val testPatient = TestPatients.MissingPatientWithDemoInfo()
        val testPartner = TestPartners.RprdCovidPartner

        registerMockServerDispatcher(
            VaxCare3CheckoutTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "checkInMissingDemoPatientAndVerify_test"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.MissingInfoIcon,
            testStockPillList = listOf(TestStockPill.PrivateStockPill)
        )
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyCheckoutPatientInfoText(
            testPatient,
            riskIconConstant = RiskIconConstant.MissingInfoIcon,
            eligibilityText = testPatient.eligibilityMessage,
            responsibilityText = "VaxCare Bill",
            payerText = "Humana",
            isMedDTagShow = false,
            eligibilityCtaText = "INVALID OR MISSING GENDER"
        )
    }

    @Test
    fun checkInMissingPayerPatientAndVerify_test() {
        val testPatient = TestPatients.MissingPatientWithPayerInfo()
        val testPartner = TestPartners.RprdCovidPartner

        registerMockServerDispatcher(
            VaxCare3CheckoutTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "checkInMissingPayerPatientAndVerify_test"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.MissingInfoIcon,
            testStockPillList = listOf(TestStockPill.PrivateStockPill)
        )
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyCheckoutPatientInfoText(
            testPatient,
            riskIconConstant = RiskIconConstant.MissingInfoIcon,
            eligibilityText = testPatient.eligibilityMessage,
            responsibilityText = "VaxCare Bill",
            payerText = "Aetna",
            isMedDTagShow = false,
            eligibilityCtaText = "INVALID PAYER INFO"
        )
    }

    private class VaxCare3CheckoutTestsDispatcher(
        private val useCases: CheckoutUseCases,
        private val clinicId: Long,
        testDirectory: String,
    ) : BaseMockDispatcher() {
        companion object {
            const val APPOINTMENT_LIST_REGEX = "patients/appointment\\?clinicId.*"
            const val APPOINTMENT_DETAILS_REGEX = "patients/appointment/\\d+\\?version=2\\.0"
        }

        override val mockTestDirectory = "${LOGIN_TESTS_DIRECTORY}$testDirectory/"
        private var checkoutSession = CheckoutSession()

        init {
            this withRequestListener { request ->
                when {
                    isPostAppointment(request) -> {
                        checkoutSession = useCases.getPatientNameAndAppointmentTime(
                            checkoutSession,
                            request.requestBody
                        )
                        useCases.sendACE(
                            clinicId = clinicId,
                            appointmentTime = checkoutSession.appointmentDateTime
                                ?: LocalDateTime.now()
                        )
                        checkoutSession
                    }

                    else -> checkoutSession
                }
            }
            this withMutator { request, responseBody ->
                when {
                    isPostAppointment(request) -> {
                        checkoutSession.appointmentId = responseBody?.toInt()
                        responseBody
                    }

                    request.endpoint.contains(APPOINTMENT_LIST_REGEX.toRegex()) -> {
                        Log.d("MOCK", "responseBody: $responseBody")
                        Log.d("MOCK", "checkoutSession: $checkoutSession")
                        checkoutSession =
                            useCases.changeAppointmentList(checkoutSession, responseBody)
                        Log.d("MOCK", "checkoutList: ${checkoutSession.appointmentListPayload}")
                        checkoutSession.appointmentListPayload
                    }

                    request.endpoint.contains(APPOINTMENT_DETAILS_REGEX.toRegex()) &&
                        request.requestMethod.equals("GET", true) -> {
                        checkoutSession =
                            useCases.changeAppointment(checkoutSession, responseBody)
                        checkoutSession.appointmentDetailPayload
                    }

                    else -> responseBody
                }
            }
        }

        private fun isPostAppointment(request: MockRequest): Boolean =
            request.endpoint.contains("patients/appointment") &&
                request.requestMethod.equals("POST", true)
    }
}
