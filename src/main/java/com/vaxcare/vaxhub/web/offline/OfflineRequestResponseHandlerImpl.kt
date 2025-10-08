/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.web.offline

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.ABANDON_APPT
import com.vaxcare.vaxhub.model.OfflineRequest.Companion.LOT_CREATION
import com.vaxcare.vaxhub.model.inventory.LotNumber
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LotNumbersRepository
import okhttp3.Response

class OfflineRequestResponseHandlerImpl(
    private val moshi: Moshi,
    private val appointmentRepository: AppointmentRepository,
    private val lotNumbersRepository: LotNumbersRepository
) : OfflineRequestResponseHandler {
    /**
     * Perform any processing of response bodies after successful retry of offline request
     *
     * @param response The response body from the successful retry
     */
    override suspend fun handleResponse(response: Response) {
        if (response.isSuccessful) {
            val requestUri = response.request.url.toUri().toASCIIString()
            when {
                requestUri.contains(Regex(ABANDON_APPT)) -> {
                    val appointmentId = "\\d+".toRegex().find(requestUri)?.value
                    appointmentId?.let { id ->
                        val ids = listOf(id.toInt())
                        // Delete local appointments
                        appointmentRepository.deleteAppointmentByIds(ids)

                        // Delete HubMetaData
                        appointmentRepository.deleteAppointmentHubMetaDataByIds(ids)
                    }
                }

                requestUri.contains(Regex(LOT_CREATION)) -> {
                    val type = Types.newParameterizedType(List::class.java, LotNumber::class.java)
                    val jsonAdapter: JsonAdapter<List<LotNumber>> = moshi.adapter(type)
                    val lotNumberList = response.body?.source()?.let { jsonAdapter.fromJson(it) }
                    lotNumberList?.let {
                        lotNumbersRepository.insertAll(it)
                    }
                }

                else -> Unit
            }
        }
    }
}
