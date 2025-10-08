/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogBaseCheckoutExclusionBinding
import com.vaxcare.vaxhub.model.enums.DialogAction
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import java.math.BigDecimal
import java.math.RoundingMode

abstract class BaseOneTouchCheckoutDialog : BaseDialog<DialogBaseCheckoutExclusionBinding>() {
    protected abstract val globalDestinations: GlobalDestinations
    abstract val header: String
    abstract val body: String
    abstract val resultKey: String

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogBaseCheckoutExclusionBinding =
        DialogBaseCheckoutExclusionBinding.inflate(inflater, container, true)

    private val insuranceName: String by lazy {
        arguments?.getString(INSURANCE_NAME) ?: ""
    }

    private val oneTouchRate: BigDecimal by lazy {
        val raw = arguments?.getDouble(ONE_TOUCH) ?: 0.0
        return@lazy BigDecimal(raw)
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.dialogHeader?.text = header
        binding?.dialogBody?.text = body

        disableNativeBackButton()
        val payerDisplayName =
            getString(R.string.patient_checkout_base_exclusion_body_payer, insuranceName)
        val spannable = SpannableStringBuilder(payerDisplayName)
        val boldText = "Payer:"
        spannable.setSpan(
            TextAppearanceSpan(context, R.style.H6BoldBlack),
            0,
            boldText.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
        spannable.setSpan(
            TextAppearanceSpan(context, R.style.H6RegBlack),
            boldText.length,
            payerDisplayName.length,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )

        binding?.dialogBodyPayer?.text = spannable
        binding?.buttonContinue?.text = getString(R.string.patient_checkout_base_exclusion_continue)
        val selfPayText = if (oneTouchRate == BigDecimal.ZERO) {
            getString(R.string.patient_checkout_base_exclusion_neutral)
        } else {
            @Suppress("StringFormatMatches")
            getString(
                R.string.patient_checkout_base_exclusion_neutral_fmt,
                oneTouchRate.setScale(2, RoundingMode.HALF_UP)?.toDouble()
            )
        }
        binding?.buttonNeutral?.text = selfPayText
        binding?.dialogSubBody?.show()

        binding?.buttonDelete?.text = getString(R.string.patient_checkout_base_exclusion_cancel)
        binding?.buttonContinue?.setOnClickListener {
            onActionNext(DialogAction.POSITIVE)
        }

        binding?.buttonNeutral?.setOnClickListener {
            onActionNext(DialogAction.NEUTRAL)
        }

        binding?.buttonDelete?.setOnClickListener {
            onActionNext(DialogAction.CANCEL)
        }

        viewChanges()
    }

    open fun viewChanges() = Unit

    private fun onActionNext(action: DialogAction) {
        setFragmentResultAndGoBack(action)
    }

    private fun setFragmentResultAndGoBack(action: DialogAction) {
        setFragmentResult(
            resultKey,
            bundleOf(
                ONE_TOUCH_BUNDLE_RESULT_KEY to action.ordinal
            )
        )
        globalDestinations.goBack(this@BaseOneTouchCheckoutDialog, mapOf(DIALOG_RESULT to action))
    }

    companion object {
        const val INSURANCE_NAME = "INSURANCE_NAME"
        const val ONE_TOUCH = "ONE_TOUCH"
        const val ONE_TOUCH_BUNDLE_RESULT_KEY = "oneTouchDialogFragmentResultKey"
    }
}
