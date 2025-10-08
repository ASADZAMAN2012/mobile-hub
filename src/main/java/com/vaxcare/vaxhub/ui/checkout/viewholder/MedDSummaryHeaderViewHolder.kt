/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.databinding.RvMedDSummaryHeaderBinding
import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.ui.checkout.adapter.VaccineSummaryItemAdapterOptions

class MedDSummaryHeaderViewHolder(
    val binding: RvMedDSummaryHeaderBinding,
    private val patient: Patient,
    private val options: VaccineSummaryItemAdapterOptions?
) : RecyclerView.ViewHolder(binding.root) {
    fun bind() {
        setCheckoutInfo()
    }

    private fun setCheckoutInfo() {
        with(itemView) {
            val firstName = options?.updatedFirstName ?: patient.firstName
            val lastName = options?.updatedLastName ?: patient.lastName

            binding.medDPatientName.text = context.formatString(
                R.string.patient_name_display, firstName.captureName(), lastName.captureName()
            )

            if (!patient.originatorPatientId.isNullOrEmpty()) {
                binding.medDPatientId.text = patient.originatorPatientId
            } else {
                binding.medDPatientId.text = patient.id.toString()
            }

            val dob = options?.manualDob?.toLocalDateString("M/dd/yyyy")
                ?: patient.getDobString()
            binding.medDPatientDob.text = dob ?: ""
        }
    }
}
