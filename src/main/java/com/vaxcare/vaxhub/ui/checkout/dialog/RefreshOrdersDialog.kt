/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.BaseDialogLayoutBinding

class RefreshOrdersDialog : BaseDialog<BaseDialogLayoutBinding>() {
    companion object {
        const val REQUEST_KEY = "refreshOrdersDialogRequestKey"
        const val OPTION_SELECTED_BUNDLE_KEY = "refreshOrdersDialogBundleKey"
    }

    enum class Option {
        REFRESH,
        CANCEL
    }

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
            textViewTitle.text = getString(R.string.dialog_refresh_orders_title)
            textViewBody.text = getString(R.string.dialog_refresh_orders_body)
            with(buttonPrimaryCTA) {
                text = getString(R.string.button_continue)
                setOnClickListener {
                    dismiss()
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(OPTION_SELECTED_BUNDLE_KEY to Option.REFRESH.ordinal)
                    )
                }
            }
            with(buttonSecondaryCTA) {
                text = getString(R.string.button_cancel)
                setOnClickListener {
                    dismiss()
                    setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(OPTION_SELECTED_BUNDLE_KEY to Option.CANCEL.ordinal)
                    )
                }
            }
        }
    }

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): BaseDialogLayoutBinding =
        BaseDialogLayoutBinding.inflate(inflater, container, true)
}
