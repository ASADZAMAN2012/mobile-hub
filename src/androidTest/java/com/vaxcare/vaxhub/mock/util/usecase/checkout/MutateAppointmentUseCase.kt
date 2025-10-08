/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.mock.util.usecase.checkout

import android.util.Log
import com.squareup.moshi.Moshi
import com.vaxcare.vaxhub.di.MobileMoshi
import com.vaxcare.vaxhub.mock.model.CheckoutSession
import com.vaxcare.vaxhub.model.AppointmentDetailDto
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MutateAppointmentUseCase @Inject constructor(
    @MobileMoshi private val moshi: Moshi
) {
    /**
     * Changes appointment detail to have the same appointmentId, appointmentTime,
     * patient first name and patient last name with a given CheckoutSession
     *
     * @param checkoutSession current checkout session
     * @param responseBody body from mocked response
     * @return CheckoutSession with the appropriate appointmentDateTime and appointmentPayload
     */
    operator fun invoke(checkoutSession: CheckoutSession, responseBody: String?): CheckoutSession =
        responseBody?.let {
            val appt = moshi.adapter(AppointmentDetailDto::class.java).fromJson(it)
            val mutated = appt?.copy(
                id = checkoutSession.appointmentId ?: 0,
                appointmentTime = LocalDateTime.now()
                    .withHour(appt.appointmentTime.hour)
                    .withMinute(appt.appointmentTime.minute)
                    .withSecond(appt.appointmentTime.second),
                patient = appt.patient.copy(
                    firstName = checkoutSession.patientFirstName ?: "",
                    lastName = checkoutSession.patientLastName ?: "",
                    paymentInformation = appt.patient.paymentInformation?.copy(
                        insuredFirstName = checkoutSession.patientFirstName ?: "",
                        insuredLastName = checkoutSession.patientLastName ?: ""
                    )
                )
            )

            Log.d(
                "MOCK",
                "mutate appointment: id: ${mutated?.id} / apptTime: ${mutated?.appointmentTime} "
            )
            checkoutSession.copy(
                appointmentDateTime = mutated?.appointmentTime
                    ?: checkoutSession.appointmentDateTime,
                appointmentDetailPayload = moshi.adapter(AppointmentDetailDto::class.java)
                    .toJson(mutated)
            )
        } ?: checkoutSession
}
