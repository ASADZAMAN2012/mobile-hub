/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.model.AppointmentMediaType

data class UploadMediaMetric(
    val appointmentId: Int,
    val success: Boolean,
    val mediaType: AppointmentMediaType
) : CheckoutMetric(appointmentId, "UploadMedia") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().toMutableMap().apply {
            put("success", "$success")
            put("mediaType", mediaType.value)
        }
    }
}
