/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

data class CheckoutPinMetric(
    val isSuccess: Boolean
) : CheckoutMetric(null, "PIN") {
    private val pinResult = if (isSuccess) "Success" else "Fail"

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("PINResult", pinResult)
        }
    }
}
