/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.mock.util.usecase.checkout

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.vaxcare.vaxhub.di.MobileMoshi
import com.vaxcare.vaxhub.mock.model.CheckoutSession
import com.vaxcare.vaxhub.model.AppointmentDto
import com.vaxcare.vaxhub.model.CallToAction
import com.vaxcare.vaxhub.model.appointment.AppointmentIcon
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MutateFirstAppointmentListUseCase @Inject constructor(
    @MobileMoshi private val moshi: Moshi
) {
    /**
     * Changes appointments in the appointmentList. The first appointment will have the
     * appointmentId, appointmentTime, patient first name and patient last name
     * of the given CheckoutSession. Subsetquent appointments will have their id incremened.
     *
     * @param checkoutSession current checkout session
     * @param responseBody body from mocked response
     * @return CheckoutSession with the appropriate appointmentListPayload
     */
    operator fun invoke(checkoutSession: CheckoutSession, responseBody: String?): CheckoutSession =
        responseBody?.let {
            val adapter: JsonAdapter<List<AppointmentDto>> = moshi.adapter(
                Types.newParameterizedType(
                    List::class.java,
                    AppointmentDto::class.java,
                    CallToAction::class.java,
                    AppointmentIcon::class.java,
                    AppointmentStatus::class.java
                )
            )

            val appts = adapter.fromJson(it)
            val mutated = mutableListOf<AppointmentDto>()
            appts?.forEachIndexed { index, appt ->
                mutated.add(
                    appt.copy(
                        id = (checkoutSession.appointmentId ?: 0) + index,
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
                )
            }

            Log.d("MOCK", "mutated list: $mutated")
            checkoutSession.copy(
                appointmentListPayload = adapter.toJson(mutated)
            )
        } ?: checkoutSession
}
