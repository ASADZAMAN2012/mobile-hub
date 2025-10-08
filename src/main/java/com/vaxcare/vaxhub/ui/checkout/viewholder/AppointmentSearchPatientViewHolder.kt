/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.invisible
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.databinding.RvAppointmentSearchListItemBinding
import com.vaxcare.vaxhub.model.SearchPatient

class AppointmentSearchPatientViewHolder(
    val binding: RvAppointmentSearchListItemBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(patient: SearchPatient, onStartCheckout: ((patient: SearchPatient) -> Unit)? = null) {
        setPatientDemographicInfo(patient)
        binding.eligibilityIcon.invisible()
        binding.appointmentTime.hide()
        binding.clickArea.setOnSingleClickListener {
            onStartCheckout?.invoke(patient)
        }
    }

    private fun setPatientDemographicInfo(patient: SearchPatient) {
        with(binding) {
            patientLastName.text = "${patient.lastName.captureName()},"
            patientFirstName.text = patient.firstName.captureName()

            if (patient.originatorId?.isNotEmpty() != null) {
                patientOriginatorId.text = patient.originatorId
            } else {
                patientOriginatorId.text = patient.id.toString()
            }

            patientDob.text = patient.getDobString() ?: ""
        }
    }
}
