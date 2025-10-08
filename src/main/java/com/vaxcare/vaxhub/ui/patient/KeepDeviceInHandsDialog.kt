/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogKeepDeviceInHandsBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class KeepDeviceInHandsDialog : BaseDialog<DialogKeepDeviceInHandsBinding>() {
    companion object {
        const val REQUEST_KEY = "keepDeviceInHandsDialogKey"
    }

    private val args: KeepDeviceInHandsDialogArgs by navArgs()

    @Inject @MHAnalyticReport
    lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogKeepDeviceInHandsBinding =
        DialogKeepDeviceInHandsBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.apply {
            if (args.isAddPatientFlow) {
                textViewKeepDeviceDialogTitle.text = getString(R.string.patient_add_keep_device_header)
                textViewKeepDeviceDialogBody.text = getString(R.string.patient_add_keep_device_body)
            }
            buttonOk.setOnClickListener {
                dismiss()
                setFragmentResult(
                    REQUEST_KEY,
                    Bundle.EMPTY
                )
            }
        }
    }
}
