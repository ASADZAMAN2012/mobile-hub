/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.core.annotation.LocalTime
import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import com.vaxcare.vaxhub.model.enums.AppointmentChangeType
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
data class AppointmentChangedEvent(
    @Json(name = "PartnerId") val partnerId: Int,
    @Json(name = "ParentClinicId") val parentClinicId: Int,
    @Json(name = "ClinicId") val clinicId: Int,
    @Json(name = "ChangeType") val changeType: AppointmentChangeType,
    @Json(name = "ChangeReason") val changeReason: AppointmentChangeReason?,
    @Json(name = "AppointmentId") val appointmentId: Int,
    @LocalTime @Json(name = "VisitDate") val visitDate: LocalDateTime
)
