/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogPatientInfoConfirmationBinding

class PatientInfoConfirmationDialog : BaseDialog<DialogPatientInfoConfirmationBinding>() {
    companion object {
        const val REQUEST_KEY = "patientInfoConfirmationDialogRequestKey"
        const val OPTION_SELECTED_BUNDLE_KEY = "optionSelectedBundleKey"
    }

    enum class Option {
        YES,
        NO
    }

    private val args: PatientInfoConfirmationDialogArgs by navArgs()

    override fun handleBack(): Boolean = true

    override val baseDialogProperties: DialogProperties
        get() = DialogProperties(
            dialogSize = DialogSize.LEGACY_WRAP
        )

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.apply {
            textViewFirstName.text =
                getString(R.string.dialog_patient_info_confirmation_first_name, args.firstName)
            textViewLastName.text =
                getString(R.string.dialog_patient_info_confirmation_last_name, args.lastName)
            textViewDateOfBirth.text =
                getString(
                    R.string.dialog_patient_info_confirmation_date_of_birth,
                    args.dateOfBirth
                )
            textViewAddress.text =
                getString(
                    R.string.dialog_patient_info_confirmation_address,
                    args.address
                )

            buttonYes.setOnClickListener {
                dismiss()
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(OPTION_SELECTED_BUNDLE_KEY to Option.YES.ordinal)
                )
            }

            buttonNo.setOnClickListener {
                dismiss()
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(OPTION_SELECTED_BUNDLE_KEY to Option.NO.ordinal)
                )
            }
        }
    }

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogPatientInfoConfirmationBinding =
        DialogPatientInfoConfirmationBinding.inflate(inflater, container, true)
}
