/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class CheckoutAppointmentRefreshAttemptMetric(patientVisitId: Int, val success: Boolean) : CheckoutMetric(
    patientVisitId = patientVisitId,
    checkoutEventName = "AppointmentRefreshAttempt"
) {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("success", success.toString())
        }
    }
}
