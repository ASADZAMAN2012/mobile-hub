/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class MedDCopayCheckResultNextClick(private val isMedDEligible: Boolean, patientVisitId: Int) : CheckoutMedDMetric(
    patientVisitId = patientVisitId,
    medDEventName = "CopayCheckResultNext.Click"
) {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("isMedDEligible", isMedDEligible.toString())
        }
    }
}
