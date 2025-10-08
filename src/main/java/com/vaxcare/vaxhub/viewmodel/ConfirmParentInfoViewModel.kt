/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.repository.AppointmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmParentInfoViewModel @Inject constructor(
    private val appointmentRepository: AppointmentRepository
) : ViewModel() {
    private val _appointment: MutableLiveData<Appointment?> = MutableLiveData<Appointment?>(null)
    val appointment: LiveData<Appointment?> = _appointment

    fun getAppointment(appointmentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val appointment = appointmentRepository.getAppointmentByIdAsync(appointmentId)
            _appointment.postValue(appointment)
        }
    }
}
