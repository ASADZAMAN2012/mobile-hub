/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class ForcedRiskType(val value: Int) {
    Undefined(0),
    RiskFree(1),
    AtRiskDataMissing(2),
    AtRiskDataIncorrect(3),
    AtRiskDataComplete(4),
    PartnerBill(10),
    SelfPay(11)
}
