/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class CheckoutLoginMetric(
    val isSuccess: Boolean,
    val targetNavigation: String,
    appointmentId: Int?
) : CheckoutMetric(appointmentId, "Authenticate") {
    private val pinResult = if (isSuccess) "Success" else "Fail"

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("PINResult", pinResult)
            put("targetNavigation", targetNavigation)
        }
    }
}
