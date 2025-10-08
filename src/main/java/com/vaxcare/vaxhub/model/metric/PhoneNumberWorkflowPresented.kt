/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

/**
 * Metric specific for the PhoneNumberCollection FF
 *
 * @property phoneNumberPrefilled - The phoneNumber was prefilled
 * @property phoneNumberUpdated - The phoneNumber was updated
 * @property phoneContactAgreement - The phoneNumber agree button was tapped
 * @property phoneNumberContext - the flow prompted during the insurance flow or the payment flow
 */
data class PhoneNumberWorkflowPresented(
    var visitId: Int?,
    val phoneNumberPrefilled: Boolean,
    var phoneNumberUpdated: Boolean,
    var phoneContactAgreement: Boolean,
    var phoneNumberContext: String,
    val phoneFlowDurationSeconds: Int
) : CheckoutMetric(visitId, "PhoneNumberWorkflowPresented") {
    companion object {
        const val PHONE_COLLECTION_WORKFLOW = "PhoneCollectionWorkflow"
    }

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("phoneNumberPrefilled", phoneNumberPrefilled.toString())
            put("phoneNumberUpdated", phoneNumberUpdated.toString())
            put("phoneContactAgreement", phoneContactAgreement.toString())
            put("phoneNumberContext", phoneNumberContext)
            put("phoneFlowDurationSeconds", phoneFlowDurationSeconds.toString())
        }
    }
}
