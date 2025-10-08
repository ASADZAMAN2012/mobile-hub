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
import com.vaxcare.vaxhub.model.SearchPatient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MutatePatientSearchUseCase @Inject constructor(
    @MobileMoshi private val moshi: Moshi
) {
    /**
     * Changes patients in the search patient list to have the same first and last names with a
     * given CheckoutSession
     *
     * @param checkoutSession current checkout session
     * @param responseBody body from mocked response
     * @return CheckoutSession with the appropriate patientSearchPayload
     */
    operator fun invoke(checkoutSession: CheckoutSession, responseBody: String?): CheckoutSession =
        responseBody?.let { body ->
            val adapter: JsonAdapter<List<SearchPatient>> = moshi.adapter(
                Types.newParameterizedType(
                    List::class.java,
                    SearchPatient::class.java
                )
            )
            val list = adapter.fromJson(body)?.toMutableList()
            val newResponse = listOfNotNull(
                list?.firstOrNull()?.copy(
                    firstName = checkoutSession.patientFirstName ?: "",
                    lastName = checkoutSession.patientLastName ?: ""
                )
            )

            Log.d("MOCK", "mutate patientSearch: lastname: ${checkoutSession.patientLastName} ")
            checkoutSession.copy(patientSearchPayload = adapter.toJson(newResponse))
        } ?: checkoutSession
}
