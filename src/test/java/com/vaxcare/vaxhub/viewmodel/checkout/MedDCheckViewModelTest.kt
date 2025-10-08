/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel.checkout

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.core.dispatcher.TestDispatcherProvider
import com.vaxcare.vaxhub.data.MockAppointments
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.AppointmentDetailDto
import com.vaxcare.vaxhub.model.appointment.EncounterMessageEntity
import com.vaxcare.vaxhub.model.appointment.EncounterMessageJson
import com.vaxcare.vaxhub.model.appointment.EncounterStateEntity
import com.vaxcare.vaxhub.model.appointment.EncounterStateJson
import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.util.FlowDispatcherRule
import com.vaxcare.vaxhub.viewmodel.MedDCheckViewModel
import com.vaxcare.vaxhub.viewmodel.MedDCheckViewModel.MedDCheckState
import com.vaxcare.vaxhub.viewmodel.State
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import java.math.BigDecimal

private const val CHECK_NOT_RUN_ID = 0
private const val MISSING_MBI_AND_SSN = 1
private const val CHECK_DONE_ID = 2

class MedDCheckViewModelTest {
    @get:Rule
    var dispatcherRule = FlowDispatcherRule()

    @get:Rule
    val rule: TestRule = InstantTaskExecutorRule()

    @RelaxedMockK
    lateinit var appointmentRepository: AppointmentRepository

    @RelaxedMockK
    lateinit var analyticReport: AnalyticReport

    private val dispatcher = TestDispatcherProvider()

    private val medDCheckViewModel: MedDCheckViewModel by lazy {
        MedDCheckViewModel(
            appointmentRepository = appointmentRepository,
            dispatcherProvider = dispatcher,
            analytics = analyticReport,
            partDService = mockk(relaxed = true)
        )
    }

    private val autoRunFailAppointment =
        MockAppointments.medDAppointmentCheckNotRunAndMissingMedDDemo
    private val checkNotRunAppointment = MockAppointments.medDAppointmentCheckNotRun
    private val checkDoneAppointment = MockAppointments.medDAppointmentCheckFinished

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery {
            appointmentRepository.getAppointmentByIdAsync(CHECK_NOT_RUN_ID)
        } returns checkNotRunAppointment
        coEvery {
            appointmentRepository.getAppointmentByIdAsync(MISSING_MBI_AND_SSN)
        } returns autoRunFailAppointment
        coEvery {
            appointmentRepository.getAppointmentByIdAsync(CHECK_DONE_ID)
        } returns checkDoneAppointment
        coEvery {
            appointmentRepository.doMedDCheck(any(), any())
        } returns Unit
        coEvery {
            appointmentRepository.getAndInsertUpdatedAppointment(CHECK_DONE_ID)
        } returns checkDoneAppointment.toAppointmentDetailDto()
        coEvery { appointmentRepository.getPartDCopays(any()) } returns null
        coEvery { analyticReport.saveMetric(any()) } returns Unit
    }

    @Test
    fun `MedD Check Run Fail`() {
        runTest {
            val expectedStates =
                listOf(MedDCheckState.MissingCheckField(autoRunFailAppointment))
            val collectedStates = mutableListOf<State>()
            medDCheckViewModel.runMedDCheckIfAvailable(MISSING_MBI_AND_SSN, true)
            medDCheckViewModel.medDCheckUIState.take(1).collect { collectedStates.add(it) }
            assert(collectedStates.toList() == expectedStates)
        }
    }

    @Test
    fun `Med D Check Run Success`() {
        runTest(
            context = dispatcher.default
        ) {
            val expectedMedDInfo = MedDInfo(
                eligible = true,
                copays = listOf(
                    ProductCopayInfo(
                        antigen = MedDVaccines.TDAP,
                        copay = BigDecimal.ZERO
                    ),
                    ProductCopayInfo(
                        antigen = MedDVaccines.ZOSTER,
                        copay = BigDecimal.ZERO
                    ),
                    ProductCopayInfo(
                        antigen = MedDVaccines.RSV,
                        copay = BigDecimal.ZERO
                    )
                )
            )
            val expectedStates = listOf(
                MedDCheckState.MedDCheckAutoRunning(checkNotRunAppointment),
                MedDCheckState.CopayResponse(expectedMedDInfo)
            )
            val collectedStates = mutableListOf<State>()
            medDCheckViewModel.runMedDCheckIfAvailable(CHECK_NOT_RUN_ID, true)
            delay(500L)
            medDCheckViewModel.updateAppointmentDetails(CHECK_DONE_ID, expectedMedDInfo)
            medDCheckViewModel.medDCheckUIState.take(2).collect { collectedStates.add(it) }
            println("collected: $collectedStates\nexpected: $expectedStates")
            assert(collectedStates.toList() == expectedStates)
        }
    }

    private fun Appointment.toAppointmentDetailDto() =
        AppointmentDetailDto(
            id = id,
            clinicId = clinicId,
            appointmentTime = appointmentTime,
            patient = patient,
            paymentType = paymentType,
            paymentMethod = paymentMethod,
            stock = vaccineSupply,
            visitType = visitType,
            checkedOut = checkedOut,
            checkedOutTime = checkedOutTime,
            provider = provider,
            administeredBy = administeredBy,
            isEditable = isEditable,
            encounterState = encounterState!!.toEncounterStateJson(),
            administeredVaccines = administeredVaccines
        )

    private fun EncounterStateEntity.toEncounterStateJson() =
        EncounterStateJson(
            id = id,
            appointmentId = appointmentId,
            shotStatus = shotStatus,
            isClosed = isClosed,
            createdUtc = createdUtc,
            messages = messages.map { it.toEncounterMessageJson() }
        )

    private fun EncounterMessageEntity.toEncounterMessageJson() =
        EncounterMessageJson(
            riskAssessmentId = riskAssessmentId,
            appointmentId = appointmentId,
            status = status,
            icon = icon,
            primaryMessage = primaryMessage,
            secondaryMessage = secondaryMessage,
            callToAction = callToAction,
            topRejectCode = topRejectCode,
            serviceType = serviceType
        )
}
