/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.viewmodel.MedDCheckViewModel

data class MedDStateMetric(
    val visitId: Int,
    val state: MedDCheckViewModel.MedDCheckState?
) : CheckoutMetric(visitId, "MedD.State") {
    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            if (state is MedDCheckViewModel.MedDCheckState.CopayResponse) {
                put("medDCheckResponse", state.medDInfo.toString())
            }
            put("state", state?.javaClass?.simpleName ?: state.toString())
        }
    }
}
