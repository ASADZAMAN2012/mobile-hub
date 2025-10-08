/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.PayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditDriverLicenseViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository,
    private val payerRepository: PayerRepository
) : BaseViewModel() {
    sealed class EditDriverLicenseState : State {
        data class AppointmentFetched(val appointment: Appointment?) : EditDriverLicenseState()

        object Failure : EditDriverLicenseState()
    }

    private val _appointment: MutableLiveData<Appointment?> = MutableLiveData<Appointment?>(null)
    val appointment: LiveData<Appointment?> = _appointment

    fun getAppointment(appointmentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            appointmentRepository.getAndInsertUpdatedAppointment(appointmentId)
            val appointment = appointmentRepository.getAppointmentByIdAsync(appointmentId)
            _appointment.postValue(appointment)
        }
    }

    /**
     * Fetches the Appointment from the DAO and aligns the Payer InsuranceId and PlanId acoordingly
     * (when these values do NOT exist, they should be -1 and null. The back end will send us 0 and 0
     * which is what caused the 500s for some cases)
     *
     * @param appointmentId - The appointment ID for the selected Appointment
     * @return Coroutine Job that can be ignored.
     */
    fun fetchAppointmentAndAlignPayer(appointmentId: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            val state = try {
                val cachedAppt = appointmentRepository.getAppointmentByIdAsync(appointmentId)
                val identifier = cachedAppt?.patient?.paymentInformation?.insuranceName
                val alignedAppointment = identifier?.let {
                    val foundPayer = payerRepository.searchPayersAsync(it)
                        .firstOrNull { payer -> payer.insuranceName == it }
                    cachedAppt.let { appt ->
                        appt.copy(
                            patient = appt.patient
                                .copy(
                                    paymentInformation = appt.patient.paymentInformation
                                        ?.copy(
                                            primaryInsuranceId = foundPayer?.insuranceId ?: -1,
                                            primaryInsurancePlanId = foundPayer?.insurancePlanId,
                                        )
                                )
                        )
                    }
                }

                EditDriverLicenseState.AppointmentFetched(alignedAppointment)
            } catch (e: Exception) {
                EditDriverLicenseState.Failure
            }

            setState(state)
        }
}
