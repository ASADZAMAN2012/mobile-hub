/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.model.PaymentInformation
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.util.getOrAwaitValue
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.time.LocalDateTime

class PatientEditCollectPhoneViewModelTest {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    private var collectPhoneViewModel: PatientEditCollectPhoneViewModel? = null

    @RelaxedMockK
    lateinit var appointmentRepository: AppointmentRepository

    @RelaxedMockK
    lateinit var locationRepository: LocationRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        collectPhoneViewModel =
            PatientEditCollectPhoneViewModel(appointmentRepository, locationRepository)
    }

//    Volatile test case, commented to prevent build issues
//    @Test
//    fun testSetPhoneHint() {
//        every {
//            runBlocking {
//                locationRepository
//                    .getFeatureFlagByConstant(any())
//            }
//        } returns FeatureFlag(999, "")
//        collectPhoneViewModel?.setPhoneHint("", null)
//        val phoneState: State? = collectPhoneViewModel?.state?.getOrAwaitValue()
//        Assert.assertTrue(phoneState is PatientEditCollectPhoneViewModel.CollectPhoneState.InitialState)
//
//        every {
//            runBlocking {
//                appointmentRepository.getAppointmentByIdAsync(any())
//            }
//        } returns providerAppointment("111-333-4444")
//
//        collectPhoneViewModel?.setPhoneHint(null, 0)
//        val appointmentPhoneState: State? =
//            collectPhoneViewModel?.state?.getOrAwaitValue(changeEventsCount = 2)
//        println("State: $appointmentPhoneState")
//        Assert.assertTrue(appointmentPhoneState is PatientEditCollectPhoneViewModel.CollectPhoneState.PhoneNumberFetched)
//        val phoneNumberFetched: PatientEditCollectPhoneViewModel.CollectPhoneState.PhoneNumberFetched =
//            appointmentPhoneState as PatientEditCollectPhoneViewModel.CollectPhoneState.PhoneNumberFetched
//        Assert.assertEquals("111", phoneNumberFetched.area)
//        Assert.assertEquals("333", phoneNumberFetched.prefix)
//        Assert.assertEquals("4444", phoneNumberFetched.line)
//    }

    @Test
    fun testValidateAndUpdatePhone() {
        collectPhoneViewModel?.validateAndUpdatePhone("111", "222", "5555")
        val validPhone: State? = collectPhoneViewModel?.state?.getOrAwaitValue()
        Assert.assertTrue(validPhone is PatientEditCollectPhoneViewModel.CollectPhoneState.PhoneNumberProvided)

        collectPhoneViewModel?.validateAndUpdatePhone("111", "222", "555")
        val invalidPhone: State? = collectPhoneViewModel?.state?.getOrAwaitValue()
        Assert.assertTrue(invalidPhone is PatientEditCollectPhoneViewModel.CollectPhoneState.PhoneNumberNotValid)
    }

    private fun providerAppointment(phoneNumber: String): Appointment {
        return Appointment(
            id = 0,
            clinicId = 0,
            vaccineSupply = "",
            appointmentTime = LocalDateTime.now(),
            patient = Patient(
                0, "", "", "", "", "", "", "", "", "", "", "", "", "", "", phoneNumber, "",
                PaymentInformation(0, "", 0, 0, "", "", false, PaymentMethod.EmployerPay)
            ),
            paymentType = "",
            paymentMethod = PaymentMethod.NoPay,
            visitType = "",
            checkedOut = false,
            checkedOutTime = null,
            provider = Provider(0, "", ""),
            administeredBy = null,
            isEditable = false,
            encounterState = null,
            administeredVaccines = listOf(),
            isProcessing = false,
            orders = listOf()
        )
    }
}
