/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class DuplicateProductBannerMetric(
    visitId: Int,
    val lotNumber: String,
    val productId: Int,
    val salesProductId: Int
) : CheckoutMetric(visitId, "DuplicateProductBannerPresented") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("lotNumber", lotNumber)
            put("productId", productId.toString())
            put("salesProductId", salesProductId.toString())
        }
    }
}
