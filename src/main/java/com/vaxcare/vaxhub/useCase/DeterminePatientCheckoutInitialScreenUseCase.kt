/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.useCase

import com.vaxcare.vaxhub.core.extension.isDoBCaptureDisabled
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.repository.LocationRepository
import timber.log.Timber
import javax.inject.Inject

class DeterminePatientCheckoutInitialScreenUseCase @Inject constructor(
    val locationRepository: LocationRepository
) {
    enum class InitialScreen {
        CHECKOUT_PATIENT,
        DOB_CAPTURE
    }

    suspend operator fun invoke(appointment: Appointment): InitialScreen =
        try {
            if (
                appointment.patient.dob.isNullOrBlank() &&
                !locationRepository.getFeatureFlagsAsync().isDoBCaptureDisabled()
            ) {
                InitialScreen.DOB_CAPTURE
            } else {
                InitialScreen.CHECKOUT_PATIENT
            }
        } catch (e: Exception) {
            Timber.e(e)
            if (appointment.patient.dob.isNullOrBlank()) {
                InitialScreen.DOB_CAPTURE
            } else {
                InitialScreen.CHECKOUT_PATIENT
            }
        }
}
