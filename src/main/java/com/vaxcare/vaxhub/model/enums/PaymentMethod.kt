/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.enums

import com.squareup.moshi.JsonClass
import com.vaxcare.vaxhub.model.PaymentMode

@JsonClass(generateAdapter = false)
enum class PaymentMethod(val printableName: String, val modeId: Int) {
    InsurancePay("VaxCare Bill", 0),
    PartnerBill("Partner Bill", 1),
    SelfPay("Self Pay", 2),
    Section317("317", -1),
    State("State", -1),
    Vfc("Vfc", -1),
    EmployerPay("Partner Bill", 3),
    NoPay("No Pay", 4);

    /**
     * Parse modeId to PaymentMode
     *
     * @return the corresponding PaymentMode. Default for Med D is InsurancePay
     */
    fun toPaymentMode(): PaymentMode {
        return PaymentMode::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .firstOrNull { it.id == modeId }
            .let {
                when (it) {
                    null -> PaymentMode.InsurancePay
                    else -> it
                }
            }
    }
}
