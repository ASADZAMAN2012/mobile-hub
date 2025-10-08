/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.model.enums.AppointmentChangeReason
import com.vaxcare.vaxhub.model.enums.AppointmentChangeType

class AceReceivedMetric(
    private val changeReason: AppointmentChangeReason,
    private val changeType: AppointmentChangeType?,
    patientVisitId: Int
) : CheckoutMedDMetric(patientVisitId, "AceReceived") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("changeReason", changeReason.name)
            put("changeType", changeType?.name ?: "Not received")
        }
    }
}
