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
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.toRelativeDayTime
import com.vaxcare.vaxhub.databinding.RvAppointmentSearchListItemBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.ui.checkout.extensions.getRiskIconColor
import com.vaxcare.vaxhub.ui.checkout.extensions.vaccineSupplyColor
import com.vaxcare.vaxhub.ui.checkout.extensions.vaccineSupplyFadedColor

class AppointmentSearchResultViewHolder(
    val binding: RvAppointmentSearchListItemBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        appointment: Appointment,
        isPublicFlag: Boolean,
        onAppointmentSelected: ((appointment: Appointment) -> Unit)? = null
    ) {
        setPatientDemographicInfo(appointment)
        setRiskIcon(appointment)
        setBackgroundAndTextColorByCheckedOut(appointment)
        showMedDTag(appointment)
        showStockTag(appointment, isPublicFlag)
        alignTagContainer()

        binding.clickArea.setOnSingleClickListener {
            onAppointmentSelected?.invoke(appointment)
        }

        binding.appointmentTime.text = appointment.appointmentTime.toRelativeDayTime()
    }

    private fun setPatientDemographicInfo(appointment: Appointment) {
        with(binding) {
            patientLastName.text = "${appointment.patient.lastName.captureName()},"
            patientFirstName.text = appointment.patient.firstName.captureName()

            if (appointment.patient.originatorPatientId?.isNotEmpty() != null) {
                patientOriginatorId.text = appointment.patient.originatorPatientId
            } else {
                patientOriginatorId.text = appointment.patient.id.toString()
            }

            patientDob.text = appointment.patient.getDobString() ?: ""
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

    private fun setBackgroundAndTextColorByCheckedOut(appointment: Appointment) {
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
        }
    }

    private fun showMedDTag(appointment: Appointment) {
        binding.medDTag.isVisible = appointment.isMedDTagShown()
    }

    private fun showStockTag(appointment: Appointment, isPublicFlag: Boolean) {
        val count = appointment.nonExpiredOrdersCount

        if (count == 0 && !appointment.checkedOut && isPublicFlag) {
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

    private fun alignTagContainer() {
        binding.tagContainer.isInvisible = binding.medDTag.isGone && binding.inventoryType.isGone
    }
}
