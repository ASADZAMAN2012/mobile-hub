/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.mock.util.usecase.checkout

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.vaxcare.vaxhub.di.MobileMoshi
import com.vaxcare.vaxhub.mock.model.CheckoutSession
import com.vaxcare.vaxhub.model.Patient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MutatePatientDetailUseCase @Inject constructor(
    @MobileMoshi private val moshi: Moshi
) {
    /**
     * Changes patient details to have the same patient first name and patient last name with a
     * given CheckoutSession
     *
     * @param checkoutSession current checkout session
     * @param responseBody body from mocked response
     * @return CheckoutSession with the appropriate patientDetailPayload
     */
    operator fun invoke(checkoutSession: CheckoutSession, responseBody: String?): CheckoutSession {
        val adapter: JsonAdapter<Patient> = moshi.adapter(Patient::class.java)
        val incomingPatient = responseBody?.let { adapter.fromJson(it) }
        return incomingPatient?.let { patient ->
            val (newFirstName, newLastName) = (
                checkoutSession.patientFirstName
                    ?: ""
            ) to (checkoutSession.patientLastName ?: "")
            val mutated = patient.copy(
                firstName = newFirstName,
                lastName = newLastName,
                paymentInformation = patient.paymentInformation?.copy(
                    insuredFirstName = newFirstName,
                    insuredLastName = newLastName
                )
            )
            Log.d("MOCK", "mutate patientDetail: lastname: ${mutated.lastName} / raw: $mutated ")
            checkoutSession.copy(patientDetailPayload = adapter.toJson(mutated))
        } ?: checkoutSession
    }
}
