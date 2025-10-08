/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.core.report.model.checkout.RelativeDoS
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.enums.RiskFactor

data class CheckoutFinishMetric(
    val visitId: Int,
    val doseCount: Int,
    val paymentMethod: PaymentMethod,
    val duration: Long,
    val result: CheckoutResult,
    val missingInfoCaptured: Boolean,
    val networkStatus: NetworkStatus,
    val relativeDoS: RelativeDoS,
    val paymentType: String,
    val isCheckedOut: Boolean,
    val showedRiskFree: Boolean = false,
    val riskFactors: List<RiskFactor> = emptyList(),
) : CheckoutMetric(visitId, "Finish") {
    enum class CheckoutResult {
        SUBMITTED,
        ERROR,
        ABANDONED
    }

    enum class PaymentModeType(val displayName: String) {
        INSURANCE_PAY("InsurancePay"),
        PARTNER_BILL("PartnerBill"),
        SELF_PAY("SelfPay"),
        UNKNOWN("Unknown")
    }

    private val paymentMode = when (paymentMethod) {
        PaymentMethod.InsurancePay -> PaymentModeType.INSURANCE_PAY
        PaymentMethod.PartnerBill -> PaymentModeType.PARTNER_BILL
        PaymentMethod.SelfPay -> PaymentModeType.SELF_PAY
        else -> PaymentModeType.UNKNOWN
    }

    private val connectivityError = when (networkStatus) {
        NetworkStatus.CONNECTED -> "None"
        NetworkStatus.CONNECTED_NO_INTERNET, NetworkStatus.DISCONNECTED -> "WiFi Offline"
        NetworkStatus.CONNECTED_VAXCARE_UNREACHABLE -> "System Offline"
    }

    private val riskFlags: String?
        get() = if (riskFactors.isNotEmpty()) {
            riskFactors.joinToString { it.name }
        } else {
            null
        }

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("result", result.name)
            put("isCheckedOut", isCheckedOut.toString())
            riskFlags?.let { put("riskFlags", it) }
            put("doseCount", doseCount.toString())
            put("paymentMode", paymentMode.displayName)
            put("duration", duration.toString())
            put("connectivityError", connectivityError)
            put("missingInfoCaptured", if (missingInfoCaptured) "Captured" else "N/A")
            put("relativeDoS", relativeDoS.name)
            put("paymentType", paymentType)
            put("showedRiskFree", showedRiskFree.toString())
        }
    }
}
