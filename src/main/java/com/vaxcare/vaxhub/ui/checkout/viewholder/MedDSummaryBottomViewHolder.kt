/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.formatAmount
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.view.card.CardBrand
import com.vaxcare.vaxhub.core.view.card.CardUtils
import com.vaxcare.vaxhub.databinding.RvMedDSummaryBottomBinding
import com.vaxcare.vaxhub.model.PaymentInformationRequestBody
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.extension.amount
import com.vaxcare.vaxhub.ui.checkout.ChangePaymentMethodMode

class MedDSummaryBottomViewHolder(
    val binding: RvMedDSummaryBottomBinding,
    private val listener: OnPaymentMethodModeChangeListener,
) : RecyclerView.ViewHolder(binding.root) {
    interface OnPaymentMethodModeChangeListener {
        fun onChanged(paymentMethodMode: ChangePaymentMethodMode)
    }

    fun bind(items: MutableList<VaccineAdapterProductDto>, paymentInformation: PaymentInformationRequestBody?) {
        with(itemView) {
            val cardNumber = paymentInformation?.cardNumber
            val isOnFile = paymentInformation?.isOnFile ?: false
            val phoneNumber = paymentInformation?.phoneNumber
            val sum = items.amount()
            binding.medDSummaryCopayTotalLayout.show()
            binding.medDSummaryCopaySubtotal.text =
                context.getString(
                    R.string.med_d_check_copay_double,
                    sum.formatAmount(2)
                )

            when {
                !phoneNumber.isNullOrEmpty() && cardNumber.isNullOrEmpty() -> {
                    binding.medDSummaryPhoneLayout.show()
                    binding.medDSummaryCashLayout.invisible()
                    binding.medDSummaryValidOnFileLayout.invisible()
                    binding.medDSummaryCopayCardLayout.invisible()
                    binding.medDSummaryCopayCardInfo.text =
                        context.resources.getString(R.string.med_d_summary_copay_with_phone)
                    binding.medDSummaryPhoneEdit.setOnSingleClickListener {
                        listener.onChanged(ChangePaymentMethodMode.CASH_OR_CHECK)
                    }
                }
                cardNumber.isNullOrEmpty() -> {
                    binding.medDSummaryCashLayout.show()
                    binding.medDSummaryPhoneLayout.invisible()
                    binding.medDSummaryValidOnFileLayout.invisible()
                    binding.medDSummaryCopayCardLayout.invisible()
                    binding.medDSummaryCopayCardInfo.text =
                        context.resources.getString(R.string.med_d_summary_copay_without_card)
                    binding.medDSummaryCopayTotal.text =
                        context.resources.getString(R.string.med_d_checkout_vaccine_copay_total)
                    binding.medDSummaryCashEdit.setOnSingleClickListener {
                        listener.onChanged(ChangePaymentMethodMode.COLLECT_CREDIT_CARD)
                    }
                }
                isOnFile -> {
                    // there is a valid card already on file
                    // display text “Valid Credit Card On File” below the “Total to be charged” text
                    binding.medDSummaryValidOnFileLayout.show()
                    binding.medDSummaryCashLayout.invisible()
                    binding.medDSummaryPhoneLayout.invisible()
                    binding.medDSummaryCopayCardLayout.invisible()
                }
                else -> {
                    binding.medDSummaryValidOnFileLayout.invisible()
                    binding.medDSummaryCashLayout.invisible()
                    binding.medDSummaryPhoneLayout.invisible()
                    binding.medDSummaryCopayCardLayout.show()
                    when {
                        items.any { it.oneTouch != null } -> {
                            binding.medDSummaryCopayCardInfo.text =
                                context.resources.getString(R.string.self_pay_one_touch_checkout_with_card)
                        }
                        items.any { it.copay != null } -> {
                            binding.medDSummaryCopayCardInfo.text =
                                context.resources.getString(R.string.med_d_summary_copay_with_card)
                        }
                        else -> {
                            binding.medDSummaryCopayCardInfo.hide()
                        }
                    }
                    binding.medDSummaryCopayTotal.text =
                        context.resources.getString(R.string.med_d_checkout_vaccine_copay_total)
                    CardUtils.getPossibleCardBrand(cardNumber).let { cardBrand ->
                        when (cardBrand) {
                            CardBrand.Visa -> {
                                binding.medDSummaryCopayCardIcon.show()
                                binding.medDSummaryCopayCardIcon.setImageResource(R.drawable.ic_icon_visa)
                            }
                            CardBrand.MasterCard -> {
                                binding.medDSummaryCopayCardIcon.show()
                                binding.medDSummaryCopayCardIcon.setImageResource(R.drawable.ic_icon_mastercard)
                            }
                            CardBrand.AmericanExpress -> {
                                binding.medDSummaryCopayCardIcon.show()
                                binding.medDSummaryCopayCardIcon.setImageResource(R.drawable.ic_icon_amex)
                            }
                            CardBrand.Discover -> {
                                binding.medDSummaryCopayCardIcon.show()
                                binding.medDSummaryCopayCardIcon.setImageResource(R.drawable.ic_icon_discover)
                            }
                            else -> {
                                binding.medDSummaryCopayCardIcon.hide()
                            }
                        }
                        binding.medDSummaryCopayCardBrand.text =
                            context.getString(
                                R.string.med_d_summary_copay_card_brand,
                                cardBrand.type
                            )
                        binding.medDSummaryCopayCardNumber.text =
                            cardNumber.substring(cardNumber.length - 4)
                    }
                    binding.medDSummaryCardEdit.setOnSingleClickListener {
                        listener.onChanged(ChangePaymentMethodMode.CASH_OR_CHECK)
                    }
                }
            }
        }
    }
}
