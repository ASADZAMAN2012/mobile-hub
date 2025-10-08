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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.navArgs
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogConfirmEnteredLotNumberBinding
import com.vaxcare.vaxhub.ui.checkout.dialog.ConfirmEnteredLotNumberDialog.Options.CONFIRM
import com.vaxcare.vaxhub.ui.checkout.dialog.ConfirmEnteredLotNumberDialog.Options.GO_BACK

class ConfirmEnteredLotNumberDialog : BaseDialog<DialogConfirmEnteredLotNumberBinding>() {
    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    private val args: ConfirmEnteredLotNumberDialogArgs by navArgs()

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogConfirmEnteredLotNumberBinding =
        DialogConfirmEnteredLotNumberBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        val navHost = NavHostFragment.findNavController(this)

        binding?.apply {
            textViewTitle.text = getString(
                R.string.dialog_confirm_entered_lot_number_title,
                args.enteredLotNumber
            )

            buttonConfirm.setOnClickListener {
                navHost.navigateUp()
                setFragmentResult(
                    CONFIRM_LOT_NUMBER_DIALOG_RESULT_KEY,
                    bundleOf(OPTION_SELECTED_BUNDLE_KEY to CONFIRM.ordinal)
                )
            }

            buttonGoBack.setOnClickListener {
                navHost.navigateUp()
                setFragmentResult(
                    CONFIRM_LOT_NUMBER_DIALOG_RESULT_KEY,
                    bundleOf(OPTION_SELECTED_BUNDLE_KEY to GO_BACK.ordinal)
                )
            }
        }
    }

    enum class Options {
        CONFIRM,
        GO_BACK
    }

    companion object {
        const val CONFIRM_LOT_NUMBER_DIALOG_RESULT_KEY = "confirmLotNumberDialogResultKey"
        const val OPTION_SELECTED_BUNDLE_KEY = "optionSelectedBundleKey"
    }
}
