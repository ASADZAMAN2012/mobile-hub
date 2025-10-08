/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.viewholder

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.core.extension.toLocalTimeString
import com.vaxcare.vaxhub.databinding.RvCheckoutVaccineSummaryHeaderBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.EligibilityUiOptions
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.enums.ShotStatus
import com.vaxcare.vaxhub.ui.checkout.adapter.VaccineSummaryItemAdapterOptions
import com.vaxcare.vaxhub.ui.checkout.extensions.setEligibilityIcon

class VaccineSummaryHeaderViewHolder(
    val binding: RvCheckoutVaccineSummaryHeaderBinding,
    private val appointment: Appointment?,
    private val isEditable: Boolean,
    private val listener: SummaryItemListener?,
    private val options: VaccineSummaryItemAdapterOptions?,
    private val isCheckedOut: Boolean
) : RecyclerView.ViewHolder(binding.root) {
    companion object {
        const val DATE_PATTERN = "MM/dd/yy h:mm a"
    }

    fun bind(provider: Provider?, shotAdministratorName: String?) {
        appointment?.let {
            setupProviders(provider ?: it.provider, shotAdministratorName)
            setCheckoutInfo(it)
        }
    }

    private fun setupProviders(provider: Provider, shotAdministratorName: String?) {
        with(itemView) {
            binding.patientCheckoutProvider.text = context.formatString(
                R.string.provider_name_display,
                provider.firstName.captureName(),
                provider.lastName.captureName()
            )

            binding.editPatientCheckoutProviderContainer.setOnSingleClickListener {
                listener?.onEditPhysicianClick()
            }

            binding.patientCheckoutShotAdmin.text = shotAdministratorName

            binding.editPatientCheckoutShotAdminContainer.setOnSingleClickListener {
                listener?.onEditAdministeredByClick()
            }
        }
    }

    private fun setEligibilityIcon(appointment: Appointment, forceInReview: Boolean) {
        binding.patientCheckEligibilityIcon.setEligibilityIcon(
            options = EligibilityUiOptions(
                appointment = appointment,
                inReviewIconOverride = forceInReview
            )
        )
    }

    private fun setCheckoutInfo(appointment: Appointment) {
        with(itemView) {
            binding.editPatientCheckoutProviderContainer.isVisible = !isCheckedOut
            if (isEditable) {
                binding.patientCheckEligibilityIcon.hide()
                binding.labelAlreadyProcessed.hide()
                binding.apptTime.text = appointment.appointmentTime.toLocalTimeString().lowercase()
            } else {
                binding.labelAlreadyProcessed.show()
                binding.apptTime.text =
                    appointment.appointmentTime.toLocalTimeString(DATE_PATTERN).lowercase()
                binding.editPatientCheckoutProvider.hide()
                binding.editPatientCheckoutShotAdmin.hide()
                binding.editPatientCheckoutShotAdminContainer.hide()

                val reviewOverride = appointment.encounterState?.shotStatus == ShotStatus.PreShot

                setEligibilityIcon(
                    appointment = appointment,
                    forceInReview = reviewOverride
                )
            }

            val firstName = options?.updatedFirstName ?: appointment.patient.firstName
            val lastName = options?.updatedLastName ?: appointment.patient.lastName

            binding.patientName.text = context.formatString(
                R.string.patient_name_display,
                firstName.captureName(),
                lastName.captureName()
            )
            if (!appointment.patient.originatorPatientId.isNullOrEmpty()) {
                binding.patientId.text = appointment.patient.originatorPatientId
            } else {
                binding.patientId.text = appointment.patient.id.toString()
            }

            val dob = options?.manualDob?.toLocalDateString("M/dd/yyyy")
                ?: appointment.patient.getDobString()
            binding.patientDob.text = dob
        }
    }
}
