/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogTroubleConnectingBinding

class TroubleConnectingDialog : BaseDialog<DialogTroubleConnectingBinding>() {
    companion object {
        const val REQUEST_KEY = "troubleConnectingDialogRequestKey"
        const val OPTION_SELECTED_BUNDLE_KEY = "optionSelectedBundleKey"
    }

    enum class Option {
        TRY_AGAIN,
        OK
    }

    private val args: TroubleConnectingDialogArgs by navArgs()

    override val baseDialogProperties: DialogProperties
        get() = DialogProperties(
            dialogSize = DialogSize.LEGACY_WRAP
        )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireActivity(), theme) {
            @Deprecated("Deprecated in Java")
            override fun onBackPressed() {
                // Do nothing
            }
        }
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.apply {
            textViewBody.text = getString(args.bodyStringRes)

            buttonTryAgain.setOnClickListener {
                dismiss()
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(OPTION_SELECTED_BUNDLE_KEY to Option.TRY_AGAIN.ordinal)
                )
            }

            buttonOk.setOnClickListener {
                dismiss()
                setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(OPTION_SELECTED_BUNDLE_KEY to Option.OK.ordinal)
                )
            }
        }
    }

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogTroubleConnectingBinding =
        DialogTroubleConnectingBinding.inflate(inflater, container, true)
}
