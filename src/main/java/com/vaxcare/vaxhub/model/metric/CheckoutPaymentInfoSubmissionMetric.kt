/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

data class CheckoutPaymentInfoSubmissionMetric(
    private val visitId: Int,
    private val success: Boolean,
    private val totalAttempts: Int
) : CheckoutMetric(visitId, "PaymentSubmission") {
    private val result = if (success) "Success" else "Fail"

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("result", result)
            put("totalAttempts", totalAttempts.toString())
        }
    }
}
