/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class StockChangeTimeOutMetric(
    private val originalStock: String,
    private val originalPaymentMethod: String,
    private val presentedStock: String,
    private val presentedPaymentMethod: String,
    private val waitInMillis: Long,
    visitId: Int
) : CheckoutMetric(visitId, "StockChangeTimeOut") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("originalStock", originalStock)
            put("originalPaymentMethod", originalPaymentMethod)
            put("presentedStock", presentedStock)
            put("presentedPaymentMethod", presentedPaymentMethod)
            put("TimeWaiting", waitInMillis.toString())
        }
    }
}
