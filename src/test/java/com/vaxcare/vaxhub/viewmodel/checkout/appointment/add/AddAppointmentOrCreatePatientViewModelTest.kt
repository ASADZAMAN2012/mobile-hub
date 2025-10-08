package com.vaxcare.vaxhub.viewmodel.checkout.appointment.add

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.core.dispatcher.TestDispatcherProvider
import com.vaxcare.vaxhub.model.Clinic
import com.vaxcare.vaxhub.model.metric.TroubleConnectingDialogClickMetric
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.ClinicRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.PatientRepository
import com.vaxcare.vaxhub.repository.ProductRepository
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.useCase.DeterminePatientCheckoutInitialScreenUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.time.LocalDate
import java.time.ZoneId

@ExperimentalCoroutinesApi
class AddAppointmentOrCreatePatientViewModelTest {
    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AddAppointmentOrCreatePatientViewModel

    private val patientRepository = mockk<PatientRepository>()
    private val appointmentRepository = mockk<AppointmentRepository>()
    private val clinicRepository = mockk<ClinicRepository>()
    private val analytics = mockk<AnalyticReport>()
    private val networkMonitor = mockk<NetworkMonitor>()
    private val productRepository = mockk<ProductRepository>()
    private val locationRepository = mockk<LocationRepository>()
    private val determinePatientCheckoutInitialScreenUseCase = mockk<DeterminePatientCheckoutInitialScreenUseCase>()

    @Before
    fun setUp() {
        viewModel = AddAppointmentOrCreatePatientViewModel(
            patientRepository,
            appointmentRepository,
            clinicRepository,
            analytics,
            networkMonitor,
            productRepository,
            locationRepository,
            TestDispatcherProvider(),
            determinePatientCheckoutInitialScreenUseCase
        )
    }

    @Test
    fun `saveMetric should call analytics saveMetric`() {
        val metric = mockk<TroubleConnectingDialogClickMetric>()

        coEvery { analytics.saveMetric(metric) } coAnswers {
            // Nothing
        }

        viewModel.saveMetric(metric)
        coVerify { analytics.saveMetric(metric) }
    }

    @Test
    fun `rangeOfDaysForAppointments should return correct range`() =
        runBlocking {
            val zoneId = ZoneId.systemDefault()
            val clinic = mockk<Clinic>()
            coEvery { clinicRepository.getCurrentClinic() } returns clinic
            val today = LocalDate.now(zoneId)

            // Test case 1: No temporary clinic
            coEvery { clinic.isTemporaryClinic() } returns false
            val expectedRange = Pair(
                today.minusDays(7L).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                today.plusDays(7L).atStartOfDay(zoneId).toInstant().toEpochMilli()
            )
            val actualRange = viewModel.rangeOfDaysForAppointments()
            assertEquals(expectedRange, actualRange)

            // Test case 2: Temporary clinic with no start date
            coEvery { clinic.isTemporaryClinic() } returns true
            coEvery { clinic.startDate } returns null
            val expectedRange2 = Pair(
                today.minusDays(7L).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                today.plusDays(7L).atStartOfDay(zoneId).toInstant().toEpochMilli()
            )
            val actualRange2 = viewModel.rangeOfDaysForAppointments()
            assertEquals(expectedRange2, actualRange2)

            // Test case 3: Temporary clinic with start date
            val startDate = today.minusDays(10L)
            coEvery { clinic.isTemporaryClinic() } returns true
            coEvery { clinic.startDate } returns startDate
            val expectedRange3 = Pair(
                startDate.minusDays(7L).atStartOfDay(zoneId).toInstant().toEpochMilli(),
                startDate.plusDays(7L).atStartOfDay(zoneId).toInstant().toEpochMilli()
            )
            val actualRange3 = viewModel.rangeOfDaysForAppointments()
            assertEquals(expectedRange3, actualRange3)
        }
}
