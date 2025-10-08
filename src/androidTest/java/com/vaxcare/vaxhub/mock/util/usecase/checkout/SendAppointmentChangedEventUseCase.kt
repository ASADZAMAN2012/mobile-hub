/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.mock.util.usecase.checkout

import android.util.Log
import com.squareup.moshi.Moshi
import com.vaxcare.vaxhub.core.constant.FirebaseEventTypes
import com.vaxcare.vaxhub.di.MobileMoshi
import com.vaxcare.vaxhub.model.AppointmentChangedEvent
import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import com.vaxcare.vaxhub.model.enums.AppointmentChangeType
import com.vaxcare.vaxhub.worker.JobSelector
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendAppointmentChangedEventUseCase @Inject constructor(
    @MobileMoshi private val moshi: Moshi,
    private val jobSelector: JobSelector
) {
    /**
     * Mock an AppointmentChangedEvent (ACE) from Firebase
     *
     * @param clinicId id of the current target clinic
     * @param appointmentTime time of appointment
     */
    operator fun invoke(
        clinicId: Long,
        appointmentTime: LocalDateTime,
        appointmentId: Int = 0,
        changeReason: AppointmentChangeReason = AppointmentChangeReason.RiskUpdated
    ) {
        Log.d("MOCK", "Sending ACE for $clinicId | $appointmentTime | ${changeReason.name}")
        jobSelector.queueJob(
            eventType = FirebaseEventTypes.SYNC_APPOINTMENT,
            payload = moshi.adapter(AppointmentChangedEvent::class.java).toJson(
                generateACE(
                    clinicId = clinicId,
                    appointmentTime = appointmentTime,
                    changeReason = changeReason,
                    appointmentId = appointmentId
                )
            )
        )
    }

    private fun generateACE(
        clinicId: Long,
        appointmentTime: LocalDateTime,
        changeReason: AppointmentChangeReason,
        appointmentId: Int = 0
    ) = AppointmentChangedEvent(
        partnerId = 0,
        clinicId = clinicId.toInt(),
        parentClinicId = clinicId.toInt(),
        changeType = AppointmentChangeType.Updated,
        changeReason = changeReason,
        appointmentId = appointmentId,
        visitDate = appointmentTime
    )
}
