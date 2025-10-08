/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.mock.util.usecase.checkout

import android.util.Log
import com.squareup.moshi.Moshi
import com.vaxcare.vaxhub.di.MobileMoshi
import com.vaxcare.vaxhub.mock.model.CheckoutSession
import com.vaxcare.vaxhub.model.PatientPostBody
import okio.Buffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtractPatientNameAndAppointmentTimeUseCase @Inject constructor(
    @MobileMoshi private val moshi: Moshi
) {
    /**
     * Extracts the patient name and appointmentTime from an outgoing request body
     *
     * @param checkoutSession current checkout session
     * @param requestBody body from request
     * @return CheckoutSession with the appropriate patient name and appointmentDateTime
     */
    operator fun invoke(checkoutSession: CheckoutSession, requestBody: Buffer?): CheckoutSession {
        val postBody = requestBody?.let { moshi.adapter(PatientPostBody::class.java).fromJson(it) }
        Log.d("MOCK", "invoke: extracting :${postBody?.newPatient?.lastName} / ${postBody?.date}")
        return checkoutSession.copy(
            patientFirstName = postBody?.newPatient?.firstName ?: checkoutSession.patientFirstName,
            patientLastName = postBody?.newPatient?.lastName ?: checkoutSession.patientLastName,
            appointmentDateTime = postBody?.date ?: checkoutSession.appointmentDateTime
        )
    }
}
