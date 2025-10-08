/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.toLocalTimeString
import com.vaxcare.vaxhub.core.extension.toOrdinalDate
import com.vaxcare.vaxhub.databinding.RvPatientSearchResultItemBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.EligibilityUiOptions
import com.vaxcare.vaxhub.ui.checkout.adapter.PatientSearchResultAdapter
import com.vaxcare.vaxhub.ui.checkout.extensions.setEligibilityIcon
import java.time.LocalDate
import java.util.regex.Pattern

class PatientSearchResultViewHolder(
    private val type: PatientSearchResultAdapter.AddPatientType,
    val binding: RvPatientSearchResultItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        keyword: String,
        patientSearchWrapper: PatientSearchResultAdapter.PatientSearchWrapper,
        onStartCheckout: ((appointment: Appointment) -> Unit)? = null
    ) {
        with(itemView) {
            setOnClickListener(null)
            isClickable = false
            binding.eligibilityIcon.hide()
            patientSearchWrapper.appointment?.let {
                setViewEligibilityIcon(it)
            }
            setPatientNameAndHighlight(patientSearchWrapper, keyword)
            setIdAndHighlight(patientSearchWrapper, keyword)

            context?.let { ctx ->
                if (patientSearchWrapper.patient.doB != null) {
                    binding.patientDob.text = ctx.formatString(
                        R.string.patient_lookup_dob_display,
                        patientSearchWrapper.patient.getDobString()!!
                    )
                }
            }

            binding.patientDob.text = patientSearchWrapper.patient.getDobString()

            patientSearchWrapper.appointment?.appointmentTime?.let { appointmentTime ->

                val today = LocalDate.now()
                val prefixResId = when {
                    appointmentTime.toLocalDate() == today -> R.string.patient_lookup_today
                    appointmentTime.toLocalDate() == today.plusDays(1) -> R.string.patient_lookup_tomorrow
                    appointmentTime.toLocalDate() == today.minusDays(1) -> R.string.patient_lookup_yesterday
                    else -> -1
                }
                val text =
                    if (prefixResId != -1) {
                        context!!.formatString(
                            R.string.patient_lookup_display_fmt,
                            context!!.getString(prefixResId),
                            appointmentTime.toLocalTimeString().lowercase()
                        )
                    } else {
                        appointmentTime.toOrdinalDate(" ")
                    }

                binding.appointmentTime.text = text
                binding.appointmentTime.visibility = View.VISIBLE
                binding.lotRowPointlessBtn.isGone = true
                if (type == PatientSearchResultAdapter.AddPatientType.SEARCH) {
                    setOnSingleClickListener {
                        onStartCheckout?.invoke(patientSearchWrapper.appointment)
                    }
                }
            } ?: run {
                binding.appointmentTime.visibility = View.GONE
                binding.lotRowPointlessBtn.isGone = true
            }
        }
    }

    private fun View.setPatientNameAndHighlight(
        patientWrapper: PatientSearchResultAdapter.PatientSearchWrapper,
        keyword: String
    ) {
        context?.let { ctx ->
            val patientName = ctx.formatString(
                R.string.patient_name_display,
                patientWrapper.patient.firstName.captureName(),
                patientWrapper.patient.lastName.captureName()
            )
            val spannableString = SpannableString(patientName)
            val pattern = Pattern.compile(keyword.lowercase())
            val matcher = pattern.matcher(patientName.lowercase())
            spannableString.setSpan(
                StyleSpan(Typeface.NORMAL),
                0,
                patientName.length,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                spannableString.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                )
            }
            binding.patientName.text = spannableString
        }
    }

    private fun View.setIdAndHighlight(
        patientWrapper: PatientSearchResultAdapter.PatientSearchWrapper,
        keyword: String
    ) {
        val id = if (patientWrapper.patient.originatorId.isNullOrEmpty()) {
            patientWrapper.patient.id.toString()
        } else {
            patientWrapper.patient.originatorId
        }
        val spannableString = SpannableString(id)
        val pattern = Pattern.compile(keyword.lowercase())
        val matcher = pattern.matcher(id)
        spannableString.setSpan(
            StyleSpan(Typeface.NORMAL),
            0,
            id.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }
        binding.patientOriginatorId.text = spannableString
    }

    private fun View.setViewEligibilityIcon(appointment: Appointment) {
        binding.eligibilityIcon.setEligibilityIcon(
            options = EligibilityUiOptions(
                appointment = appointment
            )
        )

        if (appointment.checkedOut) {
            binding.container.setBackgroundResource(R.drawable.bg_rounded_corner_purple)
        } else {
            binding.container.setBackgroundResource(R.drawable.bg_rounded_white_ripple_item)
        }

        val colorPrimaryWhite = ContextCompat.getColor(context, R.color.primary_white)
        val colorPrimaryBlack = ContextCompat.getColor(context, R.color.primary_black)
        val textColor = if (appointment.checkedOut) colorPrimaryWhite else colorPrimaryBlack
        binding.patientName.setTextColor(textColor)
        binding.patientOriginatorIdLabel.setTextColor(textColor)
        binding.patientOriginatorId.setTextColor(textColor)
        binding.patientDobLabel.setTextColor(textColor)
        binding.patientDob.setTextColor(textColor)
        binding.appointmentTime.setTextColor(textColor)
    }
}
