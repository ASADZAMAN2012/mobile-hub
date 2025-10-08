/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.flow.appointment

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.vaxcare.vaxhub.common.PatientUtil
import com.vaxcare.vaxhub.data.TestPatients
import com.vaxcare.vaxhub.flow.TestsBase
import com.vaxcare.vaxhub.ui.PermissionsActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

/**
 * End-to-End API tests for creating appointments
 * 
 * These tests verify the complete flow of creating appointments through the API,
 * including success scenarios, validation, and error handling.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class CreateAppointmentE2ETests : TestsBase() {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @get:Rule
    var activityRule = ActivityTestRule(PermissionsActivity::class.java)

    @Inject
    lateinit var storageUtil: StorageUtil

    private val patientUtil = PatientUtil()

    @Before
    fun setUp() {
        hiltRule.inject()
        // Clear any existing data before each test
        storageUtil.clearLocalStorageAndDatabase()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        storageUtil.clearLocalStorageAndDatabase()
    }

    /**
     * Test successful appointment creation with a risk-free patient
     * 
     * This test verifies:
     * - Patient and appointment are created successfully
     * - Appointment ID is returned
     * - Appointment can be retrieved by ID
     * - Patient information is correctly stored
     */
    @Test
    fun createAppointment_Success_RiskFreePatient() {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCreatePatient()
        val appointmentDate = LocalDateTime.now().plusDays(1)
        
        // Act
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(
            patient = testPatient,
            pastDays = 0
        )
        
        // Assert
        Assert.assertNotNull("Appointment ID should not be null", appointmentId)
        Assert.assertTrue("Appointment ID should be numeric", appointmentId.matches(Regex("\\d+")))
        
        // Verify appointment can be retrieved
        val appointment = patientUtil.getAppointmentById(appointmentId)
        Assert.assertNotNull("Appointment should be retrievable", appointment)
        Assert.assertEquals(
            "Patient first name should match",
            testPatient.firstName,
            appointment?.patient?.firstName
        )
        Assert.assertEquals(
            "Patient last name should match",
            testPatient.lastName,
            appointment?.patient?.lastName
        )
    }

    /**
     * Test appointment creation with past date
     * 
     * This test verifies that appointments can be created for past dates
     * (useful for testing historical data scenarios)
     */
    @Test
    fun createAppointment_Success_PastDate() {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCreatePatient()
        val pastDays = 7L // 7 days ago
        
        // Act
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(
            patient = testPatient,
            pastDays = pastDays
        )
        
        // Assert
        Assert.assertNotNull("Appointment ID should not be null", appointmentId)
        
        // Verify the appointment was created with the correct date
        val appointment = patientUtil.getAppointmentById(appointmentId)
        Assert.assertNotNull("Appointment should be retrievable", appointment)
        
        val expectedDate = LocalDate.now().minusDays(pastDays)
        val actualDate = appointment?.appointmentTime?.toLocalDate()
        Assert.assertEquals(
            "Appointment date should match expected past date",
            expectedDate,
            actualDate
        )
    }

    /**
     * Test appointment creation with different patient types
     * 
     * This test verifies that various patient types can create appointments
     */
    @Test
    fun createAppointment_Success_DifferentPatientTypes() {
        // Test with MedD patient
        val medDPatient = TestPatients.MedDPatientForCopayRequired()
        val medDAppointmentId = patientUtil.getAppointmentIdByCreateTestPatient(medDPatient)
        Assert.assertNotNull("MedD appointment should be created", medDAppointmentId)
        
        // Test with Self Pay patient
        val selfPayPatient = TestPatients.SelfPayPatient()
        val selfPayAppointmentId = patientUtil.getAppointmentIdByCreateTestPatient(selfPayPatient)
        Assert.assertNotNull("Self Pay appointment should be created", selfPayAppointmentId)
        
        // Test with VFC patient
        val vfcPatient = TestPatients.VFCPatient()
        val vfcAppointmentId = patientUtil.getAppointmentIdByCreateTestPatient(vfcPatient)
        Assert.assertNotNull("VFC appointment should be created", vfcAppointmentId)
    }

    /**
     * Test appointment creation with pregnant patient
     * 
     * This test verifies special handling for pregnant patients
     */
    @Test
    fun createAppointment_Success_PregnantPatient() {
        // Arrange
        val pregnantPatient = TestPatients.PregnantPatient()
        
        // Act
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(pregnantPatient)
        
        // Assert
        Assert.assertNotNull("Pregnant patient appointment should be created", appointmentId)
        
        val appointment = patientUtil.getAppointmentById(appointmentId)
        Assert.assertNotNull("Pregnant patient appointment should be retrievable", appointment)
        Assert.assertEquals(
            "Patient gender should be female",
            1, // Female gender code
            appointment?.patient?.gender
        )
    }

    /**
     * Test appointment creation with patient having SSN
     * 
     * This test verifies that patients with SSN can create appointments
     */
    @Test
    fun createAppointment_Success_PatientWithSSN() {
        // Arrange
        val patientWithSSN = TestPatients.MedDWithSsnPatientForCopayRequired()
        
        // Act
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(patientWithSSN)
        
        // Assert
        Assert.assertNotNull("Patient with SSN appointment should be created", appointmentId)
        
        val appointment = patientUtil.getAppointmentById(appointmentId)
        Assert.assertNotNull("Patient with SSN appointment should be retrievable", appointment)
        Assert.assertEquals(
            "Patient SSN should match",
            patientWithSSN.ssn,
            appointment?.patient?.ssn
        )
    }

    /**
     * Test appointment creation with partner bill patient
     * 
     * This test verifies partner billing scenarios
     */
    @Test
    fun createAppointment_Success_PartnerBillPatient() {
        // Arrange
        val partnerBillPatient = TestPatients.PartnerBillPatient()
        
        // Act
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(partnerBillPatient)
        
        // Assert
        Assert.assertNotNull("Partner bill patient appointment should be created", appointmentId)
        
        val appointment = patientUtil.getAppointmentById(appointmentId)
        Assert.assertNotNull("Partner bill patient appointment should be retrievable", appointment)
        Assert.assertEquals(
            "Payment mode should be partner bill",
            partnerBillPatient.paymentMode,
            appointment?.paymentMode
        )
    }

    /**
     * Test appointment creation with missing payer information
     * 
     * This test verifies handling of patients with incomplete insurance information
     */
    @Test
    fun createAppointment_Success_MissingPayerInfo() {
        // Arrange
        val missingPayerPatient = TestPatients.MissingPatientWithPayerInfo()
        
        // Act
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(missingPayerPatient)
        
        // Assert
        Assert.assertNotNull("Missing payer info patient appointment should be created", appointmentId)
        
        val appointment = patientUtil.getAppointmentById(appointmentId)
        Assert.assertNotNull("Missing payer info appointment should be retrievable", appointment)
        // Note: The appointment should still be created even with missing payer info
        // as this is handled by the business logic
    }

    /**
     * Test appointment creation with missing demographic information
     * 
     * This test verifies handling of patients with incomplete demographic data
     */
    @Test
    fun createAppointment_Success_MissingDemoInfo() {
        // Arrange
        val missingDemoPatient = TestPatients.MissingPatientWithDemoInfo()
        
        // Act
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(missingDemoPatient)
        
        // Assert
        Assert.assertNotNull("Missing demo info patient appointment should be created", appointmentId)
        
        val appointment = patientUtil.getAppointmentById(appointmentId)
        Assert.assertNotNull("Missing demo info appointment should be retrievable", appointment)
    }

    /**
     * Test appointment creation with future date
     * 
     * This test verifies that appointments can be created for future dates
     */
    @Test
    fun createAppointment_Success_FutureDate() {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCreatePatient()
        val futureDays = 7L // 7 days in the future
        
        // Act
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(
            patient = testPatient,
            pastDays = -futureDays // Negative value for future date
        )
        
        // Assert
        Assert.assertNotNull("Future appointment should be created", appointmentId)
        
        val appointment = patientUtil.getAppointmentById(appointmentId)
        Assert.assertNotNull("Future appointment should be retrievable", appointment)
        
        val expectedDate = LocalDate.now().plusDays(futureDays)
        val actualDate = appointment?.appointmentTime?.toLocalDate()
        Assert.assertEquals(
            "Appointment date should match expected future date",
            expectedDate,
            actualDate
        )
    }

    /**
     * Test appointment creation with specific time
     * 
     * This test verifies that appointments are created with the correct time (12:30 PM)
     */
    @Test
    fun createAppointment_Success_SpecificTime() {
        // Arrange
        val testPatient = TestPatients.RiskFreePatientForCreatePatient()
        
        // Act
        val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(testPatient)
        
        // Assert
        Assert.assertNotNull("Appointment should be created", appointmentId)
        
        val appointment = patientUtil.getAppointmentById(appointmentId)
        Assert.assertNotNull("Appointment should be retrievable", appointment)
        
        val appointmentTime = appointment?.appointmentTime?.toLocalTime()
        val expectedTime = LocalTime.of(12, 30)
        Assert.assertEquals(
            "Appointment time should be 12:30 PM",
            expectedTime,
            appointmentTime
        )
    }

    /**
     * Test multiple appointment creation
     * 
     * This test verifies that multiple appointments can be created in sequence
     */
    @Test
    fun createAppointment_Success_MultipleAppointments() {
        // Arrange
        val patients = listOf(
            TestPatients.RiskFreePatientForCreatePatient(),
            TestPatients.MedDPatientForCopayRequired(),
            TestPatients.SelfPayPatient(),
            TestPatients.VFCPatient()
        )
        
        // Act & Assert
        val appointmentIds = mutableListOf<String>()
        
        patients.forEach { patient ->
            val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(patient)
            Assert.assertNotNull("Appointment should be created for ${patient.firstName}", appointmentId)
            appointmentIds.add(appointmentId)
        }
        
        // Verify all appointments are unique
        val uniqueIds = appointmentIds.toSet()
        Assert.assertEquals(
            "All appointment IDs should be unique",
            appointmentIds.size,
            uniqueIds.size
        )
        
        // Verify all appointments can be retrieved
        appointmentIds.forEach { appointmentId ->
            val appointment = patientUtil.getAppointmentById(appointmentId)
            Assert.assertNotNull("Appointment $appointmentId should be retrievable", appointment)
        }
    }

    /**
     * Test appointment creation with edge case dates
     * 
     * This test verifies appointment creation with various date scenarios
     */
    @Test
    fun createAppointment_Success_EdgeCaseDates() {
        val testPatient = TestPatients.RiskFreePatientForCreatePatient()
        
        // Test with different past days
        val pastDaysList = listOf(0L, 1L, 7L, 30L, 365L)
        
        pastDaysList.forEach { pastDays ->
            val appointmentId = patientUtil.getAppointmentIdByCreateTestPatient(
                patient = testPatient,
                pastDays = pastDays
            )
            
            Assert.assertNotNull("Appointment should be created for $pastDays days ago", appointmentId)
            
            val appointment = patientUtil.getAppointmentById(appointmentId)
            Assert.assertNotNull("Appointment should be retrievable for $pastDays days ago", appointment)
            
            val expectedDate = LocalDate.now().minusDays(pastDays)
            val actualDate = appointment?.appointmentTime?.toLocalDate()
            Assert.assertEquals(
                "Appointment date should match for $pastDays days ago",
                expectedDate,
                actualDate
            )
        }
    }
}
