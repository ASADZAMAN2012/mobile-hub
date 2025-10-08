/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.RvAppointmentItemListViewBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.ui.checkout.adapter.AppointmentListFlags
import com.vaxcare.vaxhub.ui.checkout.extensions.getRiskIconColor
import com.vaxcare.vaxhub.ui.checkout.extensions.vaccineSupplyColor
import com.vaxcare.vaxhub.ui.checkout.extensions.vaccineSupplyFadedColor

class AppointmentListItemViewHolder(
    val binding: RvAppointmentItemListViewBinding,
    private val listener: AppointmentClickListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(appointment: Appointment, flags: AppointmentListFlags) {
        setPatientDemographicInfo(appointment)

        setRiskIcon(appointment)

        setBackgroundAndTextColorByCheckedOut(appointment)

        showMedD(appointment)

        showOrderCount(appointment, flags)

        showNoOrderTag(appointment, flags)

        alignTagContainer()

        binding.clickArea.setOnSingleClickListener {
            listener.onItemClicked(
                appointment,
                flags
            )
        }
    }

    private fun showMedD(appointment: Appointment) {
        binding.medDTag.isVisible = appointment.isMedDTagShown()
    }

    private fun showNoOrderTag(appointment: Appointment, flags: AppointmentListFlags) {
        val count = appointment.nonExpiredOrdersCount

        if (count == 0 && !appointment.checkedOut && flags.isPublicStock) {
            binding.inventoryType.show()
            // show the private, vcf ...
            binding.inventoryType.text = appointment.vaccineSupplyString()

            // background
            val tintColor = ContextCompat.getColor(
                itemView.context,
                appointment.vaccineSupplyFadedColor()
            )
            binding.inventoryType.backgroundTintList = ColorStateList.valueOf(tintColor)

            // text color
            val textColor = ContextCompat.getColor(
                itemView.context,
                appointment.vaccineSupplyColor()
            )
            binding.inventoryType.setTextColor(textColor)
        } else {
            binding.inventoryType.hide()
        }
    }

    /**
     * Match the visibility of the tag container with its children.
     */
    private fun alignTagContainer() =
        with(binding) {
            tagContainer.isInvisible =
                medDTag.isGone && ordersCount.isGone && inventoryType.isGone
        }

    private fun showOrderCount(appointment: Appointment, flags: AppointmentListFlags) {
        val count = appointment.nonExpiredOrdersCount

        if (flags.isRprd && count > 0 && !appointment.checkedOut) {
            binding.ordersCount.show()
            binding.ordersCount.text = itemView.resources.getQuantityString(
                R.plurals.number_of_doses,
                count, count
            )

            // set color
            val colorRes = appointment.vaccineSupplyColor()

            val tintColor = ContextCompat.getColor(itemView.context, colorRes)
            binding.ordersCount.backgroundTintList = ColorStateList.valueOf(tintColor)
        } else {
            binding.ordersCount.hide()
        }
    }

    private fun setRiskIcon(appointment: Appointment) {
        when {
            (appointment.encounterState?.vaccineMessage != null) -> {
                appointment.encounterState?.vaccineMessage?.getIconRes()?.let {
                    binding.eligibilityIcon.apply {
                        setImageResource(it)
                        imageTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(
                                itemView.context,
                                appointment.getRiskIconColor()
                            )
                        )
                        show()
                    }
                } ?: binding.eligibilityIcon.invisible()
            }

            else -> {
                binding.eligibilityIcon.invisible()
            }
        }
    }

    private fun setPatientDemographicInfo(appointment: Appointment) {
        with(binding) {
            patientName.isGone = true
            patientFirstName.isVisible = true
            patientLastName.isVisible = true

            patientName.text = itemView.context.formatString(
                R.string.patient_name_display,
                appointment.patient.firstName.captureName(),
                appointment.patient.lastName.captureName()
            )

            val lastnameDisplay = "${appointment.patient.lastName.captureName()},"
            patientLastName.text = lastnameDisplay
            patientFirstName.text = appointment.patient.firstName.captureName()

            if (appointment.patient.originatorPatientId?.isNotEmpty() != null) {
                patientOriginatorId.text = appointment.patient.originatorPatientId
            } else {
                patientOriginatorId.text = appointment.patient.id.toString()
            }

            patientDob.text = appointment.patient.getDobString() ?: ""
        }
    }

    private fun setBackgroundAndTextColorByCheckedOut(appointment: Appointment) =
        with(binding.checkedOutBg) {
            backgroundTintList = if (appointment.checkedOut) {
                setBackgroundResource(R.drawable.bg_rounded_corner_purple)
                val colorRes = appointment.vaccineSupplyFadedColor()
                val tintColor = ContextCompat.getColor(context, colorRes)
                ColorStateList.valueOf(tintColor)
            } else {
                setBackgroundResource(R.drawable.bg_rounded_corner_white)
                val colorPrimaryWhite =
                    ContextCompat.getColor(itemView.context, R.color.primary_white)

                ColorStateList.valueOf(colorPrimaryWhite)
            }

            val colorPrimaryBlack = ContextCompat.getColor(itemView.context, R.color.primary_black)
            binding.patientOriginatorIdLabel.setTextColor(colorPrimaryBlack)
            binding.patientOriginatorId.setTextColor(colorPrimaryBlack)
            binding.patientDobLabel.setTextColor(colorPrimaryBlack)
            binding.patientDob.setTextColor(colorPrimaryBlack)
            binding.eligibilityBg.setBackgroundResource(R.color.transparent)
        }
}
