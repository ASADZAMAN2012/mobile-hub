/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.core.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.getLayoutInflater
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.databinding.PatientInfoDrawerBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.checkout.MedDInfo
import com.vaxcare.vaxhub.model.checkout.hasCovered
import com.vaxcare.vaxhub.model.checkout.isError
import com.vaxcare.vaxhub.model.checkout.isNotCovered
import com.vaxcare.vaxhub.model.enums.MedDVaccines
import com.vaxcare.vaxhub.ui.checkout.extensions.getRiskIconColor
import com.vaxcare.vaxhub.ui.checkout.extensions.setEligibilityIcon
import com.vaxcare.vaxhub.ui.checkout.extensions.vaccineSupplyColor
import com.vaxcare.vaxhub.ui.checkout.extensions.vaccineSupplyFadedColor
import java.time.LocalDate

class PatientInfoDrawer(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs) {
    var onRunCopayCheck: () -> Unit = {}
    private val binding: PatientInfoDrawerBinding =
        PatientInfoDrawerBinding.inflate(context.getLayoutInflater(), this, true)

    private var isAppointmentCheckedOut: Boolean? = null

    fun setMedDData(medDInfo: MedDInfo?) {
        binding.apply {
            meddMessagingContainer.show()
            runCopayBtn.isVisible = medDInfo == null
            when {
                medDInfo.isError() -> {
                    binding.medDUnavailableText.setItalicTextWithBottomAnchor(
                        textResId = R.string.med_d_unable_to_retrieve,
                        bottomAnchor = LayoutParams.UNSET
                    )
                }

                else -> setCopays(medDInfo)
            }
        }
    }

    fun setAppointmentData(appointment: Appointment) {
        isAppointmentCheckedOut = appointment.checkedOut
        binding.apply {
            patientName.apply {
                text = context.formatString(
                    R.string.patient_name_display,
                    appointment.patient.firstName.captureName(),
                    appointment.patient.lastName.captureName()
                )
                visibility = View.VISIBLE
            }

            appointment.encounterState?.let { encounterState ->
                eligibilityText.text = encounterState.vaccinePrimaryMessage
                    ?: context.getString(R.string.eligibility_unavailable)
                encounterState.vaccineMessage?.getIconRes()?.let { icon ->
                    binding.eligibilityIcon.setEligibilityIcon(
                        icon = icon,
                        color = appointment.getRiskIconColor()
                    )
                }
            }

            setMedDTagsAndEligibilityCta(appointment)
            payerText.apply {
                text = appointment.patient.paymentInformation?.insuranceName
                    ?: context.getString(R.string.patient_add_select_payer_uninsured)
                isGone = text.isBlank()
            }
            responsibilityText.apply {
                text = appointment.paymentMethod.printableName
                visibility = View.VISIBLE
            }
        }
    }

    private fun setCopays(medDInfo: MedDInfo?) {
        val copays = medDInfo?.copays
        val hideCopayValues = medDInfo == null || copays.isNullOrEmpty() || medDInfo.isNotCovered()
        if (hideCopayValues) {
            binding.hideCopayValuesText()
        } else {
            binding.invisibleCopayValuesText()
        }

        when {
            medDInfo.hasCovered() -> {
                binding.medDUnavailableText.hide()
                copays?.forEach {
                    val displayText = it.getDisplayString(resources::getString)
                    when (it.antigen) {
                        MedDVaccines.RSV -> {
                            binding.rsvCopay.show()
                            binding.rsvCopayValue.show()
                            binding.rsvCopayValue.text = displayText
                        }

                        MedDVaccines.TDAP -> {
                            binding.tdapCopay.show()
                            binding.tdapCopayValue.show()
                            binding.tdapCopayValue.text = displayText
                        }

                        MedDVaccines.ZOSTER -> {
                            binding.zosterCopay.show()
                            binding.zosterCopayValue.show()
                            binding.zosterCopayValue.text = displayText
                        }

                        else -> Unit
                    }
                }
            }

            medDInfo.isNotCovered() -> binding.medDUnavailableText.setItalicTextWithBottomAnchor(
                textResId = R.string.med_d_check_patient_not_eligible,
                bottomAnchor = LayoutParams.UNSET
            )

            medDInfo.isError() -> binding.medDUnavailableText.setItalicTextWithBottomAnchor(
                textResId = R.string.med_d_unable_to_retrieve,
                bottomAnchor = LayoutParams.UNSET
            )
        }
    }

    /**
     * Display the stock tag of the associated vaccine supply from the appointment.
     */
    fun displayAppointmentStock(appointment: Appointment) {
        binding.tagContainer.isVisible = true
        binding.textviewInventoryType.apply {
            val (tintColor, textColor) = ContextCompat.getColor(
                context,
                appointment.vaccineSupplyFadedColor()
            ) to ContextCompat.getColor(
                context,
                appointment.vaccineSupplyColor()
            )
            text = appointment.vaccineSupplyString()
            backgroundTintList = ColorStateList.valueOf(tintColor)
            setTextColor(textColor)
            isGone = appointment.isMedDTagShown()
        }
    }

    private fun PatientInfoDrawerBinding.setMedDTagsAndEligibilityCta(appointment: Appointment) {
        val isAppointmentMedD = appointment.isMedDTagShown()
        when {
            isAppointmentCheckedOut == true -> hideCtasAndMedDText()
            isAppointmentMedD -> {
                medDTag.show()
                medDCta.show()
                val medDCtaText = appointment.encounterState?.medDMessage?.secondaryMessage
                val isNotDateOfService =
                    appointment.appointmentTime.toLocalDate() != LocalDate.now()
                if (isNotDateOfService) {
                    binding.medDCta.text = context.getString(R.string.med_d_not_today)
                    binding.runCopayBtn.hide()
                } else {
                    binding.medDCta.text = medDCtaText
                    val buttonText =
                        SpannableString(context.getString(R.string.run_copay_check))
                    buttonText.setSpan(UnderlineSpan(), 0, buttonText.length, 0)
                    binding.runCopayBtn.apply {
                        text = buttonText
                        setOnSingleClickListener { onRunCopayCheck() }
                    }
                }
            }
        }
        tagContainer.isVisible = tagContainer.isVisible || isAppointmentMedD == true
        if (appointment.checkedOut) {
            hideCtasAndMedDText()
        }

        eligibilityCta.text = appointment.encounterState?.vaccineSecondaryMessage
    }

    private fun PatientInfoDrawerBinding.hideCopayValuesText() {
        rsvCopay.hide()
        rsvCopayValue.hide()
        tdapCopay.hide()
        tdapCopayValue.hide()
        zosterCopay.hide()
        zosterCopayValue.hide()
    }

    private fun PatientInfoDrawerBinding.invisibleCopayValuesText() {
        rsvCopay.invisible()
        rsvCopayValue.invisible()
        tdapCopay.invisible()
        tdapCopayValue.invisible()
        zosterCopay.invisible()
        zosterCopayValue.invisible()
    }

    private fun PatientInfoDrawerBinding.hideCtasAndMedDText() {
        eligibilityCta.hide()
        medDCta.hide()
        runCopayBtn.hide()
        medDUnavailableText.hide()
    }

    private fun TextView.setItalicTextWithBottomAnchor(textResId: Int, bottomAnchor: Int = LayoutParams.PARENT_ID) {
        val textSpannable =
            SpannableString(context.getString(textResId))
        textSpannable.setSpan(
            StyleSpan(Typeface.ITALIC),
            0,
            textSpannable.length,
            0
        )
        text = textSpannable
        (layoutParams as? LayoutParams)?.bottomToBottom = bottomAnchor
        show()
    }
}
