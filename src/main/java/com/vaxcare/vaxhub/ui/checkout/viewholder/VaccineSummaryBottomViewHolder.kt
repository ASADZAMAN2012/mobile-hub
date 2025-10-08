/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.formatAmount
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.RvCheckoutVaccineSummaryBottomBinding
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.extension.amount

class VaccineSummaryBottomViewHolder(val binding: RvCheckoutVaccineSummaryBottomBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(lotNumberWithProduct: List<VaccineAdapterProductDto>) {
        with(itemView) {
            val sum = lotNumberWithProduct.amount()
            show()
            binding.checkoutVaccineCopayTotal.text = context.getString(
                R.string.med_d_check_copay_double,
                sum.formatAmount(2)
            )
            when {
                lotNumberWithProduct.any { it.requiresPayment() } -> binding.footerMessage.show()
                lotNumberWithProduct.any { it.hasCopay(true) } -> {
                    binding.footerMessage.setText(R.string.nurse_disclaimer_medd_signature)
                    binding.footerMessage.show()
                }
            }
        }
    }

    private fun VaccineAdapterProductDto.requiresPayment(): Boolean =
        hasCopay() || isSelfPayDose() || paymentModeReason == PaymentModeReason.SelfPayOptOut
}
