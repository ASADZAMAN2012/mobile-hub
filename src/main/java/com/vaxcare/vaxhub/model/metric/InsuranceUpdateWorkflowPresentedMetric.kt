/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

data class InsuranceUpdateWorkflowPresentedMetric(
    val visitId: Int?,
    val payerChanged: Boolean,
    val insuranceCardRequested: Boolean,
    val noInsuranceCardSelected: Boolean,
    val cleanImageCaptureValidationPresented: Boolean
) : CheckoutMetric(visitId, "InsuranceUpdateWorkflowPresented") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("payerChanged", payerChanged.toString())
            put("insuranceCardRequested", insuranceCardRequested.toString())
            put("noInsuranceCardSelected", noInsuranceCardSelected.toString())
            put(
                "cleanImageCaptureValidationPresented",
                cleanImageCaptureValidationPresented.toString()
            )
        }
    }
}
