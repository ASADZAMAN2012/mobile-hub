/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.model.enums.PregnancyDurationOptions

data class OutOfAgeWarningMetric(
    val visitId: Int,
    val productName: String,
    val userSelection: PregnancyDurationOptions
) : CheckoutMetric(visitId, "OutOfAgeWarningPromptPresented") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("productName", productName)
            put("userSelection", userSelection.toString())
        }
    }
}
