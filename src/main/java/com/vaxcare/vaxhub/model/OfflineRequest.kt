/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.time.LocalDateTime

/**
 * OfflineRequest serves as the database representation of a request that was made while offline.
 * Once back online, the OfflineRequestWorker will attempt to replay each OfflineRequest found
 * in the OfflineRequest database
 *
 * @property id
 * @property requestUri
 * @property requestMethod
 * @property requestHeaders
 * @property contentType
 * @property requestBody
 */
@Entity
@JsonClass(generateAdapter = true)
data class OfflineRequest(
    @PrimaryKey val id: Int,
    val requestUri: String,
    val requestMethod: String,
    val requestHeaders: String,
    val contentType: String,
    val requestBody: String,
    val originalDateTime: LocalDateTime? = null
) {
    companion object {
        // CheckoutAppointment
        const val CHECKOUT_APPT = "/api/patients/appointment/(\\d+)/checkout"

        // UploadMedia
        const val MEDIA = "/api/patients/appointment/media"

        // Patient
        const val PATIENT = "/api/patients/patient/(\\d+)"

        // Abandon
        const val ABANDON_APPT = "/api/patients/appointment/(\\d+)/abandon"

        // Lot Creation
        const val LOT_CREATION = "/api/inventory/lotnumbers"

        // Unordered Dose
        const val UNORDERED_DOSE_REASON = "api/patients/appointment/noCheckoutReason"
    }
}

data class OfflineRequestInfo(
    val id: Int,
    val requestUri: String,
    val bodySize: Int,
    val originalDateTime: LocalDateTime?
)
