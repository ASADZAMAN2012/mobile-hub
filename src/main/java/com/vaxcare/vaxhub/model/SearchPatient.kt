/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

@JsonClass(generateAdapter = true)
data class SearchPatient(
    val id: Int,
    val originatorId: String?,
    val firstName: String,
    val lastName: String,
    val doB: String?
) {
    fun getDobString(): String? {
        var dobDisplay: String? = null
        if (doB != null) {
            try {
                val epoch =
                    Instant.ofEpochSecond(LocalDateTime.parse(doB).toEpochSecond(ZoneOffset.UTC))
                        .toEpochMilli()
                dobDisplay = Instant.ofEpochMilli(epoch).atZone(ZoneId.of("UTC")).toLocalDate()
                    .toLocalDateString()
            } catch (ex: DateTimeParseException) {
                Timber.e(ex, "doB: $doB is malformed. Exception: ")
            }
        }

        return dobDisplay
    }

    companion object {
        fun convertPatient(patient: Patient): SearchPatient {
            return SearchPatient(
                id = patient.id,
                originatorId = patient.originatorPatientId,
                firstName = patient.firstName,
                lastName = patient.lastName,
                doB = patient.dob
            )
        }
    }
}
