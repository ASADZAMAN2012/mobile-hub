/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class CheckoutStockOptionSelectedMetric(
    private val selectedStock: String,
    private val selectedFinancialClass: String?,
    private val appointmentStock: String,
    private val patientInsurancePrimaryId: Int?,
    private val patientInsurancePlanId: Int?,
    private val activePublicStocks: List<String>,
    visitId: Int
) : CheckoutMetric(visitId, "StockOptionSelected") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("selectedStock", selectedStock)
            selectedFinancialClass?.let { put("selectedFinancialClass", it) }
            put("appointmentStock", appointmentStock)
            patientInsurancePrimaryId?.let { put("patientInsurancePrimaryId", it.toString()) }
            patientInsurancePlanId?.let { put("patientInsurancePrimaryPlanId", it.toString()) }
            put("activePublicStocks", activePublicStocks.joinToString(","))
        }
    }
}
