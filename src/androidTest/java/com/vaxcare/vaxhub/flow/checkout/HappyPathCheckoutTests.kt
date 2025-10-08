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
import com.vaxcare.vaxhub.common.CheckOutUtil
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.AddDose
import com.vaxcare.vaxhub.common.CheckOutUtil.DoseAction.EditDose
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
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
@RunWith(AndroidJUnit4::class)
@LargeTest
class HappyPathCheckoutTests : TestsBase() {
    companion object {
        const val MAX_CREATE_LOAD_TIME = 10
        private const val HAPPY_PATH_CHECKOUT_TESTS_DIRECTORY = "HappyPathCheckoutTests/"
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
        if (BuildConfig.BUILD_TYPE == "local") {
            mockServer.shutdown()
        }
    }

    @Ignore("Needs update from curbside add patients to new add patients")
    @Test
    fun checkoutThenReturnHomeScreen_test() {
        val testPartner = TestPartners.RprdCovidPartner
        registerMockServerDispatcher(
            HappyPathCheckoutTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "checkoutThenReturnHomeScreen_test"
            )
        )
        // Requires login before every start（do this function）
        homeScreenUtil.loginAsTestPartner(testPartner)

        // Each patient here can only be run once,
        // the second time the patient information must be reconfigured to avoid duplication of adding patient appointments that already exist on that day
        val testPatient = TestPatients.RiskFreePatientForCheckout()

        val testProduct = TestProducts.Varicella
        val testSite = TestSites.LeftArm
        val doseActions = arrayOf(
            AddDose(testProduct),
            EditDose(testProduct, testSite)
        )

        // Create the patient appointment, add doses, and complete checkout
        checkOutUtil.createAndCheckOutCurbsidePatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = doseActions
        )

        // Tap “Logout”
        checkOutUtil.tapLogoutButton()
        // Verify that you land on the home screen
        homeScreenUtil.verifyLandOnHomeScreen(testPartner)
    }

    @Ignore("Needs update from curbside add patients to new add patients")
    @Test
    fun checkoutThenReturnScheduleGrid_test() {
        val testPartner = TestPartners.RprdCovidPartner

        registerMockServerDispatcher(
            HappyPathCheckoutTestsDispatcher(
                useCases = checkoutUseCases,
                clinicId = testPartner.clinicID.toLong(),
                testDirectory = "checkoutThenReturnScheduleGrid_test"
            )
        )

        // Requires login before every start（do this function）
        homeScreenUtil.loginAsTestPartner(testPartner)
        // Each patient here can only be run once,
        // the second time the patient information must be reconfigured to avoid duplication of adding patient appointments that already exist on that day
        val testPatient = TestPatients.RiskFreePatientForCheckout()

        val testProduct = TestProducts.Varicella
        val testSite = TestSites.LeftArm
        val productModifications = arrayOf(
            AddDose(testProduct),
            EditDose(testProduct, testSite)
        )

        checkOutUtil.createAndCheckOutCurbsidePatientWithStopAt(
            testPartner = testPartner,
            testPatient = testPatient,
            doseActions = productModifications
        )

        // Select prompt to check out another patient from the Check Out Complete screen
        checkOutUtil.tapCheckoutAnotherPatientButton()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // Verify that you land on the schedule grid for the current date of service
        appointmentListUtil.verifyScheduleGridForCurrentDate()
        // Verify the checked out patient visit is highlighted purple
        appointmentListUtil.verifyCheckedOutPatientVisitHighlightedPurple(testPatient)
    }

    private class HappyPathCheckoutTestsDispatcher(
        useCases: CheckoutUseCases,
        clinicId: Long,
        testDirectory: String
    ) : CheckoutDispatcher(useCases, clinicId) {
        init {
            this withRequestListener requestListener()
            this withMutator responseMutator()
        }

        override val mockTestDirectory: String =
            "$HAPPY_PATH_CHECKOUT_TESTS_DIRECTORY$testDirectory/"
    }
}
