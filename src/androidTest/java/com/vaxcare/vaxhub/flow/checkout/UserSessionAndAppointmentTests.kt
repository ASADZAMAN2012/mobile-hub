/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.checkout

import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vaxcare.vaxhub.BuildConfig
import com.vaxcare.vaxhub.TestWorkManagerHelper
import com.vaxcare.vaxhub.common.HomeScreenUtil
import com.vaxcare.vaxhub.common.PatientUtil
import com.vaxcare.vaxhub.common.StorageUtil
import com.vaxcare.vaxhub.data.TestPartners
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.flow.TestsBase
import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.ui.PermissionsActivity
import com.vaxcare.vaxhub.web.PatientsApi
import com.vaxcare.vaxhub.service.UserSessionService
import retrofit2.HttpException
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * Focused test suite for user session creation and appointment creation
 * 
 * This test suite verifies:
 * 1. User session creation works properly
 * 2. Appointment creation via API works
 * 3. Basic authentication and API connectivity
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@HiltAndroidTest
class UserSessionAndAppointmentTests : TestsBase() {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var storageUtil: StorageUtil

    @Inject
    lateinit var patientsApi: PatientsApi
    
    @Inject
    lateinit var userSessionService: UserSessionService

    private val testWorkManagerHelper = TestWorkManagerHelper()
    private lateinit var scenario: ActivityScenario<PermissionsActivity>
    private val patientUtil = PatientUtil()
    private val homeScreenUtil = HomeScreenUtil()
    private val testPartner = TestPartners.TestPartner()

    companion object {
        private var globalScenario: ActivityScenario<PermissionsActivity>? = null
        private var isLoggedIn = false
        private var isInitialized = false
        
        @JvmStatic
        @BeforeClass
        fun setUpOnce() {
            // Launch activity once before all tests
            globalScenario = ActivityScenario.launch(PermissionsActivity::class.java)
            
            // Wait for activity to be ready
            Thread.sleep(2000)
            
            // Login once before all tests (this will be done in first @Before)
            isLoggedIn = false
            isInitialized = true
        }
        
        @JvmStatic
        @AfterClass
        fun tearDownOnce() {
            // Close activity after all tests
            globalScenario?.close()
        }
    }

    @Before
    fun setUp() {
        hiltRule.inject()
        // Initialize WorkManager for API tests
        testWorkManagerHelper.initializeWorkManager(workerFactory)
        // Use global scenario (launched in @BeforeClass)
        scenario = globalScenario!!

        // Login only once (first test) - bypass UI login
        if (!isLoggedIn) {
            setupUserSessionDirectly(testPartner)
            isLoggedIn = true
        }
    }

    @After
    fun tearDown() {
        // No cleanup needed - maintain session state
    }
    
    /**
     * Setup user session directly without UI login
     * This bypasses the UI login flow and directly sets up the authentication state
     */
    private fun setupUserSessionDirectly(testPartner: TestPartners) {
        try {
            // Create a new user session using injected UserSessionService
            userSessionService.generateAndCacheNewUserSessionId()
            
            // Verify session is created
            val sessionId = userSessionService.getCurrentUserSessionId()
            Assert.assertNotNull("User session should be created", sessionId)
            
        } catch (e: Exception) {
            // Fallback to UI login if direct setup fails
            homeScreenUtil.loginAsTestPartner(testPartner)
            homeScreenUtil.tapHomeScreenAndPinIn(testPartner)
        }
    }

    /**
     * Test user session creation
     * 
     * This test verifies that user session creation works properly
     * and that the session ID is available for API calls
     */
    @Test
    fun testUserSessionCreation() {
        // Act - Create user session
        userSessionService.generateAndCacheNewUserSessionId()
        
        // Assert - Verify session was created
        val sessionId = userSessionService.getCurrentUserSessionId()
        Assert.assertNotNull("User session should be created", sessionId)
        Assert.assertTrue("Session ID should not be empty", sessionId.toString().isNotEmpty())
        
        println("✅ User session created successfully: $sessionId")
    }

    /**
     * Test that user session is properly maintained across test setup
     * 
     * This test verifies that the session created in setupUserSessionDirectly()
     * is still available and valid
     */
    @Test
    fun testUserSessionPersistence() {
        // Assert - Verify session is still available from setup
        val sessionId = userSessionService.getCurrentUserSessionId()
        Assert.assertNotNull("User session should be available from setup", sessionId)
        Assert.assertTrue("Session ID should not be empty", sessionId.toString().isNotEmpty())
        
        println("✅ User session persisted successfully: $sessionId")
    }

    /**
     * Test appointment creation via API
     * 
     * This test verifies that we can create an appointment using the API
     * with proper authentication
     */
    @Test
    fun testAppointmentCreation() = runBlocking {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val visitDate = LocalDateTime.now()
        val patientPostBody = generatePatientPostBody(testPatient, visitDate)
        
        // Act
        try {
            val appointmentId = patientsApi.postAppointment(patientPostBody)
            
            // Assert
            Assert.assertNotNull("Appointment ID should not be null", appointmentId)
            Assert.assertTrue("Appointment ID should not be empty", appointmentId.isNotEmpty())
            
            println("✅ Appointment created successfully: $appointmentId")
            
        } catch (e: Exception) {
            println("❌ Appointment creation failed: ${e.message}")
            println("Exception type: ${e.javaClass.simpleName}")
            if (e is retrofit2.HttpException) {
                println("HTTP Code: ${e.code()}")
                println("HTTP Message: ${e.message()}")
                println("Response Body: ${e.response()?.errorBody()?.string()}")
            }
            throw e
        }
    }

    /**
     * Test appointment creation with different patient types
     * 
     * This test verifies appointment creation works with various patient configurations
     */
    @Test
    fun testAppointmentCreationWithDifferentPatients() = runBlocking {
        val patients = listOf(
            TestPatients.RiskFreePatientForCheckout(),
            TestPatients.SelfPayPatient(),
            TestPatients.VFCPatient()
        )
        
        for (patient in patients) {
            try {
                val visitDate = LocalDateTime.now()
                val patientPostBody = generatePatientPostBody(patient, visitDate)
                val appointmentId = patientsApi.postAppointment(patientPostBody)
                
                Assert.assertNotNull("Appointment ID should not be null for ${patient.firstName}", appointmentId)
                println("✅ Appointment created for ${patient.firstName}: $appointmentId")
                
            } catch (e: Exception) {
                println("❌ Appointment creation failed for ${patient.firstName}: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Test appointment retrieval
     * 
     * This test verifies that we can retrieve an appointment after creating it
     */
    @Test
    fun testAppointmentRetrieval() = runBlocking {
        // Arrange - Create an appointment first
        val testPatient = TestPatients.RiskFreePatientForCheckout()
        val visitDate = LocalDateTime.now()
        val patientPostBody = generatePatientPostBody(testPatient, visitDate)
        
        val appointmentId = patientsApi.postAppointment(patientPostBody)
        Assert.assertNotNull("Appointment should be created first", appointmentId)
        
        // Act - Retrieve the appointment
        try {
            val appointment = patientsApi.getAppointment(appointmentId.toInt())
            
            // Assert
            Assert.assertNotNull("Appointment should be retrieved", appointment)
            Assert.assertEquals("Appointment ID should match", appointmentId.toInt(), appointment.id)
            
            println("✅ Appointment retrieved successfully: ${appointment.id}")
            
        } catch (e: Exception) {
            println("❌ Appointment retrieval failed: ${e.message}")
            println("Exception type: ${e.javaClass.simpleName}")
            if (e is retrofit2.HttpException) {
                println("HTTP Code: ${e.code()}")
                println("HTTP Message: ${e.message()}")
                println("Response Body: ${e.response()?.errorBody()?.string()}")
            }
            throw e
        }
    }

    /**
     * Generate patient post body for API calls
     */
    private fun generatePatientPostBody(patient: TestPatients, visitDate: LocalDateTime): PatientPostBody {
        return PatientPostBody(
            newPatient = PatientPostBody.NewPatient(
                firstName = patient.firstName,
                lastName = patient.lastName,
                dob = patient.dateOfBirth,
                gender = patient.gender,
                phoneNumber = "1234567890",
                address1 = null,
                address2 = null,
                city = null,
                state = "FL",
                zip = null,
                paymentInformation = PatientPostBody.PaymentInformation(
                    primaryInsuranceId = patient.primaryInsuranceId,
                    primaryMemberId = patient.primaryMemberId,
                    primaryGroupId = patient.primaryGroupId,
                    uninsured = false
                ),
                race = null,
                ethnicity = null,
                ssn = patient.ssn
            ),
            clinicId = getCurrentClinicId(),
            date = visitDate,
            providerId = 0,
            initialPaymentMode = patient.paymentMode,
            visitType = "Well"
        )
    }

    /**
     * Get current clinic ID from storage
     */
    private fun getCurrentClinicId(): Long {
        return storageUtil.entryPoint.localStorage().currentClinicId
    }
}
