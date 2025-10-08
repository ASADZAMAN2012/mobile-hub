/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.checkout.RelativeDoS
import com.vaxcare.vaxhub.core.extension.toUtc
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import java.time.LocalDateTime

data class CheckoutAppointmentSelectedMetric(
    val visitId: Int,
    val dateOfService: LocalDateTime,
    val relativeDoS: RelativeDoS,
    val stock: String,
    val risk: AppointmentStatus?,
    val ormsPresented: String = ""
) : CheckoutMetric(visitId, "AppointmentSelected") {
    val status = risk?.display ?: ""

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("DOS", dateOfService.toUtc().toString())
            put("RelativeDOS", relativeDoS.name)
            put("Stock", stock)
            put("VaccineRiskPresented", status)
            put("ormsPresented", ormsPresented)
        }
    }
}
