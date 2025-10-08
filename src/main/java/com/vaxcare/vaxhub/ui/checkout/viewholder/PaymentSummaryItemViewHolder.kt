/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import android.annotation.SuppressLint
import android.view.View
import androidx.core.content.ContextCompat
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.removeLined
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.strikeThroughLined
import com.vaxcare.vaxhub.databinding.RvMedDSummaryItemBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.DoseState
import com.vaxcare.vaxhub.model.PaymentInformationRequestBody
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.appointment.AppointmentStatus
import java.math.BigDecimal
import java.math.RoundingMode

class PaymentSummaryItemViewHolder(
    binding: RvMedDSummaryItemBinding,
    private val appointment: Appointment,
    private val paymentInformation: PaymentInformationRequestBody?
) : BaseVaccineSummaryItemViewHolder<RvMedDSummaryItemBinding>(binding) {
    @SuppressLint("StringFormatMatches")
    override fun setupUI(item: VaccineAdapterProductDto) {
        with(binding) {
            val context = itemView.context
            val resources = itemView.resources
            val cardNumber = paymentInformation?.cardNumber
            val phoneCollected = paymentInformation?.phoneNumber
            medDSummaryVaccineIcon.show()
            medDSummaryVaccineLotNumber.show()
            medDSummaryVaccineCopayValue.show()

            medDSummaryVaccineIcon.setImageResource(item.product.getProductIcon())
            medDSummaryVaccineName.text =
                item.product.displaySpannableForCheckout(context)
            medDSummaryVaccineLotNumber.text = item.lotNumber
            val paymentMode = item.paymentMode ?: item.appointmentPaymentMethod.toPaymentMode()

            when (paymentMode) {
                PaymentMode.SelfPay -> {
                    binding.medDSummaryVaccineCopayLayout.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.primary_pink)
                    )
                    binding.medDSummaryVaccineCopayLayout.show()
                    val copayValue =
                        item.oneTouch?.selfPayRate?.setScale(
                            2,
                            RoundingMode.UP
                        )?.toDouble()
                    binding.medDSummaryVaccineCopayValue.text = if (copayValue != null) {
                        context.getString(R.string.med_d_check_copay_double, copayValue)
                    } else {
                        context.getString(R.string.med_d_summary_copay_error)
                    }

                    medDSummaryVaccineCopayTitle.setText(R.string.patient_checkout_base_exclusion_neutral)
                }

                PaymentMode.InsurancePay -> {
                    val copay = item.copay
                    if (copay != null && copay.copay > BigDecimal.ZERO) {
                        binding.medDSummaryVaccineCopayLayout.show()
                        binding.medDSummaryVaccineCopayValue.text = context.getString(
                            R.string.med_d_check_copay_double,
                            copay.copay.setScale(2, RoundingMode.UP).toDouble()
                        )

                        if (cardNumber.isNullOrEmpty()) {
                            binding.medDSummaryVaccineCopayTitle.text =
                                resources.getString(
                                    if (phoneCollected.isNullOrEmpty()) {
                                        R.string.med_d_summary_copay_cash_or_check
                                    } else {
                                        R.string.med_d_summary_copay_phone
                                    }
                                )
                        } else {
                            binding.medDSummaryVaccineCopayTitle.text =
                                resources.getString(R.string.med_d_summary_copay)
                        }
                    } else {
                        binding.medDSummaryVaccineCopayLayout.hide()
                    }
                }

                PaymentMode.PartnerBill -> {
                    binding.medDSummaryVaccineCopayLayout.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.primary_light_cyan)
                    )
                    binding.medDSummaryVaccineCopayLayout.show()

                    medDSummaryVaccineCopayTitle.setText(R.string.patient_checkout_base_exclusion_continue)
                    if (item.paymentModeReason == PaymentModeReason.SelfPayOptOut) {
                        val copayValue =
                            item.oneTouch?.selfPayRate?.setScale(
                                2,
                                RoundingMode.UP
                            )?.toDouble()
                        binding.medDSummaryVaccineCopayValue.text = if (copayValue != null) {
                            context.getString(R.string.med_d_check_copay_double, copayValue)
                        } else {
                            context.getString(R.string.med_d_summary_copay_error)
                        }
                    } else {
                        item.copay?.copay?.let { copay ->
                            val copayValue = copay.setScale(2, RoundingMode.UP)?.toDouble()
                            binding.medDSummaryVaccineCopayValue.text =
                                context.getString(R.string.med_d_check_copay_double, copayValue)
                            binding.medDSummaryVaccineCopayTitle.text =
                                resources.getString(R.string.med_d_summary_copay)
                        } ?: run {
                            binding.medDSummaryVaccineCopayTitle.text =
                                context.formatString(R.string.checkout_summary_partner_bill_fmt, "")
                            binding.medDSummaryVaccineCopayValue.hide()
                        }
                    }
                }

                else -> {
                    binding.medDSummaryVaccineCopayLayout.hide()
                }
            }

            val spaceHeight =
                if (binding.medDSummaryVaccineCopayLayout.visibility == View.VISIBLE) {
                    R.dimen.dp_15
                } else {
                    R.dimen.dp_30
                }
            binding.medDSummaryVaccineBottomSpace.layoutParams =
                binding.medDSummaryVaccineBottomSpace.layoutParams.apply {
                    height = resources.getDimensionPixelOffset(spaceHeight)
                }

            if (PaymentMode.SelfPay !in listOf(
                    item.paymentMode,
                    item.appointmentPaymentMethod.toPaymentMode()
                )
            ) {
                when (appointment.encounterState?.vaccineMessage?.status) {
                    AppointmentStatus.SELF_PAY -> {
                        if (cardNumber == null) {
                            binding.medDSummaryVaccineCopayLayout.setBackgroundColor(
                                ContextCompat.getColor(context, R.color.primary_light_cyan)
                            )
                        } else {
                            binding.medDSummaryVaccineCopayLayout.setBackgroundColor(
                                ContextCompat.getColor(context, R.color.primary_pink)
                            )
                        }
                    }

                    AppointmentStatus.PARTNER_BILL -> {
                        binding.medDSummaryVaccineCopayLayout.setBackgroundColor(
                            ContextCompat.getColor(context, R.color.primary_light_cyan)
                        )
                    }

                    else -> Unit
                }
            }

            // Remove strikeThroughLined
            binding.medDSummaryVaccineName.removeLined()
            binding.medDSummaryVaccineLotNumber.removeLined()
            when (item.doseState) {
                DoseState.ADDED, DoseState.ADMINISTERED -> Unit
                DoseState.ADMINISTERED_REMOVED, DoseState.REMOVED -> {
                    binding.medDSummaryVaccineCopayLayout.hide()
                    // Add strikeThroughLined
                    binding.medDSummaryVaccineName.strikeThroughLined()
                    binding.medDSummaryVaccineLotNumber.strikeThroughLined()
                    binding.medDSummaryVaccineIssues.hide()
                }

                else -> Unit
            }

            medDSummaryVaccineCopayLayout.setBackgroundColor(
                ContextCompat.getColor(context, R.color.list_blue)
            )
        }
    }
}
