/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class StockChangeRetryPromptShownMetric(
    private val userChoice: String,
    private val waitInMillis: Long,
    visitId: Int
) : CheckoutMetric(visitId, "StockErrorRetryPrompt") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("Presented", "true")
            put("UsersChoice", userChoice)
            put("TimeWaiting", waitInMillis.toString())
        }
    }
}
