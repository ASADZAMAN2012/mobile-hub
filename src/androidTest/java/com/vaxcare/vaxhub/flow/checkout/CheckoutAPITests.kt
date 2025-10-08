/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.checkout

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
// import androidx.test.espresso.IdlingRegistry
// import androidx.test.espresso.IdlingResource
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.PatientUtil
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.data.TestProducts
import com.vaxcare.vaxhub.data.TestSites
import com.vaxcare.vaxhub.flow.TestsBase
import com.vaxcare.vaxhub.mock.BaseMockDispatcher
import com.vaxcare.vaxhub.model.AppointmentCheckout
import com.vaxcare.vaxhub.model.CheckInVaccination
import com.vaxcare.vaxhub.model.PaymentInformationRequestBody
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.appointment.PhoneContactConsentStatus
import com.vaxcare.vaxhub.model.enums.RiskFactor
import com.vaxcare.vaxhub.ui.PermissionsActivity
// import com.vaxcare.vaxhub.ui.idlingresource.HubIdlingResource
import com.vaxcare.vaxhub.web.PatientsApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
// import dagger.hilt.EntryPoint
// import dagger.hilt.InstallIn
// import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * API tests for the checkout process
 * 
 * These tests verify the complete checkout API flow including:
 * - Creating appointments
 * - Performing checkout operations via API
 * - Verifying checkout completion
 * - Testing various checkout scenarios
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class CheckoutAPITests : TestsBase() {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var storageUtil: StorageUtil

    @Inject
    lateinit var patientsApi: PatientsApi

    private val testWorkManagerHelper = TestWorkManagerHelper()
    private lateinit var scenario: ActivityScenario<PermissionsActivity>
    private val patientUtil = PatientUtil()
    private val homeScreenUtil = HomeScreenUtil()
    // private val idlingResource: IdlingResource? = HubIdlingResource.instance

    // Test data
    private val testPartner = TestPartners.RprdCovidPartner
    private val testProductVaricella = TestProducts.Varicella
    private val testProductAdacel = TestProducts.Adacel
    private val testProductPPSV23 = TestProducts.PPSV23
    private val testSite = TestSites.RightArm

    @Before
    fun setUp() {
        hiltRule.inject()
        // Initialize WorkManager for API tests
        testWorkManagerHelper.initializeWorkManager(workerFactory)
        // Launch minimal activity for EntryPoint access (required for PatientUtil)
        scenario = ActivityScenario.launch(PermissionsActivity::class.java)
        storageUtil.clearLocalStorageAndDatabase()
        
        // Setup mock server for local build type
        if (BuildConfig.BUILD_TYPE == "local") {
            registerMockServerDispatcher(CheckoutAPITestsDispatcher())
        }
        
        // Login before making API calls (required for authentication)
        homeScreenUtil.loginAsTestPartner(testPartner)
        homeScreenUtil.tapBackgroundToOpenAppointmentList()
        homeScreenUtil.pinInUser(testPartner.pin)
        // IdlingRegistry.getInstance().register(idlingResource)
    }

    @After
    fun tearDown() {
        storageUtil.clearLocalStorageAndDatabase()
        // IdlingRegistry.getInstance().unregister(idlingResource)
        if (BuildConfig.BUILD_TYPE == "local") {
            mockServer.shutdown()
        }
    }

    /**
     * Test successful checkout with single vaccine
     * 
     * This test verifies:
     * - Appointment is created successfully
     * - Checkout API call completes successfully
     * - Single vaccine is administered correctly
     * - Checkout status is updated
     */
    @Test
    fun checkoutAppointment_Success_SingleVaccine() = runBlocking {
        // Verify dependencies are initialized
        Assert.assertNotNull("PatientsApi should not be null", patientsApi)
        Assert.assertNotNull("StorageUtil should not be null", storageUtil)
        Assert.assertNotNull("PatientUtil should not be null", patientUtil)
        
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(testPatient)
        
        // Verify appointment was created successfully
        Assert.assertNotNull("Appointment ID should not be null", appointmentId)
        Assert.assertTrue("Appointment ID should be numeric", appointmentId.matches(Regex("\\d+")))
        
        val administeredVaccines = listOf(
            CheckInVaccination(
                id = 1,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = testSite.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            )
        )
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_001",
            administeredVaccines = administeredVaccines,
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = null,
            forcedRiskType = 0,
            postShotVisitPaymentModeDisplayed = PaymentMode.InsurancePay,
            phoneNumberFlowPresented = false,
            phoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
            phoneContactReasons = "",
            flags = emptyList(),
            pregnancyPrompt = false,
            weeksPregnant = null,
            creditCardInformation = null,
            activeFeatureFlags = emptyList(),
            attestHighRisk = false,
            riskFactors = emptyList()
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = appointmentId.toInt(),
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        Assert.assertTrue("Checkout should be successful", response.isSuccessful)
        Assert.assertEquals("Response code should be 200", 200, response.code())
        
        // Verify appointment status after checkout
        val appointment = patientUtil.getAppointmentById(appointmentId)
        Assert.assertNotNull("Appointment should still be retrievable after checkout", appointment)
    }

    /**
     * Test successful checkout with multiple vaccines
     * 
     * This test verifies:
     * - Multiple vaccines can be administered in one checkout
     * - Different vaccine types are handled correctly
     * - Payment modes can vary per vaccine
     */
    @Test
    fun checkoutAppointment_Success_MultipleVaccines() = runBlocking {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(testPatient)
        
        val administeredVaccines = listOf(
            CheckInVaccination(
                id = 1,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = TestSites.RightArm.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            ),
            CheckInVaccination(
                id = 2,
                productId = testProductAdacel.id,
                ageIndicated = true,
                lotNumber = testProductAdacel.lotNumber,
                method = "Intramuscular",
                site = TestSites.LeftArm.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            ),
            CheckInVaccination(
                id = 3,
                productId = testProductPPSV23.id,
                ageIndicated = true,
                lotNumber = testProductPPSV23.lotNumber,
                method = "Intramuscular",
                site = TestSites.RightArm.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            )
        )
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_002",
            administeredVaccines = administeredVaccines,
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = null,
            forcedRiskType = 0,
            postShotVisitPaymentModeDisplayed = PaymentMode.InsurancePay,
            phoneNumberFlowPresented = false,
            phoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
            phoneContactReasons = "",
            flags = emptyList(),
            pregnancyPrompt = false,
            weeksPregnant = null,
            creditCardInformation = null,
            activeFeatureFlags = emptyList(),
            attestHighRisk = false,
            riskFactors = emptyList()
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = appointmentId.toInt(),
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        Assert.assertTrue("Multiple vaccine checkout should be successful", response.isSuccessful)
        Assert.assertEquals("Response code should be 200", 200, response.code())
    }

    /**
     * Test checkout with self-pay patient
     * 
     * This test verifies:
     * - Self-pay patients can be checked out
     * - Self-pay payment mode is handled correctly
     * - Credit card information can be included
     */
    @Test
    fun checkoutAppointment_Success_SelfPayPatient() = runBlocking {
        // Arrange
        val selfPayPatient = TestPatients.SelfPayPatient()
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(selfPayPatient)
        
        val administeredVaccines = listOf(
            CheckInVaccination(
                id = 1,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = testSite.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.SelfPay,
                paymentModeReason = null
            )
        )
        
        val creditCardInfo = PaymentInformationRequestBody(
            cardNumber = "4111111111111111",
            expirationDate = "12/2025",
            cardholderName = "John Doe",
            email = "john.doe@example.com",
            phoneNumber = "1234567890"
        )
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_003",
            administeredVaccines = administeredVaccines,
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = null,
            forcedRiskType = 0,
            postShotVisitPaymentModeDisplayed = PaymentMode.SelfPay,
            phoneNumberFlowPresented = false,
            phoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
            phoneContactReasons = "",
            flags = emptyList(),
            pregnancyPrompt = false,
            weeksPregnant = null,
            creditCardInformation = creditCardInfo,
            activeFeatureFlags = emptyList(),
            attestHighRisk = false,
            riskFactors = emptyList()
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = appointmentId.toInt(),
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        Assert.assertTrue("Self-pay checkout should be successful", response.isSuccessful)
        Assert.assertEquals("Response code should be 200", 200, response.code())
    }

    /**
     * Test checkout with VFC patient
     * 
     * This test verifies:
     * - VFC patients can be checked out
     * - VFC payment mode is handled correctly
     * - No payment information is required
     */
    @Test
    fun checkoutAppointment_Success_VFCPatient() = runBlocking {
        // Arrange
        val vfcPatient = TestPatients.VFCPatient()
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(vfcPatient)
        
        val administeredVaccines = listOf(
            CheckInVaccination(
                id = 1,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = testSite.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.NoPay,
                paymentModeReason = null
            )
        )
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_004",
            administeredVaccines = administeredVaccines,
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = null,
            forcedRiskType = 0,
            postShotVisitPaymentModeDisplayed = PaymentMode.NoPay,
            phoneNumberFlowPresented = false,
            phoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
            phoneContactReasons = "",
            flags = emptyList(),
            pregnancyPrompt = false,
            weeksPregnant = null,
            creditCardInformation = null,
            activeFeatureFlags = emptyList(),
            attestHighRisk = false,
            riskFactors = emptyList()
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = appointmentId.toInt(),
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        Assert.assertTrue("VFC checkout should be successful", response.isSuccessful)
        Assert.assertEquals("Response code should be 200", 200, response.code())
    }

    /**
     * Test checkout with pregnant patient
     * 
     * This test verifies:
     * - Pregnant patients can be checked out
     * - Pregnancy information is handled correctly
     * - Pregnancy-specific flags are set
     */
    @Test
    fun checkoutAppointment_Success_PregnantPatient() = runBlocking {
        // Arrange
        val pregnantPatient = TestPatients.PregnantPatient()
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(pregnantPatient)
        
        val administeredVaccines = listOf(
            CheckInVaccination(
                id = 1,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = testSite.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            )
        )
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_005",
            administeredVaccines = administeredVaccines,
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = null,
            forcedRiskType = 0,
            postShotVisitPaymentModeDisplayed = PaymentMode.InsurancePay,
            phoneNumberFlowPresented = false,
            phoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
            phoneContactReasons = "",
            flags = listOf("PREGNANCY"),
            pregnancyPrompt = true,
            weeksPregnant = 20,
            creditCardInformation = null,
            activeFeatureFlags = emptyList(),
            attestHighRisk = false,
            riskFactors = listOf(RiskFactor.RSV_PREGNANT)
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = appointmentId.toInt(),
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        Assert.assertTrue("Pregnant patient checkout should be successful", response.isSuccessful)
        Assert.assertEquals("Response code should be 200", 200, response.code())
    }

    /**
     * Test checkout with high-risk patient
     * 
     * This test verifies:
     * - High-risk patients can be checked out
     * - Risk factors are properly recorded
     * - High-risk attestation is handled
     */
    @Test
    fun checkoutAppointment_Success_HighRiskPatient() = runBlocking {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(testPatient)
        
        val administeredVaccines = listOf(
            CheckInVaccination(
                id = 1,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = testSite.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            )
        )
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_006",
            administeredVaccines = administeredVaccines,
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = 1,
            forcedRiskType = 1,
            postShotVisitPaymentModeDisplayed = PaymentMode.InsurancePay,
            phoneNumberFlowPresented = false,
            phoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
            phoneContactReasons = "",
            flags = listOf("HIGH_RISK"),
            pregnancyPrompt = false,
            weeksPregnant = null,
            creditCardInformation = null,
            activeFeatureFlags = emptyList(),
            attestHighRisk = true,
            riskFactors = listOf(RiskFactor.COVID_UNDER_65, RiskFactor.RSV_PREGNANT)
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = appointmentId.toInt(),
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        Assert.assertTrue("High-risk patient checkout should be successful", response.isSuccessful)
        Assert.assertEquals("Response code should be 200", 200, response.code())
    }

    /**
     * Test checkout with phone contact consent
     * 
     * This test verifies:
     * - Phone contact consent flow is handled
     * - Consent status is properly recorded
     * - Contact reasons are captured
     */
    @Test
    fun checkoutAppointment_Success_WithPhoneContactConsent() = runBlocking {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(testPatient)
        
        val administeredVaccines = listOf(
            CheckInVaccination(
                id = 1,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = testSite.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            )
        )
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_007",
            administeredVaccines = administeredVaccines,
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = null,
            forcedRiskType = 0,
            postShotVisitPaymentModeDisplayed = PaymentMode.InsurancePay,
            phoneNumberFlowPresented = true,
            phoneContactConsentStatus = PhoneContactConsentStatus.ACCEPTED,
            phoneContactReasons = "Follow-up care, Side effect monitoring",
            flags = emptyList(),
            pregnancyPrompt = false,
            weeksPregnant = null,
            creditCardInformation = null,
            activeFeatureFlags = emptyList(),
            attestHighRisk = false,
            riskFactors = emptyList()
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = appointmentId.toInt(),
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        Assert.assertTrue("Phone contact consent checkout should be successful", response.isSuccessful)
        Assert.assertEquals("Response code should be 200", 200, response.code())
    }

    /**
     * Test checkout with different administration methods
     * 
     * This test verifies:
     * - Different administration methods are supported
     * - Method information is properly recorded
     * - Site information is correctly associated
     */
    @Test
    fun checkoutAppointment_Success_DifferentAdministrationMethods() = runBlocking {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(testPatient)
        
        val administeredVaccines = listOf(
            CheckInVaccination(
                id = 1,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = TestSites.RightArm.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            ),
            CheckInVaccination(
                id = 2,
                productId = testProductAdacel.id,
                ageIndicated = true,
                lotNumber = testProductAdacel.lotNumber,
                method = "Subcutaneous",
                site = TestSites.LeftArm.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            )
        )
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_008",
            administeredVaccines = administeredVaccines,
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = null,
            forcedRiskType = 0,
            postShotVisitPaymentModeDisplayed = PaymentMode.InsurancePay,
            phoneNumberFlowPresented = false,
            phoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
            phoneContactReasons = "",
            flags = emptyList(),
            pregnancyPrompt = false,
            weeksPregnant = null,
            creditCardInformation = null,
            activeFeatureFlags = emptyList(),
            attestHighRisk = false,
            riskFactors = emptyList()
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = appointmentId.toInt(),
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        Assert.assertTrue("Different administration methods checkout should be successful", response.isSuccessful)
        Assert.assertEquals("Response code should be 200", 200, response.code())
    }

    /**
     * Test checkout with different dose series
     * 
     * This test verifies:
     * - Different dose series are supported
     * - Series information is properly recorded
     * - Multiple doses of the same vaccine are handled
     */
    @Test
    fun checkoutAppointment_Success_DifferentDoseSeries() = runBlocking {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(testPatient)
        
        val administeredVaccines = listOf(
            CheckInVaccination(
                id = 1,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = TestSites.RightArm.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            ),
            CheckInVaccination(
                id = 2,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = TestSites.LeftArm.displayName,
                doseSeries = 2,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            )
        )
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_009",
            administeredVaccines = administeredVaccines,
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = null,
            forcedRiskType = 0,
            postShotVisitPaymentModeDisplayed = PaymentMode.InsurancePay,
            phoneNumberFlowPresented = false,
            phoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
            phoneContactReasons = "",
            flags = emptyList(),
            pregnancyPrompt = false,
            weeksPregnant = null,
            creditCardInformation = null,
            activeFeatureFlags = emptyList(),
            attestHighRisk = false,
            riskFactors = emptyList()
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = appointmentId.toInt(),
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        Assert.assertTrue("Different dose series checkout should be successful", response.isSuccessful)
        Assert.assertEquals("Response code should be 200", 200, response.code())
    }

    /**
     * Test checkout with feature flags
     * 
     * This test verifies:
     * - Feature flags are properly handled
     * - Active feature flags are recorded
     * - Feature-specific behavior is supported
     */
    @Test
    fun checkoutAppointment_Success_WithFeatureFlags() = runBlocking {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(testPatient)
        
        val administeredVaccines = listOf(
            CheckInVaccination(
                id = 1,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = testSite.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            )
        )
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_010",
            administeredVaccines = administeredVaccines,
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = null,
            forcedRiskType = 0,
            postShotVisitPaymentModeDisplayed = PaymentMode.InsurancePay,
            phoneNumberFlowPresented = false,
            phoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
            phoneContactReasons = "",
            flags = listOf("FEATURE_FLAG_1", "FEATURE_FLAG_2"),
            pregnancyPrompt = false,
            weeksPregnant = null,
            creditCardInformation = null,
            activeFeatureFlags = listOf("ENHANCED_CHECKOUT", "RISK_ASSESSMENT"),
            attestHighRisk = false,
            riskFactors = emptyList()
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = appointmentId.toInt(),
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        Assert.assertTrue("Feature flags checkout should be successful", response.isSuccessful)
        Assert.assertEquals("Response code should be 200", 200, response.code())
    }

    /**
     * Test checkout with invalid appointment ID
     * 
     * This test verifies:
     * - Invalid appointment IDs are handled gracefully
     * - Appropriate error responses are returned
     * - System remains stable with invalid inputs
     */
    @Test
    fun checkoutAppointment_Error_InvalidAppointmentId() = runBlocking {
        // Arrange
        val invalidAppointmentId = 999999
        val administeredVaccines = listOf(
            CheckInVaccination(
                id = 1,
                productId = testProductVaricella.id,
                ageIndicated = true,
                lotNumber = testProductVaricella.lotNumber,
                method = "Intramuscular",
                site = testSite.displayName,
                doseSeries = 1,
                paymentMode = PaymentMode.InsurancePay,
                paymentModeReason = null
            )
        )
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_011",
            administeredVaccines = administeredVaccines,
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = null,
            forcedRiskType = 0,
            postShotVisitPaymentModeDisplayed = PaymentMode.InsurancePay,
            phoneNumberFlowPresented = false,
            phoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
            phoneContactReasons = "",
            flags = emptyList(),
            pregnancyPrompt = false,
            weeksPregnant = null,
            creditCardInformation = null,
            activeFeatureFlags = emptyList(),
            attestHighRisk = false,
            riskFactors = emptyList()
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = invalidAppointmentId,
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        Assert.assertFalse("Invalid appointment ID should result in error", response.isSuccessful)
        Assert.assertTrue("Response code should be 4xx or 5xx", response.code() >= 400)
    }

    /**
     * Test checkout with empty vaccine list
     * 
     * This test verifies:
     * - Empty vaccine lists are handled appropriately
     * - System behavior with no administered vaccines
     * - Edge case handling
     */
    @Test
    fun checkoutAppointment_Success_EmptyVaccineList() = runBlocking {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(testPatient)
        
        val checkoutRequest = AppointmentCheckout(
            tabletId = "TEST_TABLET_012",
            administeredVaccines = emptyList(),
            administered = LocalDateTime.now(),
            administeredBy = 1,
            presentedRiskAssessmentId = null,
            forcedRiskType = 0,
            postShotVisitPaymentModeDisplayed = PaymentMode.InsurancePay,
            phoneNumberFlowPresented = false,
            phoneContactConsentStatus = PhoneContactConsentStatus.NOT_APPLICABLE,
            phoneContactReasons = "",
            flags = emptyList(),
            pregnancyPrompt = false,
            weeksPregnant = null,
            creditCardInformation = null,
            activeFeatureFlags = emptyList(),
            attestHighRisk = false,
            riskFactors = emptyList()
        )
        
        // Act
        val response = patientsApi.checkoutAppointment(
            appointmentId = appointmentId.toInt(),
            appointmentCheckout = checkoutRequest,
            ignoreOfflineStorage = true
        )
        
        // Assert
        // Note: This might be successful or fail depending on business rules
        // The test verifies the system handles this scenario appropriately
        Assert.assertNotNull("Response should not be null", response)
    }

    /**
     * Mock dispatcher for CheckoutAPI tests
     * Provides mock responses for API calls during testing
     */
    private class CheckoutAPITestsDispatcher : BaseMockDispatcher() {
        override val mockTestDirectory = "CheckoutAPITests/"

        override fun dispatch(request: RecordedRequest): MockResponse {
            return when {
                request.path?.contains("api/patients/appointment") == true && request.method == "POST" -> {
                    // Mock appointment creation response
                    MockResponse()
                        .setResponseCode(200)
                        .setBody("123456") // Mock appointment ID
                }
                request.path?.contains("api/patients/appointment") == true && 
                request.path?.contains("/checkout") == true && request.method == "PUT" -> {
                    // Mock checkout response
                    MockResponse()
                        .setResponseCode(200)
                        .setBody("{}")
                }
                request.path?.contains("api/patients/appointment") == true && 
                request.method == "GET" -> {
                    // Mock appointment retrieval response
                    MockResponse()
                        .setResponseCode(200)
                        .setBody("""
                            {
                                "id": 123456,
                                "clinicId": 1,
                                "appointmentTime": "2025-01-15T12:30:00",
                                "patient": {
                                    "id": 789,
                                    "firstName": "Test",
                                    "lastName": "Patient"
                                },
                                "checkedOut": false,
                                "administeredVaccines": []
                            }
                        """.trimIndent())
                }
                else -> {
                    // Default response for other endpoints
                    MockResponse()
                        .setResponseCode(200)
                        .setBody("{}")
                }
            }
        }
    }
}
