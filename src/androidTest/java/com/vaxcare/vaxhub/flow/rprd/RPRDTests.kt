/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.rprd

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.AppointmentListUtil
import com.vaxcare.vaxhub.common.CheckOutUtil
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.CREATE_PATIENT_APPOINTMENT
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.SELECT_DOSES
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.AddDose
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.EditDose
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.PatientUtil
import com.vaxcare.vaxhub.common.RiskIconConstant
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.common.robot.appointmentlist.appointmentListScreen
import com.vaxcare.vaxhub.common.robot.createpatient.addAppointmentOrCreatePatientScreen
import com.vaxcare.vaxhub.common.robot.createpatient.curbsideConfirmPatientInfoScreen
import com.vaxcare.vaxhub.common.robot.home.homeScreen
import com.vaxcare.vaxhub.common.robot.home.splashScreen
import com.vaxcare.vaxhub.data.TestBackground
import com.vaxcare.vaxhub.data.TestOrderDose
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
import com.vaxcare.vaxhub.data.TestStockPill
import com.vaxcare.vaxhub.flow.TestsBase
import com.vaxcare.vaxhub.flow.checkout.mock.dispatcher.CheckoutDispatcher
import com.vaxcare.vaxhub.mock.util.usecase.checkout.CheckoutUseCases
import com.vaxcare.vaxhub.ui.PermissionsActivity
import com.vaxcare.vaxhub.ui.idlingresource.HubIdlingResource
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
@LargeTest
@RunWith(AndroidJUnit4::class)
class RPRDTests : TestsBase() {
    companion object {
        private const val RPRD_TESTS_DIRECTORY = "RPRDTests/"
    }

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    private val testWorkManagerHelper = TestWorkManagerHelper()

    @Inject
    lateinit var storageUtil: StorageUtil

    @Inject
    lateinit var appointmentListUtil: AppointmentListUtil

    @Inject
    lateinit var checkoutUseCases: CheckoutUseCases

    private lateinit var scenario: ActivityScenario<PermissionsActivity>
    private val checkOutUtil = CheckOutUtil()
    private val homeScreenUtil = HomeScreenUtil()
    private val patientUtil = PatientUtil()
    private val testSite = TestSites.RightArm
    private val testProductVaricella = TestProducts.Varicella
    private val testProductIPOL = TestProducts.IPOL
    private val idlingResource: IdlingResource? = HubIdlingResource.instance

    @Before
    fun beforeTests() {
        hiltRule.inject()
        testWorkManagerHelper.startAllWorkers(workerFactory)
        scenario = ActivityScenario.launch(PermissionsActivity::class.java)
        storageUtil.clearLocalStorageAndDatabase()
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @After
    fun afterTests() {
        storageUtil.clearLocalStorageAndDatabase()
        IdlingRegistry.getInstance().unregister(idlingResource)

        if (BuildConfig.BUILD_TYPE == "local") {
            mockServer.shutdown()
        }
    }

    @Test
    fun checkoutOrderedDose_test() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val productList = listOf(testProductVaricella)

        registerMockServerDispatcher(
            RPRDTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "checkoutOrderedDose_test"
            )
        )

        // Check-in a risk-free test patient
        homeScreenUtil.loginAsTestPartner(testPartner)

        // Enter the PIN to navigate to the MH’s schedule grid
        val appointmentIdString = checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )

        // Send an ORM for Varicella
        // we will send a ordered dose: TestOrderDose.Varicella
        patientUtil.fakeOrderedDoseForSpecificPatient(
            testPartners = testPartner,
            appointmentId = appointmentIdString.toInt(),
            testOrderDose = listOf(TestOrderDose.Varicella)
        )
        // refresh appointment list and wait for loading finish
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        // Verify that the test patient is on the schedule
        // Verify that, to the right of the test patient’s name, is a purple pill box that reads, “1 DOSE”
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.RiskFreeIcon,
            testStockPillList = listOf(TestStockPill.OrderedOneDose.changeText("1 DOSE"))
        )
        // Tap on the test patient’s name
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        // Verify that below the “Scan doses using the viewfinder.” text, is your order in a greyed-out text
        // swipe up patient info scan screen
        val orderedDoseList = checkOutUtil.getOrderedDoseList("select * from OrdersData")
        checkOutUtil.swipePatientInfoOnScanDosesScreen(isSwipeUp = true)
        checkOutUtil.verifyOrderedDoseDisplayed(orderedDoseList)
        // Tap the magnifying glass to search for a lot number
        // Search for and select lot number “J003535“
        checkOutUtil.performDoseActions(AddDose(TestProducts.Varicella))
        // Verify that the selected product appears on the checkout screen
        checkOutUtil.verifyDoseAddList(productList, TestSites.NoSelect)
        // Verify that the greyed-out order text from step 7 no longer appears on the screen
        checkOutUtil.verifyOrderedDoseDisappear(orderedDoseList)
        // Select Site
        checkOutUtil.performDoseActions(EditDose(TestProducts.Varicella, testSite))
        // Tap the purple arrow button
        // Verify that you land on the “Summary” screen with your checked-out ordered dose info displayed
        checkOutUtil.continueToCheckoutSummary(testPatient, productList)
        // Tap the salmon-colored checkmark icon
        // Verify that you land on Checkout Complete screen and that the checked out dose information is displayed
        checkOutUtil.continueToCheckoutComplete(testPatient)
        // Tap the “Check Out Another Patient” button
        checkOutUtil.tapCheckoutAnotherPatientButton()
        // Verify that the checked out patient visit is highlighted purple
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.RiskFreeIcon,
            testStockPillList = null,
            itemBackground = TestBackground.PrimaryPurple
        )
    }

    @Test
    fun checkOutUnorderedDoses_test() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.QaRobotPatient()

        registerMockServerDispatcher(
            RPRDTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "checkOutUnorderedDoses_test"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)
        // Create an appointment for ‘Mayah Miller’
        // Select a lot number that is not expired and within age indication (Varivax, J003535)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = arrayOf(AddDose(testProductVaricella)),
            stopAt = SELECT_DOSES
        )
        // Verify unordered dose dialog and select "Keep Dose"
        checkOutUtil.verifyUnorderedDoseDialogToSelect("Yes, Keep Dose")
        // Select an immunization Site
        checkOutUtil.performDoseActions(EditDose(testProductVaricella, testSite))
        // Tap arrow to complete checkout
        checkOutUtil.tapArrowToCheckoutSummary()
        // Verify that the user can not complete the checkout until a reason is selected
        checkOutUtil.verifyCannotCompleteCheckoutUntilSelectReason()
        // Verify that you land on the “Review Doses” screen with the following specs
        // Tap the “Set Reason” button
        checkOutUtil.verifyReviewDosesScreenAndClickSetReason(testProductVaricella)
        // Verify that the following options appear
        checkOutUtil.verifyReasonBottomDialog(
            reasonList = listOf(
                "Order Not Appearing",
                "Physician Unable to Order",
                "Product Mismatch with EHR",
                "Other"
            )
        )
        // Select “Order Not Appearing”
        checkOutUtil.clickReasonBottomDialogItem(R.string.orders_unordered_dose_reason_order_not_appearing)
        // Verify that the selected reason text has replaced the “Set Reason” button
        checkOutUtil.verifyReasonText("Order Not Appearing")
        // Tap the purple arrow button to complete the checkout
        // Verify summary screen
        checkOutUtil.continueToCheckoutSummary(testPatient, listOf(testProductVaricella))
        // Tap the salmon-colored, “Patient Counseling”, check mark button
        // Verify that you land on the “Checkout Complete” screen
        checkOutUtil.continueToCheckoutComplete(testPatient)
        // Tap the “Check Out Another Patient” button and verify that you land on the patient schedule grid
        checkOutUtil.tapCheckoutAnotherPatientButton()
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // Verify that the patient visit now appears as “checked out” (highlighted purple)
        checkOutUtil.verifyAppointmentBackgroundForPrimaryPurple(testPatient)
        IntegrationUtil.waitUIWithDelayed()
    }

    @Test
    fun removeUnorderedDoseFromCheckout_test() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.QaRobotPatient()

        registerMockServerDispatcher(
            RPRDTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "removeUnorderedDoseFromCheckout_test"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)
        // Create an appointment for ‘Mayah Miller’
        // Select a lot number that is not expired and within age indication (Varivax, J003535)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = arrayOf(AddDose(testProductVaricella)),
            stopAt = SELECT_DOSES
        )
        // Verify that the “Unordered Dose” modal appears and click.
        checkOutUtil.verifyUnorderedDoseDialogToSelect("No, Remove Dose")
        // Verify the check out screen should still read: “Scan doses using the viewfinder.“
        checkOutUtil.verifyNoDosesInCheckout()
        IntegrationUtil.waitUIWithDelayed()
    }

    @Ignore("Test broke when we added route selection prompt")
    @Test
    fun checkoutAnOrderedDoseAndAnUnorderedDose_test() {
        // Check-in a risk-free test patient
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)
        val appointmentIdString = checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        // Send the first ORM for the test patient: Varivax
        patientUtil.fakeOrderedDoseForSpecificPatient(
            testPartners = testPartner,
            appointmentId = appointmentIdString.toInt(),
            testOrderDose = listOf(TestOrderDose.Varicella)
        )
        // refresh appointment list and wait for loading finish
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // Verify that, to the right of the test patient’s name, is a purple pill box that reads, “1 DOSE”
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.RiskFreeIcon,
            testStockPillList = listOf(TestStockPill.OrderedOneDose.changeText("1 DOSE"))
        )
        // Tap on the test patient’s name
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        // Scroll below the “Scan doses using the viewfinder.” text and verify that your two orders are present in greyed-out text:
        //  a. ShortDescription (Varicella in this test’s case, but do not hardcode)
        //  b. ORDERED yesterday’s date AT timestamp (do not hardcode)
        // swipe up patient info scan screen
        val orderedDoseList = checkOutUtil.getOrderedDoseList("select * from OrdersData")
        checkOutUtil.swipePatientInfoOnScanDosesScreen(isSwipeUp = true)
        checkOutUtil.verifyOrderedDoseDisplayed(orderedDoseList)
        // Tap the magnifying glass to search for a lot number
        // Search for and select lot number “J003535“
        checkOutUtil.performDoseActions(AddDose(testProductVaricella))
        checkOutUtil.verifyToCheckoutPatientFragmentScreen(testProduct = testProductVaricella)
        checkOutUtil.performDoseActions(EditDose(testProductVaricella, testSite))
        // Verify that the selected product appears on the checkout screen
        checkOutUtil.verifyDoseAddList(
            listOf(testProductVaricella),
            testSite
        )
        // Tap the magnifying glass to search for a lot number
        // Search for and select lot number “V1B90“
        checkOutUtil.performDoseActions(AddDose(testProductIPOL))
        // Tap the “Yes, Keep Dose” button
        checkOutUtil.verifyUnorderedDoseDialogToSelect("Yes, Keep Dose")
        // Select an immunization Site
        checkOutUtil.performDoseActions(EditDose(testProductIPOL, testSite))
        // Verify that the selected product appears on the checkout screen
        // Verify that the “Unordered Dose” modal appears
        //    Header: Unordered Dose
        //    Text: “Checkout includes a dose that was not ordered in the EHR. Do you want to keep the dose?”
        //    Buttons: “Yes, Keep Dose” and “No, Remove Dose”
        checkOutUtil.verifyDoseAddList(
            listOf(testProductIPOL, testProductVaricella),
            testSite
        )

        // Tap the purple arrow to complete the checkout
        checkOutUtil.tapArrowToCheckoutSummary()
        // Verify that you land on the “Review Doses” screen with the following specs:
        //  a. Header: Review Doses
        //  b. Prompt: “Why were these doses dispensed?”
        //  c. Sub text: “Unordered doses have to be manually added to the EHR
        //  d. Vaccine info line:
        //     i.  Presentation icon: single-dose vial
        //     ii. Antigen: Varicella
        //     iii. Product name: (Varivax)
        //     iv. Set Reason button
        checkOutUtil.verifyReviewDosesScreenForOrderedDose(
            promptText = "Why were these doses dispensed?",
            subText = "Unordered doses have to be manually added to the EHR",
            setReasonButtonText = "Set Reason",
            testProduct = TestProducts.IPOL
        )
        // Verify that the user can not complete the checkout until a reason is selected
        //  a. The purple arrow button should be grayed out and if you tap on it nothing should happen
        checkOutUtil.verifyCannotCompleteCheckoutUntilSelectReason()
        // Tap the “Set Reason” button
        checkOutUtil.tapSetReason()
        // Verify that the following options appear:
        //  a. Order Not Appearing
        //  b. Physician Unable to Order
        //  c. Product Mismatch with EHR
        //  d. Other
        checkOutUtil.verifyReasonBottomDialog(
            reasonList = listOf(
                "Order Not Appearing",
                "Physician Unable to Order",
                "Product Mismatch with EHR",
                "Other"
            )
        )
        // Select “Product Mismatch with EHR”
        IntegrationUtil.clickBottomDialogItem(R.string.orders_unordered_dose_reason_product_mismatch)
        // Verify that the selected reason text has replaced the “Set Reason” button
        checkOutUtil.verifyReasonText("Product Mismatch with EHR")
        // Tap the purple arrow button to complete the checkout
        checkOutUtil.tapArrowToCheckoutSummary()
        // Verify that you land on the “Summary“ screen
        checkOutUtil.verifyAddedDoseAndCoPaySubTotalOnSummaryScreen(
            testPartners = testPartner,
            testPatient = testPatient,
            testProducts = listOf(TestProducts.IPOL, testProductVaricella),
            testSites = testSite
        )
        // Tap the salmon-colored, “Patient Counseling”, check mark button
        // Verify that you land on the “Checkout Complete” screen
        checkOutUtil.continueToCheckoutComplete(testPatient)
        // Tap the “Check Out Another Patient” button and verify that you land on the patient schedule grid
        checkOutUtil.tapCheckoutAnotherPatientButton()
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // Verify that the patient visit now appears as “checked out” (highlighted purple)
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.RiskFreeIcon,
            testStockPillList = listOf(),
            itemBackground = TestBackground.PrimaryPurple
        )
    }

    @Test
    fun addingAnOrderToVisit_test() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.RiskFreePatientForCheckout()

        registerMockServerDispatcher(
            RPRDTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "addingAnOrderToVisit_test"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)

        // 1. Check-in a risk-free test patient
        // we will send 1 ordered dose: Hib_PRP_T
        //  Enter the PIN to navigate to the MH’s schedule grid
        val appointmentIdString = checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        // 2 - 4. Send the first ORM for the test patient: ActHib
        patientUtil.fakeOrderedDoseForSpecificPatient(
            testPartners = testPartner,
            appointmentId = appointmentIdString.toInt(),
            testOrderDose = listOf(TestOrderDose.HibPrpT)
        )
        // refresh appointment list and wait for loading finish
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // 5. Verify that, to the right of the test patient’s name, is a purple pill box that reads, “1 DOSES”
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.RiskFreeIcon,
            testStockPillList = listOf(TestStockPill.OrderedOneDose.changeText("1 DOSE"))
        )
        // 6. Tap on the test patient’s name
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        // 7. Scroll below the “Scan doses using the viewfinder.” text and verify that your two orders are present in greyed-out text:
        //    a. ShortDescription (Hib (PRP-T) and Varicella in this test’s case, but do not hardcode)
        //    b. ORDERED yesterday’s date AT timestamp (do not hardcode)
        val orderedDoseList = checkOutUtil.getOrderedDoseList("select * from OrdersData")
        checkOutUtil.verifyToCheckoutPatientFragmentScreen()
        checkOutUtil.swipePatientInfoOnScanDosesScreen(isSwipeUp = true)
        checkOutUtil.verifyOrderedDoseDisplayed(orderedDoseList)
        // 8. Tap the “X” in the upper-left-hand corner to navigate back to the schedule grid
        checkOutUtil.tapCloseIconOnActionBarToCancelCheckout()
        appointmentListUtil.waitAppointmentLoadingFinish()
        // 9. Send your second ORM for Varivax
        patientUtil.fakeOrderedDoseForSpecificPatient(
            testPartners = testPartner,
            appointmentId = appointmentIdString.toInt(),
            testOrderDose = listOf(TestOrderDose.Varicella)
        )
        // 10. Tap on the Schedule Refresh icon in the upper-right-hand corner of the screen
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        // 11. Verify that to the right of the test patient’s name, is a purple pill box that reads, “2 DOSES”
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.RiskFreeIcon,
            testStockPillList = listOf(TestStockPill.OrderedOneDose.changeText("2 DOSES"))
        )
        // 12. Tap on the test patient’s name
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        // 13. Scroll below the “Scan doses using the viewfinder.” text and verify that both of the two orders are present in greyed-out text:
        // a. ShortDescription (Hib (PRP-T) & Varicella)  in this test’s case, but do not hardcode)
        // b. ORDERED yesterday’s date AT timestamp (do not hardcode)
        val orderedDoseListNew = checkOutUtil.getOrderedDoseList("select * from OrdersData")
        checkOutUtil.verifyToCheckoutPatientFragmentScreen()
        checkOutUtil.swipePatientInfoOnScanDosesScreen(isSwipeUp = true)
        checkOutUtil.verifyOrderedDoseDisplayed(orderedDoseListNew)
    }

    @Test
    fun noCheckoutReasonForOrderedDose_test() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val testProducts = listOf(testProductVaricella)

        registerMockServerDispatcher(
            RPRDTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "noCheckoutReasonForOrderedDose_test"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)

        // Check-in a risk-free test patient
        //  Enter the PIN to navigate to the MH’s schedule grid
        val appointmentIdString = checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CREATE_PATIENT_APPOINTMENT
        )
        // Send two ORMs for the test patient: ActHib and Varivax
        patientUtil.fakeOrderedDoseForSpecificPatient(
            testPartners = testPartner,
            appointmentId = appointmentIdString.toInt(),
            testOrderDose = listOf(TestOrderDose.Varicella, TestOrderDose.HibPrpT)
        )
        // refresh appointment list and wait for loading finish
        appointmentListUtil.refreshAppointmentList()
        appointmentListUtil.waitAppointmentLoadingFinish()
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // Verify that the test patient is on the schedule
        // Verify that, to the right of the test patient’s name, is a purple pill box that reads, “2 DOSES”
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.RiskFreeIcon,
            testStockPillList = listOf(TestStockPill.OrderedOneDose.changeText("2 DOSES"))
        )
        // Tap on the test patient’s name
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        // Scroll below the “Scan doses using the viewfinder.” text and verify that your two orders are present in greyed-out text:
        //    a. ShortDescription (Hib (PRP-T) and Varicella in this test’s case, but do not hardcode)
        //    b. ORDERED yesterday’s date AT timestamp (do not hardcode)
        val orderedDoseList = checkOutUtil.getOrderedDoseList("select * from OrdersData")
        checkOutUtil.swipePatientInfoOnScanDosesScreen(isSwipeUp = true)
        checkOutUtil.verifyOrderedDoseDisplayed(orderedDoseList)
        // Tap the magnifying glass to search for a lot number
        // Search for and select lot number “J003535“
        checkOutUtil.performDoseActions(AddDose(TestProducts.Varicella))
        // Verify that the selected product appears on the checkout screen
        // a. Presentation icon
        // b. Antigen
        // c. Product Name
        // d. Lot Number
        checkOutUtil.verifyDoseAddList(testProducts, TestSites.NoSelect)
        // Verify that the greyed-out order text from step 7 no longer appears on the screen
        checkOutUtil.verifyOrderedDoseDisappear(listOf(orderedDoseList[1]))
        // Select Site
        checkOutUtil.performDoseActions(EditDose(TestProducts.Varicella, testSite))
        // Tap the purple arrow button
        checkOutUtil.tapArrowToCheckoutSummary()
        // Verify that you land on the “Review Doses” screen with the following specs:
        //    Header: Review Doses
        //    Prompt: “Why were these doses not filled?”
        //    Vaccine info line: IMPORTANT :exclamation: Verify that the dose information here matches the dose information for the dose that we DID NOT checkout (in this example, it’s HIB) :exclamation:
        //        i. Presentation icon: single-dose vial
        //        ii. Antigen: Hib
        //        iii. Product name: (PRP-T)
        //        iv. Set Reason button
        checkOutUtil.verifyReviewDosesScreenForOrderedDose(
            promptText = "Why were these orders not filled?",
            setReasonButtonText = "Set Reason",
            testOrderDose = TestOrderDose.HibPrpT
        )
        // Verify that the user can not complete the checkout until a reason is selected
        //   a. The purple arrow button should be grayed out and if you tap on it nothing should happen
        checkOutUtil.verifyCannotCompleteCheckoutUntilSelectReason()
        // Tap the “Set Reason” button
        checkOutUtil.tapSetReason()
        // Verify that the following options appear:
        //    Option 1: “Postponed”
        //    Option 2: “Patient Refused“
        //    Option 3: “Out of Stock“
        //    Option 4: “Other”
        checkOutUtil.verifyReasonBottomDialog(
            reasonList = listOf(
                "Postponed",
                "Patient Refused",
                "Out of Stock",
                "Other"
            )
        )
        // Select Option 2 “Patient Refused”
        IntegrationUtil.clickBottomDialogItem(R.string.orders_ordered_dose_reason_patient_refused)
        // Verify that the selected reason “Patient Refused“ text has replaced the “Set Reason” button
        checkOutUtil.verifyReasonText("Patient Refused")
        // Tap the purple arrow button to complete the checkout
        // Verify that you land on the “Summary” screen with your checked-out ordered dose info displayed
        checkOutUtil.continueToCheckoutSummary(testPatient, listOf(testProductVaricella))
        // Tap the salmon-colored checkmark icon
        // Verify that you land on Checkout Complete screen and that the checked out dose information is displayed
        checkOutUtil.continueToCheckoutComplete(testPatient)
        // Tap the “Check Out Another Patient” button
        checkOutUtil.tapCheckoutAnotherPatientButton()
        // Verify that the checked-out patient visit is highlighted in purple
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        checkOutUtil.verifyAppointmentEligibilityIconAndTags(
            testPatient = testPatient,
            eligibilityIcon = RiskIconConstant.RiskFreeIcon,
            testStockPillList = null,
            itemBackground = TestBackground.PrimaryPurple
        )
        // Tap on the patient name that you just checked out
        appointmentListUtil.tapFirstElementInAppointmentListByPatientName(testPatient)
        // Verify that there the checked-out ordered dose appears fulfilled and the not checked-out ordered dose appears unfulfilled
        checkOutUtil.verifyDoseAddList(testProducts, testSite)
        checkOutUtil.verifyOrderedDoseAddList(listOf(TestOrderDose.HibPrpT))
    }

    @Ignore("This test is broken after we pushed out the AddPatients feature")
    @Test
    fun verifyCreateAppointmentAndNoRprdFlow() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.QaRobotPatient()

        registerMockServerDispatcher(
            RPRDTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "verifyCreateAppointmentAndNoRprdFlow"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)

        homeScreen {
            splashScreen {
                tapBackgroundToOpenAppointmentList()
                pinInUser(testPartner.pin)
            }

            appointmentListScreen {
                verifyTitleAndAppointmentList()
                tapAddAppointmentButton()
            }

            addAppointmentOrCreatePatientScreen {
                verifyTitleAndCreateButton()
                searchForPatientAndClick(testPatient)
            }

            curbsideConfirmPatientInfoScreen {
                verifyTitleAndInfo(testPatient)
            }
        }
    }

    private class RPRDTestsDispatcher(
        useCases: CheckoutUseCases,
        clinicId: Long,
        testDirectory: String
    ) : CheckoutDispatcher(useCases, clinicId) {
        init {
            this withRequestListener requestListener()
            this withMutator responseMutator()
        }

        override val mockTestDirectory: String =
            "$RPRD_TESTS_DIRECTORY$testDirectory/"
    }
}
