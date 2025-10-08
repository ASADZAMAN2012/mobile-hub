/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class CheckoutSetStockClickMetric(
    val visitId: Int,
    val currentStock: String,
) : CheckoutMetric(visitId, "SetStockClick") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("currentStock", currentStock)
        }
    }
}
