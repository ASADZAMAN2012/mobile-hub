/**************************************************************************************************
 * Copyright VaxCare (c) 2025.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogAgeWarningBooleanPromptBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.enums.DialogAction
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AgeWarningBooleanPromptDialog : BaseDialog<DialogAgeWarningBooleanPromptBinding>() {
    @Inject
    @MHAnalyticReport
    lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    private val args: AgeWarningBooleanPromptDialogArgs by navArgs()

    override fun canShowConnection(): Boolean = false

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogAgeWarningBooleanPromptBinding =
        DialogAgeWarningBooleanPromptBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        with(binding ?: return) {
            title.text = args.title
            body.text = args.message
            buttonYes.setOnClickListener {
                confirmAndDismiss(DialogAction.POSITIVE)
            }
            buttonNo.setOnClickListener {
                confirmAndDismiss(DialogAction.CANCEL)
            }
        }
    }

    private fun confirmAndDismiss(action: DialogAction) {
        setFragmentResult(
            DIALOG_RESULT_KEY,
            bundleOf(DIALOG_BUNDLE_KEY to action.ordinal)
        )
        globalDestinations.goBack(this, mapOf(DIALOG_RESULT_KEY to action))
    }

    companion object {
        const val DIALOG_RESULT_KEY = "ageWarningBooleanPromptDialogResultKey"
        const val DIALOG_BUNDLE_KEY = "ageWarningBooleanPromptDialogBundleKey"
    }
}
