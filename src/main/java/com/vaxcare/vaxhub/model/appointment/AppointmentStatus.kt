/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.appointment

enum class AppointmentStatus(val id: Int, val display: String) {
    UNDETERMINED(0, "Undetermined"),
    RISK_FREE(1, "RiskFree"),
    AT_RISK_DATA_MISSING(2, "AtRiskDataMissing"),
    AT_RISK_DATA_INCORRECT(3, "AtRiskDataIncorrect"),
    AT_RISK_DATA_COMPLETE(4, "AtRiskDataComplete"),
    PARTNER_BILL(10, "PartnerBill"),
    SELF_PAY(11, "SelfPay"),
    IN_REVIEW(100, "InReview");

    companion object {
        private val map = values().associateBy(AppointmentStatus::display)

        fun fromString(name: String) = map[name] ?: UNDETERMINED
    }
}
