/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.checkout

import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.AppointmentListUtil
import com.vaxcare.vaxhub.common.CalendarUtil
import com.vaxcare.vaxhub.common.CheckOutUtil
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.CREATE_PATIENT_APPOINTMENT
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.SELECT_APPOINTMENT
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.AddDose
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.RemoveDose
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.PatientUtil
import com.vaxcare.vaxhub.common.RiskIconConstant
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.common.robot.appointmentlist.appointmentListScreen
import com.vaxcare.vaxhub.common.robot.checkout.reviewdoses.reviewDosesScreen
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
import com.vaxcare.vaxhub.flow.checkout.VaxCare3CheckoutTests.Companion.LOGIN_TESTS_DIRECTORY
import com.vaxcare.vaxhub.flow.checkout.mock.dispatcher.CheckoutDispatcher
import com.vaxcare.vaxhub.mock.BaseMockDispatcher
import com.vaxcare.vaxhub.mock.model.CheckoutSession
import com.vaxcare.vaxhub.mock.model.MockRequest
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
import java.time.LocalDateTime
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineMockAPITests : TestsBase() {
    companion object {
        private const val RPRD_TESTS_DIRECTORY = "RPRDTests/"
        const val MAX_REFRESH_ATTEMPTS = 5
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
    private val homeScreenUtil = HomeScreenUtil()
    private val appointmentListUtil = AppointmentListUtil()
    private val checkOutUtil = CheckOutUtil()
    private val idlingResource: IdlingResource? = HubIdlingResource.instance
    private val testSite = TestSites.RightArm
    private val patientUtil = PatientUtil()
    private val calendarUtil = CalendarUtil()

    private val testProductVaricella = TestProducts.Varicella

    @Before
    fun beforeTests() {
        hiltRule.inject()
        testWorkManagerHelper.initializeWorkManager(workerFactory)
        scenario = ActivityScenario.launch(PermissionsActivity::class.java)
        storageUtil.clearLocalStorageAndDatabase()
        IdlingRegistry.getInstance().register(idlingResource)
    }

    @Test
    fun checkInMissingPatientInfoAndVerifyCheckout_test() {
        val testPatient = TestPatients.MissingPatientWithDemoInfo()
        val testPartner = TestPartners.RprdCovidPartner
        val testProduct1 = TestProducts.Varicella
        registerMockServerDispatcher(
            RPRDTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "missingPatientInfo_test"
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
        checkOutUtil.performDoseActions(AddDose(testProduct1))
        checkOutUtil.promptUnorderedDoseDialogToSelect("Yes, Keep Dose")
        checkOutUtil.tapArrowToCheckoutSummary()
        reviewDosesScreen {
            selectUnorderedReasonDoseNotAppearingForAll()
            tapArrowToCheckoutSummary()
        }
        IntegrationUtil.waitUIWithDelayed(5000)
        patientUtil.updateInfoForInvalidPatient(testPatient)
        Thread.sleep(5000)
        checkOutUtil.tapCheckOutButton()
        Thread.sleep(5000)
        IntegrationUtil.waitUIWithDelayed(10000)
        checkOutUtil.tapCheckoutAnotherPatientButton()
        Thread.sleep(2000)
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        appointmentListUtil.tapFirstElementInAppointmentListByPatientName(testPatient)
    }

    @Test
    fun checkoutSiuAdtOrm_test() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.SelfPayPatient2()
        val testProducts = listOf(testProductVaricella)

        registerMockServerDispatcher(
            RPRDTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "noCheckoutReasonForOrderedDose_test"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)
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
        checkOutUtil.performDoseActions(AddDose(TestProducts.RSV))
        // Tap the “Yes, Keep Dose” button
        checkOutUtil.verifyUnorderedDoseDialogToSelect("Yes, Keep Dose")
        checkOutUtil.waitForElementAndClick("Partner Bill")
        checkOutUtil.tapArrowToCheckoutSummary()
        reviewDosesScreen {
            selectUnorderedReasonDoseNotAppearingForAll()
            tapArrowToCheckoutSummary()
        }
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
        checkOutUtil.tapArrowToCheckoutSummary()
        IntegrationUtil.waitUIWithDelayed(5000)
        checkOutUtil.tapCheckOutButton()
        IntegrationUtil.waitUIWithDelayed(10000)
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

    @Test
    fun completePartnerBillCheckoutAndValidatePostCheckout_test() {
        val testPatient = TestPatients.PartnerBillPatient()
        val testPartner = TestPartners.RprdCovidPartner
        val testProduct = TestProducts.Adacel

        registerMockServerDispatcher(
            RPRDTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "partnerBill_test"
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
        checkOutUtil.clickFirstElementInAppointmentListByPatientName(testPatient)
        checkOutUtil.performDoseActions(AddDose(testProduct))
        checkOutUtil.promptUnorderedDoseDialogToSelect("Yes, Keep Dose")
//        IntegrationUtil.waitForElementToAppearAndClick(onView(withText("Keep Dose")))
        checkOutUtil.tapArrowToCheckoutSummary()
        reviewDosesScreen {
            selectUnorderedReasonDoseNotAppearingForAll()
            tapArrowToCheckoutSummary()
        }
        IntegrationUtil.waitUIWithDelayed(5000)
        Thread.sleep(2000)
        checkOutUtil.continueToCheckoutComplete(testPatient)
        IntegrationUtil.waitUIWithDelayed(2000)
        Thread.sleep(2000)
        checkOutUtil.tapCheckoutAnotherPatientButton()
        IntegrationUtil.waitUIWithDelayed(2000)
        appointmentListUtil.checkReloadForAppointmentListIfEligibilityNotBack(testPatient, 3)
        appointmentListUtil.tapFirstElementInAppointmentListByPatientName(testPatient)
    }

    @Ignore("Check crm to display payer info")
    @Test
    fun missingOrInvalidPayerSelectInsuranceFlow_test() {
        val testPatient = TestPatients.MissingPatientWithPayerInfo()
        val testPartner = TestPartners.RprdCovidPartner
        registerMockServerDispatcher(
            RPRDTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "missingPayerInfo_test"
            )
        )
        // 1. Create an appointment for 'MissingPatientWithPayerInfo’ (Add this new patient)
        homeScreenUtil.loginAsTestPartner(testPartner)

        val appointmentId = checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = SELECT_APPOINTMENT
        )
        // 4. Add dose (J003535) and select all options shown on video.
        Thread.sleep(5000)
        checkOutUtil.performDoseActions(AddDose(TestProducts.Adacel))
        checkOutUtil.promptUnorderedDoseDialogToSelect("Yes, Keep Dose")
//        checkOutUtil.waitForElementAndClick("Keep Dose")
//        checkOutUtil.confirmUnorderedDoseDialog()
//        checkOutUtil.performDoseActions(EditDose(TestProducts.Varicella, testSite))
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
        Thread.sleep(5000)
        IntegrationUtil.delayedClick(ViewMatchers.withId(R.id.button_ok))
        Thread.sleep(5000)
        homeScreenUtil.clickButtonsOnKeyPad(testPartner.pin)
        Thread.sleep(5000)
        checkOutUtil.tapCheckOutButton()
        Thread.sleep(5000)

        // 8. On complete screen click on 'checkout another patient' and verify you land on appt grid.
        checkOutUtil.tapCheckoutAnotherPatientButton()
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // 9. Verify patient appears checked out with purple background and missing icon.
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
//        checkOutUtil.verifyCheckoutPatientInfoText(
//            testPatient,
//            RiskIconConstant.MissingInfoIcon,
//            payerText = "Humana",
//            isMedDTagShow = false
//        )
        // verify checked out vaccine is displayed.
//        checkOutUtil.verifyDoseAddList(listOf(TestProducts.Varicella), testSite)
    }

    @After
    fun afterTests() {
        storageUtil.clearLocalStorageAndDatabase()
        IdlingRegistry.getInstance().unregister(idlingResource)
        if (BuildConfig.BUILD_TYPE == "local") {
            mockServer.shutdown()
        }
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

        override val mockTestDirectory = "$LOGIN_TESTS_DIRECTORY$testDirectory/"
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
