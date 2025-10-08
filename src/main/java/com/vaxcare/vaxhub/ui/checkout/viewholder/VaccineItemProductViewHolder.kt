/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.core.ui.extension.underlined
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.getSuffix
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.removeLined
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.setUnderlinedText
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.strikeThroughLined
import com.vaxcare.vaxhub.databinding.RvCheckoutVaccineItemBinding
import com.vaxcare.vaxhub.model.DoseState
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.enums.DeleteActionType
import com.vaxcare.vaxhub.model.extension.getIssues
import com.vaxcare.vaxhub.ui.checkout.CheckoutPatientSwipeHelper.SwipeState
import com.vaxcare.vaxhub.ui.checkout.adapter.VaccineItemOptionsListener

class VaccineItemProductViewHolder(
    val binding: RvCheckoutVaccineItemBinding,
    private val listener: VaccineItemOptionsListener
) : RecyclerView.ViewHolder(binding.root) {
    lateinit var vaccineAdapterProductDto: VaccineAdapterProductDto

    var swipeState: SwipeState = SwipeState.IDLE

    private val whiteColor = ContextCompat.getColor(
        itemView.context,
        R.color.primary_white
    )

    private val vaccineIssueColor = ContextCompat.getColor(
        itemView.context,
        R.color.checkout_vaccine_issue_bg
    )

    companion object {
        private val listOfPaymentsElections = listOf(
            PaymentMode.SelfPay,
            PaymentMode.PartnerBill
        )
    }

    fun bind(vaccineAdapterProductDto: VaccineAdapterProductDto) {
        this.vaccineAdapterProductDto = vaccineAdapterProductDto

        with(itemView) {
            if (vaccineAdapterProductDto.swipeReset) {
                vaccineAdapterProductDto.swipeReset = false
                binding.viewForeground.translationX = 0f
                swipeState = SwipeState.IDLE
            }

            binding.checkoutVaccineIcon.setImageResource(vaccineAdapterProductDto.product.getProductIcon())
            binding.checkoutVaccineName.text =
                vaccineAdapterProductDto.product.displaySpannableForCheckout(context)
            binding.checkoutVaccineLotNumber.text = vaccineAdapterProductDto.lotNumber

            binding.checkoutVaccineSite.text = vaccineAdapterProductDto.site?.truncatedName
                ?: context.resources.getString(R.string.patient_checkout_set_site)

            binding.checkoutVaccineSite.setOnClickListener {
                listener.onSiteClicked(vaccineAdapterProductDto)
            }

            binding.checkoutVaccineTips.apply {
                val vaccineMarkCondition = vaccineAdapterProductDto.vaccineMarkCondition
                val isNotInsurancePay = PaymentMode.PartnerBill in listOf(
                    vaccineAdapterProductDto.paymentMode,
                    vaccineAdapterProductDto.appointmentPaymentMethod.toPaymentMode()
                ) || PaymentMode.SelfPay in listOf(
                    vaccineAdapterProductDto.paymentMode,
                    vaccineAdapterProductDto.appointmentPaymentMethod.toPaymentMode()
                )
                when {
                    vaccineMarkCondition != null && isNotInsurancePay -> {
                        isVisible = true
                        setText(vaccineMarkCondition.medDTitleResId)
                    }

                    vaccineAdapterProductDto.hasCopay() -> {
                        isVisible = true
                        text = resources.getString(R.string.med_d_checkout_vaccine_copay_required)
                    }

                    else -> isVisible = false
                }
            }

            val paymentOverridden =
                (
                    listOfPaymentsElections.contains(
                        vaccineAdapterProductDto.paymentMode
                            ?: vaccineAdapterProductDto.appointmentPaymentMethod.toPaymentMode()
                    )
                ) && vaccineAdapterProductDto.vaccineMarkCondition != null

            if (vaccineAdapterProductDto.hasDisplayIssues && !paymentOverridden) {
                binding.viewForeground.setBackgroundColor(vaccineIssueColor)
                binding.checkoutVaccineIssues.text = vaccineAdapterProductDto.getIssues(context)
                binding.checkoutVaccineIssues.show()
                binding.checkoutDoseSeries.hide()
                binding.checkoutVaccineRoute.hide()
            } else {
                binding.viewForeground.setBackgroundColor(whiteColor)
                binding.checkoutVaccineIssues.isVisible = false
            }

            binding.checkoutDoseSeries.isVisible = vaccineAdapterProductDto.dosesInSeries > 1

            when {
                vaccineAdapterProductDto.dosesInSeries > 1 -> setupDosesInSeries(
                    vaccineAdapterProductDto
                )

                vaccineAdapterProductDto.needsRouteSelection() -> setUpVaccineRouteButton(
                    vaccineAdapterProductDto
                )

                else -> binding.pipeContainer.hide()
            }

            binding.viewForeground.translationX = 0f

            when (vaccineAdapterProductDto.doseState) {
                DoseState.ADDED, DoseState.ADMINISTERED -> {
                    binding.buttonDelete.apply {
                        show()
                        setOnSingleClickListener {
                            listener.onDeletionAttempt(
                                product = vaccineAdapterProductDto,
                                deleteActionType = DeleteActionType.TRASH_CAN
                            )
                        }
                    }
                    binding.viewBackground.setBackgroundResource(R.drawable.bg_rounded_checkout_item)
                    binding.checkoutVaccineDelete.setImageResource(R.drawable.ic_close_white)
                    // Add under line
                    switchStrikeThroughLined(false)

                    binding.checkoutVaccineSite.underlined()
                    binding.checkoutVaccineSite.isEnabled = true
                    binding.checkoutVaccineRoute.isEnabled = true
                }

                DoseState.ADMINISTERED_REMOVED, DoseState.REMOVED -> {
                    binding.buttonDelete.hide()
                    binding.viewBackground.setBackgroundResource(R.drawable.bg_rounded_checkout_restore_item)
                    binding.checkoutVaccineDelete.setImageResource(R.drawable.ic_add)

                    // Add strikeThroughLined
                    switchStrikeThroughLined(true)

                    binding.checkoutVaccineSite.isEnabled = false
                    binding.checkoutVaccineRoute.isEnabled = false
                    // Appears only during EDIT  of a past edited checkout
                    binding.viewForeground.setBackgroundColor(whiteColor)
                    binding.checkoutVaccineTips.hide()
                    binding.checkoutVaccineIssues.hide()
                }

                else -> switchStrikeThroughLined()
            }
        }
    }

    private fun View.setupDosesInSeries(vaccineAdapterProductDto: VaccineAdapterProductDto) {
        binding.pipeContainer.show()
        binding.checkoutDoseSeries.show()
        binding.checkoutVaccineRoute.hide()

        val prefix =
            "${vaccineAdapterProductDto.doseSeries}${vaccineAdapterProductDto.doseSeries?.getSuffix()}"
        val doseSeriesTxt = context.formatString(R.string.dose_series_fmt, prefix)
        binding.checkoutDoseSeries.setUnderlinedText(doseSeriesTxt)
        binding.checkoutDoseSeries.setOnClickListener {
            onCheckoutDoseSeries()
        }
    }

    private fun switchStrikeThroughLined(show: Boolean = false) {
        if (show) {
            // Add strikeThroughLined
            binding.checkoutVaccineName.strikeThroughLined()
            binding.checkoutVaccineLotNumber.strikeThroughLined()
            binding.checkoutVaccineSite.strikeThroughLined()
            binding.checkoutDoseSeries.strikeThroughLined()
            binding.checkoutVaccineRoute.strikeThroughLined()
        } else {
            // Remove strikeThroughLined
            binding.checkoutVaccineName.removeLined()
            binding.checkoutVaccineLotNumber.removeLined()
            binding.checkoutVaccineSite.removeLined()
            binding.checkoutDoseSeries.removeLined()
            binding.checkoutVaccineRoute.removeLined()
        }
    }

    private fun setUpVaccineRouteButton(vaccineAdapterProduct: VaccineAdapterProductDto) {
        binding.pipeContainer.show()
        binding.checkoutDoseSeries.hide()
        binding.checkoutVaccineRoute.show()
        val selectedRoute = vaccineAdapterProduct.product.routeCode.toString()
        binding.checkoutVaccineRoute.setUnderlinedText(selectedRoute)
        binding.checkoutVaccineRoute.setOnClickListener {
            listener.onRouteClicked(vaccineAdapterProduct)
        }
    }

    private fun onCheckoutDoseSeries() = listener.onCheckoutDoseSeries(vaccineAdapterProductDto)

    fun onFullSwipe(deleteActionType: DeleteActionType) =
        listener.onDeletionAttempt(
            product = vaccineAdapterProductDto,
            deleteActionType = deleteActionType
        )

    fun onActionDelete() = listener.onSwipeActionClicked(vaccineAdapterProductDto)
}
