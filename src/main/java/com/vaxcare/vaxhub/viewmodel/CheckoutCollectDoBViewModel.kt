/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.patient.DemographicField
import com.vaxcare.vaxhub.repository.AppointmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class CheckoutCollectDoBViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) : BaseViewModel() {
    lateinit var appointmentLiveData: LiveData<Appointment?>

    sealed class CollectDobState : State {
        object ErrorGettingAppointment : CheckoutCollectDoBViewModel.CollectDobState()

        data class AppointmentLoaded(val appointment: Appointment) :
            CheckoutCollectDoBViewModel.CollectDobState()

        object UpdatePatientSubmitted : CheckoutCollectDoBViewModel.CollectDobState()

        object TimeOutReached : CheckoutCollectDoBViewModel.CollectDobState()

        object UpdateFailed : CheckoutCollectDoBViewModel.CollectDobState()

        object AppointmentAbandoned : CheckoutCollectDoBViewModel.CollectDobState()
    }

    private val tenSeconds = 10000L

    fun fetchAppointment(appointmentId: Int) {
        setState(LoadingState)
        viewModelScope.launch(Dispatchers.IO) {
            val appointment = appointmentRepository.getAppointmentByIdAsync(appointmentId)
            appointment?.let {
                setState(CollectDobState.AppointmentLoaded(it))
            } ?: CheckoutPatientViewModel.CheckoutPatientState.ErrorGettingAppointment
        }
        awaitAppointmentUpdate(appointmentId)
    }

    fun updatePatientData(appointment: Appointment?, dob: LocalDate?) {
        viewModelScope.launch(Dispatchers.IO) {
            appointment?.let { appointment ->
                dob?.let {
                    try {
                        appointmentRepository.patchPatient(
                            patientId = appointment.patient.id,
                            fields = listOf(
                                DemographicField.DateOfBirth(
                                    it.format(
                                        DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                    )
                                )
                            ),
                            appointmentId = appointment.id
                        )
                        setState(CollectDobState.UpdatePatientSubmitted)

                        // Notify fragment if no firebase message is received after delay
                        delay(tenSeconds)
                        setState(CollectDobState.TimeOutReached)
                    } catch (exception: Exception) {
                        Timber.e(exception, "Error updating DoB for appointment: ${appointment.id}")
                        setState(CollectDobState.UpdateFailed)
                    }
                }
            }
        }
    }

    fun abandonAppointment(appointmentId: Int) =
        viewModelScope.launch {
            setState(LoadingState)
            appointmentRepository.abandonAppointment(appointmentId)
            setState(CollectDobState.AppointmentAbandoned)
        }

    private fun awaitAppointmentUpdate(appointmentId: Int) {
        appointmentLiveData = appointmentRepository.getAppointmentLiveDataById(appointmentId)
    }
}
