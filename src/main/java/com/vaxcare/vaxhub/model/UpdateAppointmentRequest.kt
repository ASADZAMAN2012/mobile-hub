/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.JsonClass
import java.time.LocalDateTime

@JsonClass(generateAdapter = true)
class UpdateAppointmentRequest(
    val clinicId: Long,
    val date: LocalDateTime,
    val providerId: Int,
    val visitType: String
)
