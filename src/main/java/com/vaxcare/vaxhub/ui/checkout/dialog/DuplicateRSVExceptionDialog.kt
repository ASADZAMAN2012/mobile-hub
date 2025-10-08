/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.app.Dialog
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
import com.vaxcare.vaxhub.databinding.DialogDuplicateRsvExceptionBinding
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode

class DuplicateRSVExceptionDialog : BaseDialog<DialogDuplicateRsvExceptionBinding>() {
    companion object {
        const val DUPLICATE_RSV_EXCEPTION_RESULT_KEY = "duplicateRSVExceptionResultKey"
        const val DUPLICATE_RSV_USER_ACTION_KEY = "duplicateRSVUserActionKey"
        const val PARTNER_BILL = "partnerBill"
        const val SELF_PAY = "selfPay"
        const val KEEP_DOSE = "keepDose"
        const val REMOVE_DOSE = "removeDose"
    }

    val args: DuplicateRSVExceptionDialogArgs by navArgs()

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogDuplicateRsvExceptionBinding =
        DialogDuplicateRsvExceptionBinding.inflate(inflater, container, true)

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
            if (!args.otPayRate.isNullOrBlank()) {
                try {
                    val oneTouchRate = BigDecimal(args.otPayRate)
                    if (oneTouchRate != BigDecimal.ZERO) {
                        buttonSelfPay.text =
                            @Suppress("StringFormatMatches")
                            getString(
                                R.string.dialog_duplicate_rsv_self_pay_amount_fmt,
                                oneTouchRate.setScale(2, RoundingMode.HALF_UP)?.toDouble()
                            )
                    }
                } catch (ex: Exception) {
                    Timber.e("Error converting one touch rate: ${args.otPayRate}")
                }
            }

            if (!args.shouldShowPaymentFlip) {
                textViewBillingMessage.visibility = View.GONE
                textViewBillingPrompt.visibility = View.GONE
                buttonPartnerBill.visibility = View.GONE
                buttonSelfPay.visibility = View.GONE
                buttonKeepDose.visibility = View.VISIBLE
            }

            buttonPartnerBill.setOnClickListener {
                setFragmentResultAndDismiss(PARTNER_BILL)
            }
            buttonSelfPay.setOnClickListener {
                setFragmentResultAndDismiss(SELF_PAY)
            }
            buttonKeepDose.setOnClickListener {
                setFragmentResultAndDismiss(KEEP_DOSE)
            }
            buttonRemoveDose.setOnClickListener {
                setFragmentResultAndDismiss(REMOVE_DOSE)
            }
        }
    }

    private fun setFragmentResultAndDismiss(action: String) {
        dismiss()
        setFragmentResult(
            DUPLICATE_RSV_EXCEPTION_RESULT_KEY,
            bundleOf(
                DUPLICATE_RSV_USER_ACTION_KEY to action
            )
        )
    }
}
