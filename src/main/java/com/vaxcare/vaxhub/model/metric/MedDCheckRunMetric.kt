/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.metric

import com.vaxcare.vaxhub.model.checkout.ProductCopayInfo
import java.time.Duration
import java.time.LocalDateTime

data class MedDCheckRunMetric(
    val visitId: Int,
    val checkContext: String,
    val validResultReturned: Boolean,
    val patientMedDCovered: Boolean,
    val resultsUnavailable: Boolean,
    val copays: List<ProductCopayInfo>?,
    val displayedMessage: String?,
    val medDCheckStartedAt: String?,
) : CheckoutMetric(visitId, "MedD.CheckRun") {
    enum class CheckContext(val displayName: String) {
        PRE_CHECKOUT("PreCheckout"),
        DURING_CHECKOUT("DuringCheckout");

        companion object {
            fun fromInt(value: Int) = values()[value]
        }
    }

    private val copayString = when {
        resultsUnavailable -> "CheckUnavailable"
        !validResultReturned -> "CheckError"
        !patientMedDCovered -> "NotCovered"
        else -> copays?.filter { it.isCovered() }.toString()
    }

    val duration = try {
        val start = LocalDateTime.parse(medDCheckStartedAt)
        Duration.between(start, LocalDateTime.now()).seconds
    } catch (e: Exception) {
        null
    }

    override fun toMap(): MutableMap<String, String> {
        return super.toMap().apply {
            put("checkContext", checkContext)
            put("validResultReturned", validResultReturned.toString())
            put("patientMedDCovered", patientMedDCovered.toString())
            put("displayedMessage", displayedMessage ?: "")
            put("medDCheckStartedAt", medDCheckStartedAt ?: "")
            put("medDCheckDuration", duration.toString())
            put("copayCheckResults", copayString)
            if (!copays.isNullOrEmpty()) {
                val resultNdcsAndAntigens =
                    copays.map { "${it.antigen}|${it.coveredProductIds ?: it.ndcCode}" }.toString()
                val resultNdcsAndCopay = copays.map { "${it.coveredProductIds ?: it.ndcCode}|${it.copay}" }.toString()
                val resultNdcsAndStatusCodes =
                    copays.map { "${it.coveredProductIds ?: it.ndcCode}|${it.eligibilityStatusCode}" }.toString()
                put("resultNDCsAndAntigens", resultNdcsAndAntigens)
                put("resultNDCsAndCopay", resultNdcsAndCopay)
                put("resultNDCsAndStatusCodes", resultNdcsAndStatusCodes)
            }
        }
    }
}
