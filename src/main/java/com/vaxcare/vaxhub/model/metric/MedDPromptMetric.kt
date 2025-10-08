/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

data class MedDPromptMetric(
    val visitId: Int,
    val promptContext: String
) : CheckoutMetric(visitId, "MedD.PromptForCheck") {
    enum class PromptContext(val displayName: String) {
        DURING_CHECKOUT("DuringCheckout")
    }

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("promptContext", promptContext)
        }
    }
}
