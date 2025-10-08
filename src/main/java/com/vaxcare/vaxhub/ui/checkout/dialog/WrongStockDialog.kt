/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogWrongStockSelectedBinding
import com.vaxcare.vaxhub.ui.checkout.dialog.WrongStockDialog.Options.KEEP_DOSE
import com.vaxcare.vaxhub.ui.checkout.dialog.WrongStockDialog.Options.REMOVE_DOSE
import com.vaxcare.vaxhub.ui.checkout.dialog.WrongStockDialog.Options.SET_STOCK

class WrongStockDialog :
    BaseDialog<DialogWrongStockSelectedBinding>() {
    private val args: WrongStockDialogArgs by navArgs()

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
            when {
                args.isStockSelector -> setUpStockSelectorBehavior()
                else -> setUpDefaultBehavior()
            }
        }
    }

    private fun DialogWrongStockSelectedBinding.setUpStockSelectorBehavior() {
        textViewBody.text = getString(
            R.string.dialog_wrong_stock_selected_body_fmt,
            args.appointmentStockType,
            getString(R.string.dialog_wrong_stock_selected_suffix2)
        )
        buttonSetStock.apply {
            isVisible = true
            setOnClickListener {
                dismiss()
                setFragmentResult(
                    WRONG_STOCK_DIALOG_RESULT_KEY,
                    bundleOf(OPTION_SELECTED_BUNDLE_KEY to SET_STOCK.ordinal)
                )
            }
        }

        buttonKeepDose.apply {
            setText(R.string.dialog_wrong_stock_selected_remove_dose)
            setOnClickListener {
                dismiss()
                setFragmentResult(
                    WRONG_STOCK_DIALOG_RESULT_KEY,
                    bundleOf(OPTION_SELECTED_BUNDLE_KEY to REMOVE_DOSE.ordinal)
                )
            }
        }

        buttonRemoveDose.apply {
            setText(R.string.dialog_wrong_stock_selected_keep_stock)
            setOnClickListener {
                dismiss()
                setFragmentResult(
                    WRONG_STOCK_DIALOG_RESULT_KEY,
                    bundleOf(OPTION_SELECTED_BUNDLE_KEY to KEEP_DOSE.ordinal)
                )
            }
        }
    }

    private fun DialogWrongStockSelectedBinding.setUpDefaultBehavior() {
        textViewBody.text = getString(
            R.string.dialog_wrong_stock_selected_body_fmt,
            args.appointmentStockType,
            getString(R.string.dialog_wrong_stock_selected_suffix_fmt, args.appointmentStockType)
        )

        buttonKeepDose.setOnClickListener {
            dismiss()
            setFragmentResult(
                WRONG_STOCK_DIALOG_RESULT_KEY,
                bundleOf(OPTION_SELECTED_BUNDLE_KEY to KEEP_DOSE.ordinal)
            )
        }

        buttonRemoveDose.setOnClickListener {
            dismiss()
            setFragmentResult(
                WRONG_STOCK_DIALOG_RESULT_KEY,
                bundleOf(OPTION_SELECTED_BUNDLE_KEY to REMOVE_DOSE.ordinal)
            )
        }
    }

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogWrongStockSelectedBinding =
        DialogWrongStockSelectedBinding.inflate(inflater, container, true)

    enum class Options {
        KEEP_DOSE,
        REMOVE_DOSE,
        SET_STOCK;

        companion object {
            fun getMetricString(option: Options) =
                when (option) {
                    KEEP_DOSE -> "Keep"
                    REMOVE_DOSE -> "Remove"
                    SET_STOCK -> "Set Stock"
                }
        }
    }

    companion object {
        const val WRONG_STOCK_DIALOG_RESULT_KEY = "wrongStockDialogResultKey"
        const val OPTION_SELECTED_BUNDLE_KEY = "optionSelectedBundleKey"
    }
}
