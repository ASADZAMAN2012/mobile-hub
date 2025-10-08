/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.patient

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.AppointmentListUtil
import com.vaxcare.vaxhub.common.CheckOutUtil
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.IntegrationUtil
import com.vaxcare.vaxhub.common.PatientUtil
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.flow.TestsBase
import com.vaxcare.vaxhub.flow.checkout.HappyPathCheckoutTests
import com.vaxcare.vaxhub.mock.BaseMockDispatcher
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
@LargeTest
@RunWith(AndroidJUnit4::class)
class PatientTests : TestsBase() {
    companion object {
        const val TESTS_DIRECTORY = "PatientTests/"
    }

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var storageUtil: StorageUtil

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val testWorkManagerHelper = TestWorkManagerHelper()

    private lateinit var scenario: ActivityScenario<PermissionsActivity>
    private val homeScreenUtil = HomeScreenUtil()
    private val appointmentListUtil = AppointmentListUtil()
    private val checkOutUtil = CheckOutUtil()
    private val patientUtil = PatientUtil()

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
    fun searchForPatient_test() {
        val testPartner = TestPartners.RprdCovidPartner
        registerMockServerDispatcher(
            PatientTestsDispatcher(testDirectory = "searchForPatient_test")
        )

        // Should login before Tests
        homeScreenUtil.loginAsTestPartner(testPartner)
        homeScreenUtil.tapHomeScreenAndPinIn(testPartner)
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        // Tap the magnifying glass icon
        appointmentListUtil.tapMagnifyingGlassIconToSearch()
        // Enter a patient name
        patientUtil.verifyEnterPendingSearchPatientText()
    }

    @Ignore("This test is broken after we pushed out the AddPatients feature")
    @Test
    fun createNewPatient_test() {
        val testPartner = TestPartners.RprdCovidPartner
        registerMockServerDispatcher(
            PatientTestsDispatcher(testDirectory = "createNewPatient_test")
        )

        val testPatient = TestPatients.RiskFreePatientForCreatePatient()
        homeScreenUtil.loginAsTestPartner(testPartner)
        homeScreenUtil.tapHomeScreenAndPinIn(testPartner)
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        appointmentListUtil.tapPurplePlusIconToAdd()
        patientUtil.tapCreateNewPatientButton()
        patientUtil.tapUnderstandToDismissDialog()
        patientUtil.enterDemographicInformationForCreatePatient(testPatient)
        IntegrationUtil.waitUIWithDelayed()
        patientUtil.verifyCreatePatientScreenLoading(HappyPathCheckoutTests.MAX_CREATE_LOAD_TIME)
        patientUtil.closeCheckoutPage()
        IntegrationUtil.waitUIWithDelayed()
        // verify to AppointmentListFragment Screen
        appointmentListUtil.verifyToAppointmentListFragmentScreen()
        checkOutUtil.scrollAppointmentListToEnd()
        patientUtil.verifySpecialPatientOnScheduleGrid(testPatient)
    }

    private class PatientTestsDispatcher(
        testDirectory: String,
    ) : BaseMockDispatcher() {
        override val mockTestDirectory = "${TESTS_DIRECTORY}$testDirectory/"
    }
}
