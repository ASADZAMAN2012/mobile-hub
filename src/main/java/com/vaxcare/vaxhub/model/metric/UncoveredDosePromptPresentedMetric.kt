/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

data class UncoveredDosePromptPresentedMetric(
    val visitId: Int,
    val userSelection: String
) : CheckoutMetric(visitId, "UncoveredDosePromptPresented") {
    enum class UserSelection(val displayName: String) {
        PARTNER_BILL("PartnerBill"),
        SELF_PAY("SelfPay"),
        REMOVE_DOSE("RemoveDose")
    }

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("userSelection", userSelection)
        }
    }
}
