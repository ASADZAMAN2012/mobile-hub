/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import android.view.View
import androidx.core.content.ContextCompat
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.getSuffix
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.removeLined
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.strikeThroughLined
import com.vaxcare.vaxhub.databinding.RvCheckoutVaccineSummaryItemBinding
import com.vaxcare.vaxhub.model.DoseState
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import timber.log.Timber
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class VaccineSummaryItemViewHolder(
    binding: RvCheckoutVaccineSummaryItemBinding
) : BaseVaccineSummaryItemViewHolder<RvCheckoutVaccineSummaryItemBinding>(binding) {
    override fun setupUI(item: VaccineAdapterProductDto) {
        val resources = itemView.resources
        val context = itemView.context
        binding.checkoutVaccineRoute.show()
        binding.checkoutVaccineRouteLabel.show()
        binding.checkoutVaccineIcon.show()
        binding.checkoutVaccineLotNumber.show()

        // hide OT and VaxCare3 components
        binding.checkoutVaccineSite.hide()
        binding.checkoutVaccineSeries.hide()

        binding.checkoutVaccineIcon.setImageResource(item.product.getProductIcon())
        binding.checkoutVaccineName.text =
            item.product.displaySpannableForCheckout(context)
        binding.checkoutVaccineLotNumber.text = item.lotNumber

        binding.checkoutAgeIndication.text =
            displayAgeIndication(context, item)
        binding.checkoutVisDate.text = item.product.visDates?.let { getVisDate(it) }
            ?: itemView.context.getText(R.string.not_applicable)
        binding.checkoutVaccineRoute.text = item.product.routeCode.toString()

        binding.checkoutVaccineCopayValue.show()
        val spaceHeight = if (binding.checkoutVaccineCopayLayout.visibility == View.VISIBLE) {
            R.dimen.dp_15
        } else {
            R.dimen.dp_30
        }
        binding.checkoutVaccineBottomSpace.layoutParams =
            binding.checkoutVaccineBottomSpace.layoutParams.apply {
                height = resources.getDimensionPixelOffset(spaceHeight)
            }

        // Remove strikeThroughLined
        binding.checkoutVaccineName.removeLined()
        binding.checkoutVaccineLotNumber.removeLined()
        binding.checkoutVisDateLabel.removeLined()
        binding.checkoutVisDate.removeLined()
        binding.checkoutAgeIndicationLabel.removeLined()
        binding.checkoutAgeIndication.removeLined()
        binding.checkoutVaccineRouteLabel.removeLined()
        binding.checkoutVaccineRoute.removeLined()

        when (item.doseState) {
            DoseState.ADDED, DoseState.ADMINISTERED -> Unit
            DoseState.ADMINISTERED_REMOVED, DoseState.REMOVED -> {
                binding.checkoutVaccineCopayLayout.hide()

                // Add strikeThroughLined
                binding.checkoutVaccineName.strikeThroughLined()
                binding.checkoutVaccineLotNumber.strikeThroughLined()
                binding.checkoutVisDateLabel.strikeThroughLined()
                binding.checkoutVisDate.strikeThroughLined()
                binding.checkoutAgeIndicationLabel.strikeThroughLined()
                binding.checkoutAgeIndication.strikeThroughLined()
                binding.checkoutVaccineRouteLabel.strikeThroughLined()
                binding.checkoutVaccineRoute.strikeThroughLined()

                // Appears only during EDIT  of a past edited checkout
                binding.viewContent.setBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.primary_white
                    )
                )
                binding.checkoutVaccineIssues.hide()
            }

            else -> Unit
        }

        with(binding) {
            when (item.paymentMode ?: item.appointmentPaymentMethod.toPaymentMode()) {
                PaymentMode.SelfPay -> {
                    checkoutVaccineCopayLayout.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.primary_pink)
                    )
                    checkoutVaccineCopayLayout.show()
                    checkoutVaccineCopayTitle.setText(R.string.patient_checkout_base_exclusion_neutral)

                    checkoutVaccineCopayValue.show()
                    val selfPayRateValue =
                        item.oneTouch?.selfPayRate?.setScale(
                            2,
                            RoundingMode.UP
                        )?.toDouble()
                    checkoutVaccineCopayValue.text = selfPayRateValue?.let {
                        context.getString(
                            R.string.med_d_check_copay_double,
                            it
                        )
                    } ?: context.getString(R.string.med_d_summary_copay_error)
                }

                PaymentMode.EmployerPay,
                PaymentMode.PartnerBill -> {
                    checkoutVaccineCopayValue.hide()
                    checkoutVaccineCopayLayout.show()
                    checkoutVaccineCopayLayout.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.primary_light_cyan)
                    )

                    item.copay?.let { copay ->
                        checkoutVaccineCopayValue.show()
                        checkoutVaccineCopayTitle.setText(R.string.checkout_summary_copay)
                        @Suppress("StringFormatMatches")
                        checkoutVaccineCopayValue.text = context.getString(
                            R.string.med_d_check_copay_double,
                            copay.copay.setScale(2, RoundingMode.HALF_UP).toDouble()
                        )
                    } ?: run {
                        checkoutVaccineCopayTitle.text =
                            context.formatString(R.string.checkout_summary_partner_bill_fmt, "")
                        if (item.paymentModeReason == PaymentModeReason.SelfPayOptOut) {
                            val selfPayRateValue =
                                item.oneTouch?.selfPayRate?.setScale(2, RoundingMode.UP)
                            checkoutVaccineCopayValue.show()
                            checkoutVaccineCopayValue.text = selfPayRateValue?.let {
                                context.getString(
                                    R.string.med_d_check_copay_double,
                                    it.toDouble()
                                )
                            }
                        }
                    }
                }

                PaymentMode.InsurancePay -> {
                    if (item.hasCopay()) {
                        item.copay?.let { copay ->
                            checkoutVaccineCopayLayout.show()
                            checkoutVaccineCopayLayout.setBackgroundColor(
                                ContextCompat.getColor(context, R.color.primary_light_cyan)
                            )
                            checkoutVaccineCopayValue.show()
                            checkoutVaccineCopayTitle.setText(R.string.checkout_summary_copay)
                            @Suppress("StringFormatMatches")
                            checkoutVaccineCopayValue.text = context.getString(
                                R.string.med_d_check_copay_double,
                                copay.copay.setScale(2, RoundingMode.HALF_UP).toDouble()
                            )
                        }
                    } else {
                        checkoutVaccineCopayLayout.hide()
                    }
                }

                else -> checkoutVaccineCopayLayout.hide()
            }

            checkoutVaccineCopayLayout.setBackgroundColor(
                ContextCompat.getColor(context, R.color.list_blue)
            )

            with(checkoutVaccineSite) {
                hide()
                val siteText = item.site?.truncatedName
                siteText?.let {
                    text = it
                    show()
                }
            }

            checkoutVaccineRouteLabel.hide()
            checkoutVaccineRoute.hide()

            when {
                item.isLegacyCovid() -> {
                    checkoutVaccineRoute.hide()
                    checkoutVaccineRouteLabel.hide()
                    checkoutVaccineSeries.show()
                    val prefix =
                        "${item.doseSeries}${item.doseSeries?.getSuffix()}"
                    val doseSeriesTxt = context.formatString(R.string.dose_series_fmt, prefix)
                    checkoutVaccineSeries.text = doseSeriesTxt
                }

                item.needsRouteSelection() -> {
                    checkoutVaccineRouteLabel.show()
                    checkoutVaccineRoute.show()
                    checkoutVaccineSeries.hide()
                }
            }
        }
    }

    private fun getVisDate(visDate: String): String {
        return try {
            // Split the string
            val visDateParts = visDate.split(",")
            // Take the first VisDate, grab the delimited value
            val formattedDate = visDateParts[0].substringAfter(": ", visDate)
            // This is the formatter used for parsing the data coming in
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            // This is the formatter used for displaying the data to the user
            val display = DateTimeFormatter.ofPattern("MM/dd/yy")
            // And return the formatted string
            LocalDate.parse(formattedDate, formatter).format(display)
        } catch (e: Exception) {
            Timber.e(e, "Could not parse VISDate")
            binding.root.context.getString(R.string.not_applicable)
        }
    }
}
