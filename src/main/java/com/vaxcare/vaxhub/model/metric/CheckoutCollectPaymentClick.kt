/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.core.extension.formatAmount
import java.math.BigDecimal

class CheckoutCollectPaymentClick(
    private val totalToCollect: BigDecimal,
    patientVisitId: Int
) : CheckoutMetric(patientVisitId, "CollectPaymentClick") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("totalToCollect", totalToCollect.formatAmount(2).toString())
        }
    }
}
