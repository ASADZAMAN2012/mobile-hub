/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.appointment

enum class PhoneContactConsentStatus(val value: String) {
    NOT_APPLICABLE("NotApplicable"),
    ACCEPTED("Accepted"),
    DECLINED("Declined");

    companion object {
        fun fromBoolean(value: Boolean) =
            if (value) {
                ACCEPTED
            } else {
                DECLINED
            }

        fun fromValue(value: String?) =
            when (value) {
                "Accepted" -> ACCEPTED
                "Declined" -> DECLINED
                else -> NOT_APPLICABLE
            }
    }
}

enum class PhoneContactReasons(val value: String) {
    DO_NOT_CONTACT("DoNotContact"),
    COPAY("Copay"),
    SELF_PAY("SelfPay"),
    INSURANCE_CARD("InsuranceCard")
}
