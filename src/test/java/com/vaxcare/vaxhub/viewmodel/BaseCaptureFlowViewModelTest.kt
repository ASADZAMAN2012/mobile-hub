/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.vaxcare.vaxhub.core.dispatcher.TestDispatcherProvider
import com.vaxcare.vaxhub.core.model.enums.AddPatientSource
import com.vaxcare.vaxhub.data.MockAppointments
import com.vaxcare.vaxhub.data.MockPatients
import com.vaxcare.vaxhub.data.MockPaymentInformation
import com.vaxcare.vaxhub.repository.AppointmentRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseCaptureFlowViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val appointmentRepository: AppointmentRepository = mockk()
    private lateinit var viewModel: BaseCaptureFlowViewModel
    private val stateObserver: Observer<State> = mockk(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = BaseCaptureFlowViewModel(appointmentRepository, TestDispatcherProvider())
        viewModel.state.observeForever(stateObserver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.state.removeObserver(stateObserver)
    }

    @Test
    fun `checkIfParentPatient should emit NavigateToNextStep state when not adding parent`() =
        runTest {
            viewModel.checkIfParentPatient(AddPatientSource.OTHER, 1)

            advanceUntilIdle()

            verify { stateObserver.onChanged(LoadingState) }
            verify { stateObserver.onChanged(BaseCaptureFlowState.NavigateToNextStep) }
        }

    @Test
    fun `checkIfParentPatient should emit DisplaySameInsuranceDialog state when appointment not found`() =
        runTest {
            coEvery { appointmentRepository.getAppointmentByIdAsync(1) } returns null

            viewModel.checkIfParentPatient(AddPatientSource.ADD_NEW_PARENT_PATIENT, 1)

            advanceUntilIdle()

            verify { stateObserver.onChanged(LoadingState) }
            verify { stateObserver.onChanged(BaseCaptureFlowState.DisplaySameInsuranceDialog) }
        }

    @Test
    fun `checkIfParentPatient should emit NavigateToNextStep state when appointment has Curbside insurance`() =
        runTest {
            val appointment = MockAppointments.medDAppointmentCheckNotRun.copy(
                patient = MockPatients.medDPatient65.copy(
                    paymentInformation = MockPaymentInformation.withCurbsideInsurance
                )
            )
            coEvery { appointmentRepository.getAppointmentByIdAsync(1) } returns appointment

            viewModel.checkIfParentPatient(AddPatientSource.ADD_NEW_PARENT_PATIENT, 1)

            advanceUntilIdle()

            verify { stateObserver.onChanged(LoadingState) }
            verify { stateObserver.onChanged(BaseCaptureFlowState.NavigateToNextStep) }
        }

    @Test
    fun `checkIfParentPatient should emit DisplaySameInsuranceDialog state when appointment does not have Curbside insurance`() =
        runTest {
            val appointment = MockAppointments.medDAppointmentCheckNotRun
            coEvery { appointmentRepository.getAppointmentByIdAsync(1) } returns appointment

            viewModel.checkIfParentPatient(AddPatientSource.ADD_NEW_PARENT_PATIENT, 1)

            advanceUntilIdle()

            verify { stateObserver.onChanged(LoadingState) }
            verify { stateObserver.onChanged(BaseCaptureFlowState.DisplaySameInsuranceDialog) }
        }
}
