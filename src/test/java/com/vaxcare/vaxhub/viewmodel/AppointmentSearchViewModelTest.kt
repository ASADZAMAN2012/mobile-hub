/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vaxcare.vaxhub.core.dispatcher.TestDispatcherProvider
import com.vaxcare.vaxhub.model.AdministeredVaccine
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase
import com.vaxcare.vaxhub.util.getOrAwaitValue
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.time.LocalDateTime

class AppointmentSearchViewModelTest {
    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    private var appointmentSearchViewModel: AppointmentSearchViewModel? = null

    @RelaxedMockK
    lateinit var appointmentRepository: AppointmentRepository

    @RelaxedMockK
    lateinit var productRepository: ProductRepository

    @RelaxedMockK
    lateinit var locationRepository: LocationRepository

    @RelaxedMockK
    lateinit var determinePatientCheckoutInitialScreenUseCase: DeterminePatientCheckoutInitialScreenUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        appointmentSearchViewModel =
            AppointmentSearchViewModel(
                appointmentRepository,
                productRepository,
                locationRepository,
                TestDispatcherProvider(),
                determinePatientCheckoutInitialScreenUseCase
            )
    }

    @Ignore("Volatile test")
    @Test
    fun filterSearchResults() =
        runTest {
            val query = "abc"
            val appointmentList = listOf(
                getAppointment(1, query, LocalDateTime.now()),
                getAppointment(
                    2,
                    query,
                    LocalDateTime.now().minusMinutes(20),
                    true,
                    listOf(getLarcVaccine())
                ),
                getAppointment(
                    3,
                    query,
                    LocalDateTime.now().minusMinutes(40),
                    true,
                    listOf(getNonLarcVaccine())
                )
            )

            coEvery {
                productRepository.getLarcProductIDs()
            } returns getLarcList()

            coEvery {
                appointmentRepository.getAppointmentsByIdentifierAsync(
                    any(),
                    any(),
                    any()
                )
            } returns appointmentList

            appointmentSearchViewModel?.filterSearchResults(query)
            var state = appointmentSearchViewModel?.state?.getOrAwaitValue()
            Assert.assertTrue(state is LoadingState)

            state = appointmentSearchViewModel?.state?.getOrAwaitValue(changeEventsCount = 2)
            Assert.assertTrue(state is AppointmentSearchViewModel.AppointmentSearchState.FilterSearchResultState)

            val searchResults =
                state as AppointmentSearchViewModel.AppointmentSearchState.FilterSearchResultState
            Assert.assertEquals(2, searchResults.value.size)
            Assert.assertEquals(1, searchResults.value[0].id)
            Assert.assertEquals(3, searchResults.value[1].id)
        }

    private fun getLarcList() = listOf(101)

    private fun getLarcVaccine() =
        AdministeredVaccine(
            id = 101,
            checkInVaccinationId = 101,
            appointmentId = 2,
            lotNumber = "larc",
            ageIndicated = false,
            method = null,
            site = null,
            productId = 101,
            synced = null,
            doseSeries = null,
            deletedDate = null,
            isDeleted = false
        )

    private fun getNonLarcVaccine() =
        AdministeredVaccine(
            id = 102,
            checkInVaccinationId = 102,
            appointmentId = 3,
            lotNumber = "larc",
            ageIndicated = false,
            method = null,
            site = null,
            productId = 102,
            synced = null,
            doseSeries = null,
            deletedDate = null,
            isDeleted = false
        )

    private fun getPatient(firstName: String) =
        Patient(
            id = 100,
            originatorPatientId = null,
            firstName = firstName,
            lastName = "def",
            dob = null,
            middleInitial = null,
            race = null,
            ethnicity = null,
            gender = null,
            ssn = null,
            address1 = null,
            address2 = null,
            city = null,
            state = null,
            zip = null,
            phoneNumber = null,
            email = null,
            paymentInformation = null
        )

    private fun getAppointment(
        id: Int,
        firstName: String,
        appointmentTime: LocalDateTime,
        checkedOut: Boolean = false,
        administeredVaccines: List<AdministeredVaccine> = mutableListOf()
    ): Appointment =
        Appointment(
            id = id,
            clinicId = 1,
            vaccineSupply = "private",
            appointmentTime = appointmentTime,
            patient = getPatient(firstName),
            paymentType = null,
            paymentMethod = PaymentMethod.EmployerPay,
            visitType = "",
            checkedOut = checkedOut,
            checkedOutTime = if (checkedOut) appointmentTime else null,
            provider = Provider(10, "first", "last"),
            administeredBy = 0,
            isEditable = false,
            encounterState = null,
            administeredVaccines = administeredVaccines,
            isProcessing = false,
            orders = mutableListOf()
        )
}
