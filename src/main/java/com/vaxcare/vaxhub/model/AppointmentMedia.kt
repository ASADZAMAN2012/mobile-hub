/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppointmentMedia(
    val appointmentId: Int,
    val mediaType: String,
    val contentType: String,
    val encodedBytes: String
)
