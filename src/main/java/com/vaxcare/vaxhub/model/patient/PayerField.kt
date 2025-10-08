/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.patient

import kotlinx.parcelize.Parcelize

/**
 * Fields for Payer information updates
 *
 */
sealed class PayerField : InfoField {
    @Parcelize
    data class PayerName(
        override var currentValue: String? = null,
        val selectedInsuranceId: Int? = null
    ) : PayerField() {
        override fun getPatchPath(): String = InfoType.INSURANCEID_PATH
    }

    @Parcelize
    data class MemberId(
        override var currentValue: String? = null
    ) : PayerField() {
        override fun getPatchPath(): String = InfoType.MEMBERID_PATH
    }

    @Parcelize
    data class GroupId(
        override var currentValue: String? = null
    ) : PayerField() {
        override fun getPatchPath(): String = InfoType.GROUPID_PATH
    }

    @Parcelize
    data class PlanId(
        override var currentValue: String? = null
    ) : PayerField() {
        override fun getPatchPath(): String = InfoType.PLANID_PATH
    }

    @Parcelize
    data class PortalMappingId(
        override var currentValue: String? = null
    ) : PayerField() {
        override fun getPatchPath(): String = InfoType.PORTALMAPPINGID_PATH
    }

    @Parcelize
    data class Stock(
        override var currentValue: String? = null
    ) : PayerField() {
        override fun getPatchPath(): String = InfoType.STOCK_PATH
    }

    @Parcelize
    data class VfcFinancialClass(
        override var currentValue: String? = null
    ) : PayerField() {
        override fun getPatchPath(): String = InfoType.FINANCIAL_PATH
    }

    companion object {
        /**
         * Returns a list of nulled/empty PayerFields. This is required in the case of the appt
         * having a null paymentInformation member.
         */
        fun emptyPayerFields() = listOf(PayerName(), MemberId(), GroupId(), PlanId(), PortalMappingId())
    }
}
