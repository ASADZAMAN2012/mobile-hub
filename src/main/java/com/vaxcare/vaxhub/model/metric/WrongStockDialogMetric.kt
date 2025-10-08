/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

class WrongStockDialogMetric(
    val optionSelected: String,
    val appointmentStock: String,
    val lotName: String,
    val lotStocks: List<String>,
    val patientInsurancePrimaryId: Int?,
    val patientInsurancePlanId: Int?,
    val activePublicStocks: List<String>,
    visitId: Int
) : CheckoutMetric(visitId, "WrongStockDialogClick") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("optionSelected", optionSelected)
            put("appointmentStock", appointmentStock)
            put("lotName", lotName)
            put("lotStocks", lotStocks.joinToString(","))
            patientInsurancePrimaryId?.let { put("patientInsurancePrimaryId", it.toString()) }
            patientInsurancePlanId?.let { put("patientInsurancePrimaryPlanId", it.toString()) }
            put("activePublicStocks", activePublicStocks.joinToString(","))
        }
    }
}
