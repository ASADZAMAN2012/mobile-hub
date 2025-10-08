/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientEditPinLockViewModel @Inject constructor(
    private val patientRepository: AppointmentRepository,
    private val userRepository: UserRepository
) : BaseViewModel() {
    sealed class PinLockState : State {
        data class PinInSuccessNewPatient(val user: User, val pinUsed: String) : PinLockState()

        data class PinInSuccessExistingPatient(
            val user: User,
            val appointment: Appointment?,
            val pinUsed: String
        ) : PinLockState()

        data class PinInSuccessAbort(val user: User, val pinUsed: String) : PinLockState()

        data class PinInSuccessPayment(
            val user: User,
            val data: PatientCollectData,
            val pinUsed: String
        ) : PinLockState()

        data class PinInFailure(val pinUsed: String) : PinLockState()
    }

    fun loginUser(pin: String, data: PatientCollectData) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = userRepository.getUserByPin(pin)?.let { user ->
                if (user.userId != userRepository.activeUserId) {
                    userRepository.storeActiveUserAndCreateNewUserSessionId(user)
                }

                when (data.flow) {
                    NoInsuranceCardFlow.CREATE_PATIENT -> PinLockState.PinInSuccessNewPatient(
                        user,
                        pin
                    )

                    NoInsuranceCardFlow.EDIT_PATIENT,
                    NoInsuranceCardFlow.COPAY_PAYMENT,
                    NoInsuranceCardFlow.CHECKOUT_PAYMENT,
                    NoInsuranceCardFlow.CHECKOUT_PATIENT -> finalize(data, user, pin)

                    else -> PinLockState.PinInSuccessAbort(user, pin)
                }
            } ?: PinLockState.PinInFailure(pin)

            setState(state)
        }
    }

    private suspend fun finalize(
        data: PatientCollectData,
        user: User,
        pin: String
    ) = when (data.flow) {
        NoInsuranceCardFlow.EDIT_PATIENT -> {
            val appointment = patientRepository.getAppointmentByIdAsync(data.appointmentId ?: -1)
            PinLockState.PinInSuccessExistingPatient(user, appointment, pin)
        }

        else -> PinLockState.PinInSuccessPayment(user, data, pin)
    }
}
