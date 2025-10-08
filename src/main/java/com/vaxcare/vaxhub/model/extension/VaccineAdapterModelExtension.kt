/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.model.extension

import android.content.Context
import android.text.SpannableStringBuilder
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import java.math.BigDecimal

fun VaccineAdapterProductDto.getIssues(context: Context): CharSequence {
    val issueSpannable = SpannableStringBuilder()
    if (doseExpired) {
        // Expired Vaccine
        issueSpannable.append(context.getString(R.string.scanned_dose_expired_header))
            .append("\n")
    }
    if (isWrongStock) {
        issueSpannable.append(context.getString(R.string.wrong_stock))
            .append("\n")
    }
    if (ageIndicated) {
        // Out of Age Indication
        issueSpannable.append(context.getString(R.string.scanned_dose_age_issue_header))
            .append("\n")
    }
    if (isRestrictedProduct) {
        issueSpannable.append(context.getString(R.string.scanned_dose_not_flu_header))
            .append("\n")
    }
    return issueSpannable.trim()
}

fun List<VaccineAdapterProductDto>.amount(): BigDecimal {
    return filterNot { it.isRemoveDoseState() }
        .mapNotNull {
            when (it.paymentMode ?: it.appointmentPaymentMethod.toPaymentMode()) {
                PaymentMode.PartnerBill -> when {
                    it.hasCopay(ignoreTotal = true) -> it.copay?.copay
                    it.paymentModeReason == PaymentModeReason.SelfPayOptOut -> it.oneTouch?.selfPayRate
                    else -> BigDecimal.ZERO
                }

                PaymentMode.SelfPay -> it.oneTouch?.selfPayRate
                else -> it.copay?.copay
            }
        }.fold(BigDecimal(0)) { acc, i -> acc.plus(i) }
}

fun List<VaccineAdapterProductDto>.isMultiplePaymentMode(): Boolean = this.groupBy { it.paymentMode }.size > 1
