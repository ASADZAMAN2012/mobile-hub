/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.core.extension.toUtc
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import java.time.LocalDateTime

data class CheckoutAppointmentMedDStatusMetric(
    val visitId: Int,
    val dateOfService: LocalDateTime,
    val risk: AppointmentStatus?,
    val appointmentChangeReason: String,
    val medDCta: String?,
    val medDRiskAssessmentId: Int
) : CheckoutMetric(visitId, "MedDStatus") {
    val status = risk?.display ?: ""

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("DOS", dateOfService.toUtc().toString())
            put("MedDStatus", status)
            put("AppointmentChangedReason", appointmentChangeReason)
            put("MedDCta", medDCta ?: "null")
            put("MedDRiskAssessmentId", medDRiskAssessmentId.toString())
        }
    }
}
