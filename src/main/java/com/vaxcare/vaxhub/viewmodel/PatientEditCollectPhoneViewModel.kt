/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import androidx.lifecycle.viewModelScope
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.util.PhoneUtils.disassemblePhone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class PatientEditCollectPhoneViewModel @Inject constructor(
    private val repo: AppointmentRepository,
    private val locationRepository: LocationRepository
) : BaseViewModel() {
    sealed class CollectPhoneState : State {
        data class InitialState(val isSessionLocked: Boolean) : CollectPhoneState()

        data class PhoneNumberFetched(
            val isSessionLocked: Boolean,
            val area: String,
            val prefix: String,
            val line: String
        ) : CollectPhoneState()

        object PhoneNumberProvided : CollectPhoneState()

        object PhoneNumberNotValid : CollectPhoneState()
    }

    fun setPhoneHint(phoneNumber: String?, appointmentId: Int?) =
        viewModelScope.launch(Dispatchers.IO) {
            val prefillPhoneAllowed = locationRepository
                .getFeatureFlagByConstant(FeatureFlagConstant.FeaturePrefillPhone) != null
            val isSessionLocked = locationRepository
                .getFeatureFlagByConstant(FeatureFlagConstant.FeatureHubLoginUserAndPin) != null

            val phone = when {
                prefillPhoneAllowed &&
                    appointmentId != null &&
                    phoneNumber == null -> getPhoneFromAppointment(appointmentId)

                prefillPhoneAllowed -> phoneNumber
                else -> null
            }

            val rawPhone = phone?.replace("-", "") ?: ""
            if (validate(rawPhone)) {
                disassemblePhone(rawPhone)?.let { phoneParts ->
                    setState(
                        CollectPhoneState.PhoneNumberFetched(
                            isSessionLocked,
                            phoneParts[0],
                            phoneParts[1],
                            phoneParts[2]
                        )
                    )
                }
            } else {
                setState(CollectPhoneState.InitialState(isSessionLocked))
            }
        }

    fun validateAndUpdatePhone(
        area: String,
        prefix: String,
        line: String
    ) = when {
        validate(area, prefix, line) -> setState(CollectPhoneState.PhoneNumberProvided)
        else -> setState(CollectPhoneState.PhoneNumberNotValid)
    }

    private fun validate(rawPhone: String): Boolean =
        rawPhone.length == 10 && validate(
            rawPhone.substring(0, 3),
            rawPhone.substring(3, 6),
            rawPhone.substring(6)
        )

    private suspend fun getPhoneFromAppointment(appointmentId: Int): String? {
        val appointment = repo.getAppointmentByIdAsync(appointmentId)
        return appointment?.patient?.phoneNumber
    }

    private fun validate(
        area: String,
        prefix: String,
        line: String
    ): Boolean = Pattern.compile("\\d{3}\\d{3}\\d{4}").matcher("$area$prefix$line").matches()
}
