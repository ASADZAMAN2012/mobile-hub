/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

data class DuplicateRSVMetric(
    val visitId: Int,
    val userSelection: String
) : CheckoutMetric(visitId, "DuplicateRSVPrompt") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("userSelection", userSelection)
        }
    }
}
