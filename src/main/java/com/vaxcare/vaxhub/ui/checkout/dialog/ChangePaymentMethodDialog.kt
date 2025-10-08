/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogChangePaymentMethodBinding
import com.vaxcare.vaxhub.ui.checkout.ChangePaymentMethodMode
import com.vaxcare.vaxhub.ui.checkout.ChangePaymentMethodMode.CASH_OR_CHECK
import com.vaxcare.vaxhub.ui.checkout.ChangePaymentMethodMode.COLLECT_CREDIT_CARD
import com.vaxcare.vaxhub.ui.navigation.CheckoutSummaryDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChangePaymentMethodDialog : BaseDialog<DialogChangePaymentMethodBinding>() {
    private val args: ChangePaymentMethodDialogArgs by navArgs()

    @Inject
    lateinit var destination: CheckoutSummaryDestination

    private val mode: ChangePaymentMethodMode by lazy {
        args.mode
    }

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogChangePaymentMethodBinding =
        DialogChangePaymentMethodBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        when (mode) {
            CASH_OR_CHECK ->
                binding?.buttonOk?.text =
                    resources.getString(R.string.med_d_copay_dialog_cancel)
            COLLECT_CREDIT_CARD ->
                binding?.buttonOk?.text =
                    resources.getString(R.string.med_d_copay_dialog_ok)
        }

        binding?.buttonOk?.setOnSingleClickListener {
            destination.goBack(this@ChangePaymentMethodDialog, mapOf(DIALOG_RESULT to mode))
        }

        binding?.buttonCancel?.setOnSingleClickListener {
            destination.goBack(this@ChangePaymentMethodDialog)
        }
    }
}
