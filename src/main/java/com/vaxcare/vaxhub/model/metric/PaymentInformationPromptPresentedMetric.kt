/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

data class PaymentInformationPromptPresentedMetric(
    val visitId: Int,
    val PaymentInformationPromptTrigger: String,
    val PatientPaymentMethodSelected: String
) : CheckoutMetric(visitId, "PaymentInformationPromptPresented") {
    enum class PaymentInfoTrigger(val displayName: String) {
        MEDD("MedD"),
        SELF_PAY("SelfPay")
    }

    enum class PaymentMethodSelect(val displayName: String) {
        CREDIT_CARD("CreditCard"),
        CASH_CHECK("CashCheck")
    }

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("PaymentInformationPromptTrigger", PaymentInformationPromptTrigger)
            put("PatientPaymentMethodSelected", PatientPaymentMethodSelected)
        }
    }
}
