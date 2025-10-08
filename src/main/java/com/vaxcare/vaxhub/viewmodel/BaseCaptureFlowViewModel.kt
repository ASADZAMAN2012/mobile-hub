/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.core.model.enums.AddPatientSource
import com.vaxcare.vaxhub.model.CurbsideInsuranceID
import com.vaxcare.vaxhub.model.PaymentInformation
import com.vaxcare.vaxhub.repository.AppointmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BaseCaptureFlowViewModel @Inject constructor(
    val appointmentRepository: AppointmentRepository,
    val dispatcherProvider: DispatcherProvider
) : BaseViewModel() {
    fun checkIfParentPatient(addPatientSource: AddPatientSource, appointmentId: Int) {
        setState(LoadingState)

        viewModelScope.launch(dispatcherProvider.io) {
            if (!isAddingParentPatient(addPatientSource)) {
                setState(BaseCaptureFlowState.NavigateToNextStep)
                return@launch
            }

            appointmentRepository.getAppointmentByIdAsync(appointmentId)?.let {
                val shouldSkipInsuranceDialog =
                    it.patient.paymentInformation?.primaryInsuranceId?.let { insuranceId ->
                        CurbsideInsuranceID.values().any { it.value == insuranceId }
                    } ?: false

                if (shouldSkipInsuranceDialog) {
                    setState(BaseCaptureFlowState.NavigateToNextStep)
                } else {
                    setState(BaseCaptureFlowState.DisplaySameInsuranceDialog)
                }
            } ?: setState(BaseCaptureFlowState.DisplaySameInsuranceDialog)
        }
    }

    fun getInfoToCreateAppointmentWithInsurance(addPatientSource: AddPatientSource, appointmentId: Int) {
        setState(LoadingState)

        viewModelScope.launch(dispatcherProvider.io) {
            appointmentRepository.getAppointmentByIdAsync(appointmentId)?.let { appointment ->
                appointment.patient.paymentInformation?.let {
                    setState(
                        BaseCaptureFlowState.CreateAppointmentWithInsurance(
                            addPatientSource = addPatientSource,
                            paymentInformation = it,
                            providerId = appointment.provider.id
                        )
                    )
                }
            }
        }
    }

    private fun isAddingParentPatient(addPatientSource: AddPatientSource): Boolean {
        return addPatientSource == AddPatientSource.ADD_SUGGEST_PARENT_PATIENT ||
            addPatientSource == AddPatientSource.ADD_NEW_PARENT_PATIENT
    }
}

sealed class BaseCaptureFlowState : State {
    object NavigateToNextStep : BaseCaptureFlowState()

    object DisplaySameInsuranceDialog : BaseCaptureFlowState()

    data class CreateAppointmentWithInsurance(
        val addPatientSource: AddPatientSource,
        val paymentInformation: PaymentInformation,
        val providerId: Int
    ) : BaseCaptureFlowState()
}
