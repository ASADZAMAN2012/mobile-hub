/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.model.enums.DeleteActionType

data class CheckoutProductDeletedMetric(
    val deleteType: DeleteActionType,
    val lotNumber: String,
    val productIssuesCsv: String
) : CheckoutMetric(null, "ProductDeleted") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("deleteType", deleteType.display)
            put("lotNumber", lotNumber)
            put("productIssues", productIssuesCsv)
        }
    }
}
