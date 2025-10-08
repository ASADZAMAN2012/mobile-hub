/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogAddNewPatientExitBinding
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddNewPatientExitDialog : BaseDialog<DialogAddNewPatientExitBinding>() {
    @Inject
    lateinit var globalDestinations: GlobalDestinations

    companion object {
        const val ADD_NEW_PATIENT_EXIT_KEY = "add_new_patient_exit_result"
        const val ADD_NEW_PATIENT_EXIT_RESULT = "add_new_patient_exit_result"
    }

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogAddNewPatientExitBinding =
        DialogAddNewPatientExitBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.apply {
            buttonYes.setOnClickListener {
                setFragmentResult(ADD_NEW_PATIENT_EXIT_KEY, bundleOf(ADD_NEW_PATIENT_EXIT_RESULT to true))
                dismiss()
            }
            buttonNo.setOnClickListener {
                setFragmentResult(ADD_NEW_PATIENT_EXIT_KEY, bundleOf(ADD_NEW_PATIENT_EXIT_RESULT to false))
                dismiss()
            }
        }
    }
}
