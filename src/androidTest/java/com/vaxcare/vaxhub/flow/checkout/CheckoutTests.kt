/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.checkout

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.AppointmentListUtil
import com.vaxcare.vaxhub.common.CalendarUtil
import com.vaxcare.vaxhub.common.CheckOutUtil
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.AddDose
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.EditDose
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.PatientUtil
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
import com.vaxcare.vaxhub.flow.TestsBase
import com.vaxcare.vaxhub.mock.BaseMockDispatcher
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
class CheckoutTests : TestsBase() {
    companion object {
        const val MAX_CREATE_LOAD_TIME = 10
        private const val CHECKOUT_TESTS_JSON_FILES_DIRECTORY = "CheckoutTests/"
    }

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var storageUtil: StorageUtil

    @Inject
    lateinit var checkoutUseCases: CheckoutUseCases

    private val testWorkManagerHelper = TestWorkManagerHelper()

    private lateinit var scenario: ActivityScenario<PermissionsActivity>
    private val appointmentListUtil = AppointmentListUtil()
    private val checkOutUtil = CheckOutUtil()
    private val calendarUtil = CalendarUtil()
    private val homeScreenUtil = HomeScreenUtil()
    private val patientUtil = PatientUtil()
    private val testProduct = TestProducts.Varicella
    private val testProductAdd = TestProducts.PPSV23
    private val testSite = TestSites.RightArm
    private val testProductAddForMTD15 = TestProducts.IPV
    private val idlingResource: IdlingResource? = HubIdlingResource.instance

    @Before
    fun beforeTests() {
        hiltRule.inject()
        testWorkManagerHelper.initializeWorkManager(workerFactory)
        scenario = ActivityScenario.launch(PermissionsActivity::class.java)
        storageUtil.clearLocalStorageAndDatabase()
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @After
    fun afterTests() {
        storageUtil.clearLocalStorageAndDatabase()
        IdlingRegistry.getInstance().unregister(idlingResource)
        @Suppress("KotlinConstantConditions")
        if (BuildConfig.BUILD_TYPE == "local") {
            mockServer.shutdown()
        }
    }

    @Test
    fun selectDifferentDateOnCalendar_test() {
        val testPartner: TestPartners = TestPartners.RprdCovidPartner
        registerMockServerDispatcher(
            CheckoutTestsDispatcher(
                testDirectory = "selectDifferentDateOnCalendar_test"
            )
        )
        // Requires login before every start（do this function）
        homeScreenUtil.loginAsTestPartner(testPartner)
        homeScreenUtil.tapHomeScreenAndPinIn(testPartner)
        repeat(3) {
            IntegrationUtil.waitForOperationComplete(1)
            calendarUtil.selectedRandomDateInCalendar(IntegrationUtil.getRandomDayForCalendar())
            calendarUtil.waitForLoadingGone()
        }
    }

    @Ignore("Test broke when we added route selection prompt")
    @Test
    fun addDoseToCheckout_test() {
        val testPatient = TestPatients.RiskFreePatientForEditCheckout()
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)

        val doseActions = arrayOf(
            AddDose(testProduct),
            EditDose(testProduct, testSite)
        )

        // Create appointment and complete checkout for Varicella
        checkOutUtil.createAndCheckOutCurbsidePatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = doseActions
        )

        // Tap “Logout”
        checkOutUtil.tapLogoutButton()
        // Verify that you land on the home screen
        homeScreenUtil.verifyLandOnHomeScreen(testPartner)

        // Tap HomeScreen to pin in again, and to add another Dose to Checkout
        homeScreenUtil.tapHomeScreenAndPinIn(testPartner)
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // Select a patient who has already received a vaccination
        checkOutUtil.scrollAppointmentListAndClickItem(testPatient.completePatientName)
        // verify to CheckoutPatientFragment Screen
        checkOutUtil.verifyToCheckoutFragmentScreenByTitle()
        // Select an additional lot number/product to add to the checkout
        checkOutUtil.performDoseActions(AddDose(testProductAdd))
        // TODO: handle route selection prompt here
        checkOutUtil.verifyToCheckoutFragmentScreenByTitle()
        // Select site
        checkOutUtil.performDoseActions(EditDose(testProductAdd, testSite))
        // Tap arrow to complete checkout
        checkOutUtil.tapArrowToCheckoutSummary()
        // verify the added vaccination along with the previous vaccination
        checkOutUtil.verifySpecialVaccination(testProductAdd)
        // Tap Patient Counseling check button
        checkOutUtil.tapCheckOutButton()
        // verify displays 2 shots administered
        checkOutUtil.verifyShotsAdministered()
        // Select “Checkout Another Patient”
        checkOutUtil.tapCheckoutAnotherPatientButton()
    }

    @Ignore("Test broke when we added route selection prompt")
    @Test
    fun editCheckoutFromCheckoutComplete_test() {
        val testPatient = TestPatients.RiskFreePatientForEditCheckout()
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)

        val productModifications = arrayOf(
            AddDose(testProduct),
            EditDose(testProduct, testSite)
        )

        // Create appointment and complete checkout for Varicella
        checkOutUtil.createAndCheckOutCurbsidePatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = productModifications
        )

        // Tap the pencil icon on the Checkout Complete screen
        checkOutUtil.tapCheckoutEdit()
        // Add a dose to the checkout (IPOL, K1696)
        checkOutUtil.verifyToCheckoutFragmentScreenByTitle()
        checkOutUtil.performDoseActions(AddDose(testProductAddForMTD15))
        // TODO: handle route selection prompt here
        checkOutUtil.verifyToCheckoutFragmentScreenByTitle()
        // Select site
        checkOutUtil.performDoseActions(EditDose(testProductAddForMTD15, testSite))
        // Tap arrow to complete checkout
        checkOutUtil.tapArrowToCheckoutSummary()
        // Verify that the Updated Summary screen now shows both doses
        val testProductList = arrayListOf(testProductAddForMTD15, testProduct)
        checkOutUtil.verifyUpdatedSummaryShowsBothDoses(testProductList)
        // Tap Patient Counseling check button
        checkOutUtil.tapCheckOutButton()
        // Verify that the Checkout Updated complete screen no shows “2 Shots Administered”
        checkOutUtil.verifyShotsAdministered()
    }

    @Ignore("Test broke when we added abandon appointment")
    @Test
    fun cancelCheckOut_test() {
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)

        // Create appointment and stop at scan dose screen
        checkOutUtil.createAndCheckOutCurbsidePatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = CheckOutUtil.CheckoutFlowScreens.SELECT_APPOINTMENT
        )
        checkOutUtil.tapCloseIconOnActionBarToCancelCheckout()

        homeScreenUtil.tapHomeScreenAndPinIn(testPartner)
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        checkOutUtil.scrollAppointmentListAndClickItem(testPatient.completePatientName)
        checkOutUtil.verifyToCheckoutFragmentScreenByTitle()
        // Select a lot number that is not expired and within age indication (Varivax, J003535)
        checkOutUtil.performDoseActions(AddDose(testProduct))
        // Tap the “X” button in the upper left-hand corner
        checkOutUtil.tapCloseIconOnActionBarToCancelCheckout()
        // Verify that the “Keep Check Out” modal appears
        checkOutUtil.verifyKeepCheckoutTextAppear()
        // Tap the “Keep Check Out” button
        checkOutUtil.tapKeepCheckoutButton()
        // Tap the “X” button in the upper left-hand corner again
        checkOutUtil.tapCloseIconOnActionBarToCancelCheckout()
        // Tap the “Cancel Check Out?” button
        checkOutUtil.tapCancelCheckoutButton()
        checkOutUtil.verifyKeepCheckoutModelClose()
        // Verify the user lands on the schedule grid
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        appointmentListUtil.verifyScheduleGridForCurrentDate()
        // Verify that the patient visit has not been checked out (not highlighted purple)
        checkOutUtil.verifyCheckedOutPatientVisitNotHighlightedPurple(testPatient)
    }

    @Ignore("The check out family members card does not appear on the checkout complete screen")
    @Test
    fun checkoutOtherFamilyFromCheckoutComplete_test() {
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val testPartner = homeScreenUtil.loginAsTestPartner(TestPartners.RprdCovidPartner)

        val doseActions = arrayOf(
            AddDose(testProduct),
            EditDose(testProduct, testSite)
        )

        // Create appointment and complete checkout for Varicella
        checkOutUtil.createAndCheckOutCurbsidePatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = doseActions
        )

        // TEST FAILS HERE

        // Verify the “Check Out Other Family” display;Existing family member name displayed;“+ Check Out Parent/Guardian“ text beneath
        checkOutUtil.verifyCheckoutOtherFamilyDisplay()
        // get family member name
        val familyMemberName = checkOutUtil.getFamilyMemberName()
        // Select the existing family member name that’s displayed
        checkOutUtil.selectFamilyMemberCheckout()
        // Confirm patient info
        checkOutUtil.confirmParentInfo()
        // Verify eligibility check takes < 10seconds
        patientUtil.verifyCreatePatientScreenLoading(MAX_CREATE_LOAD_TIME)
        // Select a lot number that is not expired and within age indication (Varivax, J003535)
        checkOutUtil.performDoseActions(AddDose(testProduct))
        // Select site
        checkOutUtil.performDoseActions(EditDose(testProduct, testSite))
        // Tap arrow to complete checkout
        checkOutUtil.tapArrowToCheckoutSummary()
        // Tap Patient Counseling check button
        checkOutUtil.tapCheckOutButton()
        // Tap “Check Out Another Patient”
        checkOutUtil.tapCheckoutAnotherPatientButton()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // Verify that a patient visit was created for the family member for the current time slot (closest 15 minute intervals)
        checkOutUtil.scrollToFamilyPatient(familyMemberName)
        // Verify that the patient visit is highlighted purple, indicating that they’ve been checked out
        checkOutUtil.verifyCheckedOutPatientVisitHighlightedPurple(familyMemberName)
    }

    @Test
    fun impersonateLARCOnlyPartner_test() {
        val testPartner: TestPartners = TestPartners.QALARCOnlyAutomationPartner
        registerMockServerDispatcher(
            CheckoutTestsDispatcher(
                testDirectory = "impersonateLARCOnlyPartner_test"
            )
        )
        // Requires login before every start（do this function）
        homeScreenUtil.loginAsTestPartner(testPartner)
        homeScreenUtil.tapHomeScreenAndPinIn(testPartner)
        // Verify the user lands on the schedule grid
        appointmentListUtil.verifyScheduleGridForCurrentDate()
    }

    private class CheckoutTestsDispatcher(
        testDirectory: String
    ) : BaseMockDispatcher() {
        override val mockTestDirectory: String =
            "${CHECKOUT_TESTS_JSON_FILES_DIRECTORY}$testDirectory/"
    }
}
