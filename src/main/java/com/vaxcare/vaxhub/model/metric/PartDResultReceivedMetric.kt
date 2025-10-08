/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class PartDResultReceivedMetric(
    private val totalCopaysReceived: Int,
    private val riskAssessmentId: Int?,
    private val medDCta: String?,
    patientVisitId: Int
) : CheckoutMedDMetric(patientVisitId, "PartDResultReceived") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("totalCopays", totalCopaysReceived.toString())
            riskAssessmentId?.toString()?.let { put("meddCurrentRiskAssessmentId", it) }
            medDCta?.let { put("meddCurrentCta", it) }
        }
    }
}
