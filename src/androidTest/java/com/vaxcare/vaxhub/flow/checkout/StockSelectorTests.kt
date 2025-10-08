/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.checkout

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.CheckOutUtil
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.SELECT_APPOINTMENT
import com.vaxcare.vaxhub.common.CheckOutUtil.CheckoutFlowScreens.SELECT_DOSES
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.RiskIconConstant
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.common.matchers.withDrawable
import com.vaxcare.vaxhub.common.matchers.withDrawableWithTintColorByInstrumentation
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
import com.vaxcare.vaxhub.flow.TestsBase
import com.vaxcare.vaxhub.mock.BaseMockDispatcher
import com.vaxcare.vaxhub.mock.model.CheckoutSession
import com.vaxcare.vaxhub.mock.model.MockRequest
import com.vaxcare.vaxhub.mock.util.usecase.checkout.CheckoutUseCases
import com.vaxcare.vaxhub.ui.PermissionsActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import javax.inject.Inject

@HiltAndroidTest
@LargeTest
@RunWith(AndroidJUnit4::class)
class StockSelectorTests : TestsBase() {
    companion object {
        const val TEST_DIRECTORY = "StockSelectorTests/"
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
    private val checkOutUtil = CheckOutUtil()
    private val homeScreenUtil = HomeScreenUtil()
    private val context = ApplicationProvider.getApplicationContext<Context>()

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
    fun wrongStockPromptRemoveDose() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.QaRobotPatient()
        val testDose = TestProducts.Varicella
        val doseActions = arrayOf<DoseAction>(DoseAction.AddDose(testDose))
        val privateString = context.resources.getString(R.string.menu_stock_selector_private)

        registerMockServerDispatcher(
            StockSelectorTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "wrongStockPromptRemoveDose"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = doseActions,
            stopAt = SELECT_DOSES
        )
        checkOutUtil.promptUnorderedDoseDialogToSelect(
            context.resources.getString(R.string.orders_unordered_dose_prompt_yes)
        )
        checkOutUtil.verifyWrongStockDialogToSelect(
            context.resources.getString(R.string.dialog_wrong_stock_selected_remove_dose)
        )

        // Verify dose removed from checkout
        checkOutUtil.verifyNoDosesInCheckout()
        onView(withId(R.id.scanner_preview)).perform(ViewActions.swipeDown())

        // Verify stock hasn't changed
        onView(withId(R.id.eligibility_icon)).check(
            matches(
                allOf(
                    withDrawableWithTintColorByInstrumentation(RiskIconConstant.RiskFreeIcon),
                    isDisplayed()
                )
            )
        )
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(withId(R.id.textview_inventoryType), withText(privateString.uppercase()))
        )
    }

    @Test
    fun wrongStockPromptKeepDose() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.QaRobotPatient()
        val testDose = TestProducts.Varicella
        val doseActions = arrayOf<DoseAction>(DoseAction.AddDose(testDose))
        val privateString = context.resources.getString(R.string.menu_stock_selector_private)

        registerMockServerDispatcher(
            StockSelectorTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "wrongStockPromptKeepDose"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = doseActions,
            stopAt = SELECT_DOSES
        )
        checkOutUtil.promptUnorderedDoseDialogToSelect(
            context.resources.getString(R.string.orders_unordered_dose_prompt_yes)
        )
        checkOutUtil.verifyWrongStockDialogToSelect(
            context.resources.getString(R.string.dialog_wrong_stock_selected_keep_stock)
        )

        // Verify dose remains in checkout
        checkOutUtil.verifyDoseAddList(listOf(testDose), TestSites.NoSelect)

        onView(withId(R.id.scanner_preview)).perform(ViewActions.swipeDown())

        // Verify stock hasn't changed
        onView(withId(R.id.eligibility_icon)).check(
            matches(
                allOf(
                    withDrawableWithTintColorByInstrumentation(RiskIconConstant.RiskFreeIcon),
                    isDisplayed()
                )
            )
        )
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(withId(R.id.textview_inventoryType), withText(privateString.uppercase()))
        )
    }

    @Test
    fun wrongStockPromptSelectVfc() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.QaRobotPatient()
        val testDose = TestProducts.Varicella
        val doseActions = arrayOf<DoseAction>(DoseAction.AddDose(testDose))
        val vfcString = "VFC"

        registerMockServerDispatcher(
            StockSelectorTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "wrongStockPromptSelectVfc"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = doseActions,
            stopAt = SELECT_DOSES
        )
        checkOutUtil.promptUnorderedDoseDialogToSelect(
            context.resources.getString(R.string.orders_unordered_dose_prompt_yes)
        )
        checkOutUtil.verifyWrongStockDialogToSelect(
            context.resources.getString(R.string.menu_stock_selector_set_stock)
        )
        checkOutUtil.verifySetStockBottomDialogToSelect(
            context.resources.getString(R.string.menu_stock_selector_vfc_enrolled)
        )
        checkOutUtil.promptUnorderedDoseDialogToSelect(
            context.resources.getString(R.string.orders_unordered_dose_prompt_yes)
        )

        // Verify stock changed from private to VFC
        onView(withId(R.id.scanner_preview)).perform(ViewActions.swipeDown())
        onView(withId(R.id.eligibility_icon)).check(
            matches(
                allOf(
                    withDrawableWithTintColorByInstrumentation(RiskIconConstant.VFCPartnerBillIcon),
                    isDisplayed()
                )
            )
        )
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(withId(R.id.textview_inventoryType), withText(vfcString))
        )
    }

    @Test
    fun selectPrivateStockForVfcAppointment() {
        val testPartner = TestPartners.RprdCovidPartner
        val testPatient = TestPatients.VFCPatient()
        val privateString = context.resources.getString(R.string.menu_stock_selector_private)
        val vfcString = "VFC"

        registerMockServerDispatcher(
            StockSelectorTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "selectPrivateStockForVfcAppointment"
            )
        )

        homeScreenUtil.loginAsTestPartner(testPartner)
        checkOutUtil.createAndCheckOutVC3PatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            stopAt = SELECT_APPOINTMENT
        )

        onView(withId(R.id.eligibility_icon)).check(
            matches(
                allOf(
                    withDrawableWithTintColorByInstrumentation(RiskIconConstant.VFCPartnerBillIcon),
                    isDisplayed()
                )
            )
        )
        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(withId(R.id.textview_inventoryType), withText(vfcString))
        )

        onView(withId(R.id.right_icon1)).check(
            matches(withDrawable(R.drawable.ic_kebab_menu))
        ).perform(click())

        onView(withText(context.resources.getString(R.string.menu_stock_selector_set_stock)))
            .inRoot(isPlatformPopup()).perform(click())
        checkOutUtil.verifySetStockBottomDialogToSelect(privateString)

        // Verify stock changed from VFC to private
        onView(withId(R.id.scanner_preview)).perform(ViewActions.swipeDown())

        IntegrationUtil.verifyElementsPresentOnPage(
            IntegrationUtil.WAIT_TIME_DEFAULT,
            allOf(withId(R.id.textview_inventoryType), withText(privateString.uppercase()))
        )
    }

    private class StockSelectorTestsDispatcher(
        private val useCases: CheckoutUseCases,
        private val clinicId: Long,
        testDirectory: String,
    ) : BaseMockDispatcher() {
        companion object {
            const val APPOINTMENT_LIST_REGEX = "patients/appointment\\?clinicId.*"
            const val APPOINTMENT_DETAILS_REGEX = "patients/appointment/\\d+\\?version=2\\.0"
        }

        override val mockTestDirectory = "$TEST_DIRECTORY$testDirectory/"
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
                    isPatchPatient(request) -> {
                        useCases.sendACE(
                            clinicId = clinicId,
                            appointmentTime = checkoutSession.appointmentDateTime
                                ?: LocalDateTime.now()
                        )
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

        private fun isPatchPatient(request: MockRequest): Boolean =
            request.endpoint.contains("patients/patient/") &&
                request.requestMethod.equals("PATCH", true)

        private fun isPostAppointment(request: MockRequest): Boolean =
            request.endpoint.contains("patients/appointment") &&
                request.requestMethod.equals("POST", true)
    }
}
