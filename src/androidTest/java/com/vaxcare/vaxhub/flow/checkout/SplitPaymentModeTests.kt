/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.checkout

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.AppointmentListUtil
import com.vaxcare.vaxhub.common.CheckOutUtil
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.AddDose
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.EditDose
import com.vaxcare.vaxhub.common.CollectPaymentUtil
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.data.PaymentModals
import com.vaxcare.vaxhub.data.TestCards
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
import com.vaxcare.vaxhub.flow.TestsBase
import com.vaxcare.vaxhub.ui.PermissionsActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class SplitPaymentModeTests : TestsBase() {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var storageUtil: StorageUtil

    private val testWorkManagerHelper = TestWorkManagerHelper()
    private lateinit var scenario: ActivityScenario<PermissionsActivity>
    private val homeScreenUtil = HomeScreenUtil()
    private val appointmentListUtil = AppointmentListUtil()
    private val checkOutUtil = CheckOutUtil()
    private val collectPaymentUtil = CollectPaymentUtil()

    companion object {
        const val MAX_CREATE_LOAD_TIME = 10
        const val MAX_SUBMIT_LOAD_TIME = 5
    }

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
    }

    @Ignore(
        "Test fails trying to tap 'Self Pay' button on Out of Age prompt, however, the prompt has no self pay button"
    )
    @Test
    fun splitPaymentForRiskFreeAndSelfPay_test() {
        // Requires login before every start（do this function）
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)

        // Each patient here can only be run once,
        // the second time the patient information must be reconfigured to avoid duplication of adding patient appointments that already exist on that day
        val testPatient = TestPatients.RiskFreePatientForCheckout()

        val testProduct = TestProducts.Varicella
        val testProductWithSelfPay = TestProducts.Shingrix
        val testSite = TestSites.LeftArm
        val testCardInfo = TestCards.ILabsCard

        val doseActions = arrayOf(
            AddDose(testProduct),
            EditDose(testProduct, testSite),
            AddDose(testProductWithSelfPay)
        )

        checkOutUtil.createAndCheckOutCurbsidePatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = doseActions,
            stopAt = CheckOutUtil.CheckoutFlowScreens.SELECT_DOSES
        )

        // Verify that the “Out of Age Indication” modal appears
        checkOutUtil.verifyPromptOutOfAgeIndicationDialog()

        // TEST FAILS HERE
        // Prompt shows out of age & expired dose, no self-pay option

        // Select “Self-Pay” button and Verify that this button also displays the $ amount for the cost of the dose (video ex = $168.37)
        checkOutUtil.selectAndVerifyDisplaySelfPayPrompt(testProductWithSelfPay)
        // Select site
        checkOutUtil.performDoseActions(EditDose(testProductWithSelfPay, testSite))
        // Tap arrow to complete checkout
        checkOutUtil.tapArrowToCheckoutSummary()
        // Verify that the summary screen displays the two doses with separate payment modes:
        // 1. Shingrix as Self-Pay: Out of Age Indication with the cost of the product (video ex = $168.37)
        // 2. Varivax with no dollar amount
        checkOutUtil.verifyCheckoutSummaryWithSeparatePaymentModes(
            testProduct,
            testProductWithSelfPay
        )
        // Tap the “Collect Payment Info” button
        checkOutUtil.tapCollectPaymentButton()
        // Tap the “Debit or Credit” button on the “Select Payment Method” modal
        checkOutUtil.promptSelectPaymentDialogToSelect(PaymentModals.PaymentDebitOrCredit())
        collectPaymentUtil.verifyToCollectPaymentInfoFragmentScreen()
        // Enter CC Info:
        collectPaymentUtil.enterCollectPaymentInfo(testCardInfo)
        // Tap “Save Payment Info”
        collectPaymentUtil.tapSavePaymentInfoButton()
        // Verify that the “Confirm and Pay” screen displays the two doses with separate payment modes:
        // 1. Shingrix as Self-Pay: Out of Age Indication with the cost of the product (video ex = $168.37)
        // 2. Varivax with no dollar amount
        checkOutUtil.verifyConfirmAndPayScreen(testProduct, testProductWithSelfPay, testCardInfo)
        // Tap the “Patient Counseling” check button to confirm the checkout
        checkOutUtil.tapPatientCounselingCheckButton()
        // Submitting checkout spinner should take <5 seconds
        checkOutUtil.verifySubmittingCheckoutScreenLoading(MAX_SUBMIT_LOAD_TIME)
        // Verify that the “Checkout Complete” screen displays:
        // 1. 2 Check Outs
        // 2. 2 Shots Administered
        checkOutUtil.verifyCheckoutCompleteScreenAfterSplitPayment(testPatient)
        // Select prompt to check out another patient from the Check Out Complete screen
        checkOutUtil.tapCheckoutAnotherPatientButton()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()

        val count = checkOutUtil.getCountInAppointmentListByPatientName(testPatient)
        if (count == 1) {
            // pre-check out there was only one:
            // Tap this one to refresh because the split out patient visit sometimes takes a few moments to load it’s updated payment mode
            appointmentListUtil.tapFirstElementInAppointmentListByPatientName(testPatient)
            checkOutUtil.promptProcessingAppointmentDialogToRefreshSplit()
            // verify to AppointmentListFragment Screen
            appointmentListUtil.verifyToAppointmentListFragmentScreen()
        }

        // Verify that there are now 2 patient visits for the patient:
        // Tap into each to verify the correct vaccination was assigned to the correct visit/payment mode
        // 1. The Risk Free visit will have the Varivax checkout
        appointmentListUtil.tapFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyRiskFreeCheckoutInVisit(testPatient, testProduct)
        checkOutUtil.promptCancelCheckDialogToBack()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()

        // 2. The Self-Pay visit will have the Shingrix checkout
        appointmentListUtil.tapLastElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifySelfPayCheckoutInVisit(testPatient, testProductWithSelfPay)
        checkOutUtil.promptCancelCheckDialogToBack()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
    }

    @Ignore(
        "Test fails trying to tap 'Partner Bill' button on Out of Age prompt, " +
            "however, the prompt has no partner bill button"
    )
    @Test
    fun splitPaymentForRiskFreeAndPartnerBill_test() {
        // Requires login before every start（do this function）
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)

        // Each patient here can only be run once,
        // the second time the patient information must be reconfigured to avoid duplication of adding patient appointments that already exist on that day
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val testProductsList = arrayListOf(TestProducts.Varicella, TestProducts.Shingrix)
        val patientVisitPositionList = arrayListOf<Int>()
        val testSite = TestSites.LeftArm

        val doseActions = arrayOf(
            AddDose(testProductsList[0]),
            EditDose(testProductsList[0], testSite),
            AddDose(testProductsList[1])
        )

        checkOutUtil.createAndCheckOutCurbsidePatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = doseActions,
            stopAt = CheckOutUtil.CheckoutFlowScreens.SELECT_DOSES
        )

        // Verify that the “Out of Age Indication” modal appears
        checkOutUtil.verifyPromptOutOfAgeIndicationDialog()

        // TEST FAILS HERE
        // Prompt shows out of age & expired dose, no partner bill option

        // Select “Partner Bill” button
        checkOutUtil.tapPartnerBillButton()
        // Select site
        checkOutUtil.performDoseActions(EditDose(testProductsList[1], testSite))
        // Tap arrow to complete checkout
        checkOutUtil.tapArrowToCheckoutSummary()
        IntegrationUtil.waitUIWithDelayed()
        // Verify that the summary screen displays the two doses with separate payment modes
        checkOutUtil.verifyTwoDosesWithSeparatePaymentModes(testProductsList)
        IntegrationUtil.waitUIWithDelayed()
        // Tap Patient Counseling check button
        checkOutUtil.tapCheckOutButton()
        // Verify that the “Checkout Complete” screen displays:a.2 Check Outs b.2 Shots Administered
        checkOutUtil.verifyTwoShotsAdministered()
        // Select prompt to check out another patient from the Check Out Complete screen
        checkOutUtil.tapCheckoutAnotherPatientButton()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        IntegrationUtil.waitUIWithDelayed()
        checkOutUtil.verifyPatientVisitCountAndCheckoutCount(testPatient, patientVisitPositionList)
        // Tap into each to verify the correct vaccination was assigned to the correct visit/payment mode
        // 1. The risk free visit will have U022984 checkout
        appointmentListUtil.tapFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyRiskFreeCheckoutInVisit(testPatient, testProductsList[0])
        checkOutUtil.promptCancelCheckDialogToBack()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // 2. The Partner Bill visit will have the J003535 checkout
        appointmentListUtil.tapLastElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyPartnerBillCheckoutInVisit(testPatient, testProductsList[1])
        checkOutUtil.promptCancelCheckDialogToBack()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
    }

    // I believe MedD was supposed to be removed from curbside. This test can likely be deleted.
    @Ignore("Test fails waiting for MedD prompt after adding dose ")
    @Test
    fun splitPaymentWithCopayRequirements_test() {
        // Requires login before every start（do this function）
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)
        // Each patient here can only be run once,
        // the second time the patient information must be reconfigured to avoid duplication of adding patient appointments that already exist on that day
        val testPatient = TestPatients.MedDPatientForCopayRequired()
        val testProduct = TestProducts.Boostrix
        val testProductWithSelfPay = TestProducts.Varicella
        val testSite = TestSites.LeftArm
        val testCardInfo = TestCards.ILabsCard

        checkOutUtil.createAndCheckOutCurbsidePatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CheckOutUtil.CheckoutFlowScreens.CREATE_PATIENT_APPOINTMENT
        )

        // Sees “Med D Check Available” and Select “No, Skip This”
        checkOutUtil.promptMedDCheckAvailableDialogToSelect(CopayRequiredTests.COPAY_REQUIRED_NO)
        // Copay check does not run, verify to CheckoutPatientFragment Screen
        checkOutUtil.verifyToCheckoutPatientFragmentScreen()
        // Select a product that requires copay (ex. Shingrix lot number 2nt5l or Boostrix 3JS9E)
        checkOutUtil.performDoseActions(AddDose(testProduct))

        // TEST FAILS HERE

        // Verify text “Requires a Copay” modal is displayed and Tap “Review Copay”
        checkOutUtil.promptReviewCopayDialogToSelect(CopayRequiredTests.COPAY_REVIEW)
        IntegrationUtil.waitUIWithDelayed()
        // Copay check runs, then displays copay requirements
        checkOutUtil.verifyToMedDCheckFragmentScreen()
        // Verify copays for Zoster and/or TDap are displayed, tap arrow
        checkOutUtil.verifyCopayDataDisplayedAndTapArrow()
        // Verify text “Copay Required” is displayed above the product
        checkOutUtil.verifyTextCopayRequiredDisplayed(testProduct)
        // Select site
        checkOutUtil.performDoseActions(EditDose(testProduct, testSite))
        // Select a lot number that is not within age indication (Shingrix, 2NT5L)
        checkOutUtil.performDoseActions(AddDose(testProductWithSelfPay))
        // Verify that the “Doses Not Covered” modal appears
        checkOutUtil.verifyPromptDosesNotCoveredDialog()
        // Select “Self-Pay” button and Verify that this button also displays the $ amount for the cost of the dose (video ex = $168.37)
        checkOutUtil.selectAndVerifyDisplaySelfPayPrompt(testProductWithSelfPay)
        // Select site
        checkOutUtil.performDoseActions(EditDose(testProductWithSelfPay, testSite))
        // Tap arrow to complete checkout
        checkOutUtil.tapArrowToCheckoutSummary()
        // Verify that the summary screen displays the two doses with separate payment modes:
        // 1. Shingrix as Self-Pay: Doses Not Covered with the cost of the product (video ex = $173.17)
        // 2. Varivax as Copay with the cost of the product
        checkOutUtil.verifyCheckoutSummaryWithSeparatePaymentModes(
            testProduct,
            testProductWithSelfPay,
            true
        )
        // Tap “Collect Payment”
        checkOutUtil.tapCollectPaymentButton()
        // Select “Debit or Credit” on “Copay Required” modal
        checkOutUtil.promptCopayRequiredDialogToSelect(PaymentModals.PaymentDebitOrCredit())
        collectPaymentUtil.verifyToCollectPaymentInfoFragmentScreen()
        // Enter CC Info:
        collectPaymentUtil.enterCollectPaymentInfo(testCardInfo)
        // Tap “Save Payment Info”
        collectPaymentUtil.tapSavePaymentInfoButton()
        // Verify the Credit Card summary info is displayed
        checkOutUtil.verifyCreditCardSummaryInfo(testCardInfo)
        // Tap “Patient Consent” arrow
        checkOutUtil.tapPatientConsentButton()
        checkOutUtil.verifyToSignatureAndConfirmFragmentScreen()
        // do Signature to Confirm
        checkOutUtil.doSignatureForConfirm()
        // Tap arrow to complete checkout
        checkOutUtil.tapSignatureSubmitButton()
        // Submitting checkout spinner should take <5 seconds
        checkOutUtil.verifySubmittingCheckoutScreenLoading(CopayRequiredTests.MAX_SUBMIT_LOAD_TIME)
        // Verify that the “Checkout Complete” screen displays:
        // 2 Shots Administered
        checkOutUtil.verifyCheckoutCompleteScreenAfterSplitPayment(testPatient)
        // Select prompt to check out another patient from the Check Out Complete screen
        checkOutUtil.tapCheckoutAnotherPatientButton()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        val count = checkOutUtil.getCountInAppointmentListByPatientName(testPatient)
        if (count == 1) {
            // pre-check out there was only one:
            // Tap this one to refresh because the split out patient visit sometimes takes a few moments to load it’s updated payment mode
            appointmentListUtil.tapFirstElementInAppointmentListByPatientName(testPatient)
            checkOutUtil.promptProcessingAppointmentDialogToRefreshSplit()
            // verify to AppointmentListFragment Screen
            appointmentListUtil.verifyToAppointmentListFragmentScreen()
        }
        // Verify that there are now 2 patient visits for the patient:
        // Tap into each to verify the correct vaccination was assigned to the correct visit/payment mode
        // 1. The Risk Free visit will have the Varivax checkout
        appointmentListUtil.tapFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifyRiskFreeCheckoutInVisit(testPatient, testProduct)
        checkOutUtil.promptCancelCheckDialogToBack()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // 2. The Self-Pay visit will have the Shingrix checkout
        appointmentListUtil.tapLastElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.verifySelfPayCheckoutInVisit(testPatient, testProductWithSelfPay)
        checkOutUtil.promptCancelCheckDialogToBack()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
    }
}
