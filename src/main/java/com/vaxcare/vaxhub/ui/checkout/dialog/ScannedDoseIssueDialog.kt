/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.checkout.CheckoutPromptResult
import com.vaxcare.core.report.model.checkout.ExpiredDoseValidationMetric
import com.vaxcare.core.report.model.checkout.OutOfAgeIndicationValidationMetric
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogCheckoutScannedDoseIssuesBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.checkout.ProductIssue
import com.vaxcare.vaxhub.model.enums.DialogAction
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScannedDoseIssueDialog : BaseDialog<DialogCheckoutScannedDoseIssuesBinding>() {
    @Inject
    @MHAnalyticReport
    lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    private val appointmentViewModel: AppointmentViewModel by activityViewModels()
    private val args: ScannedDoseIssueDialogArgs by navArgs()
    private var isExpiredOnly = false
    private var isOutOfAgeOnly = false

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun canShowConnection(): Boolean = false

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogCheckoutScannedDoseIssuesBinding =
        DialogCheckoutScannedDoseIssuesBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        showPendingIssue()
        binding?.buttonKeepDose?.setOnClickListener {
            confirmAndDismiss(DialogAction.POSITIVE)
        }

        binding?.buttonRemoveDose?.setOnClickListener {
            confirmAndDismiss(DialogAction.CANCEL)
        }
    }

    private fun showPendingIssue() {
        args.productIssue?.let { showSingleIssue(it) }
    }

    /**
     * Hides all rollup views
     */
    private fun hideGroups() {
        binding?.scannedDoseExpiredGroup?.hide()
        binding?.scannedDoseAgeRangeGroup?.hide()
        binding?.scannedDoseMultipleEntriesGroup?.hide()
        binding?.scannedDoseNotFluGroup?.hide()
    }

    /**
     * Hide the rollup views and only displays the product issue
     * @param issue
     */
    private fun showSingleIssue(issue: ProductIssue) {
        hideGroups()
        when (issue) {
            ProductIssue.Expired -> {
                isExpiredOnly = true
                binding?.scannedDoseIssueHeader?.setText(R.string.scanned_dose_expired_header)
                binding?.scannedDoseIssueBody?.setText(R.string.scanned_dose_expired_body)
            }

            ProductIssue.OutOfAgeIndication -> {
                isOutOfAgeOnly = true
                binding?.scannedDoseIssueHeader?.setText(R.string.scanned_dose_age_issue_header)
                binding?.scannedDoseIssueBody?.setText(R.string.scanned_dose_age_issue_body)
            }

            ProductIssue.DuplicateLot -> {
                binding?.scannedDoseIssueHeader?.setText(R.string.scanned_dose_multiple_entries_header)
                binding?.scannedDoseIssueBody?.setText(R.string.scanned_dose_multiple_entries_body)
            }

            ProductIssue.RestrictedProduct -> {
                binding?.scannedDoseIssueHeader?.setText(R.string.scanned_dose_not_flu_header)
                binding?.scannedDoseIssueBody?.setText(R.string.scanned_dose_not_flu_body)
            }

            ProductIssue.ProductNotCovered -> {
                binding?.scannedDoseIssueHeader?.setText(R.string.patient_checkout_prompt_exclusion_title)
                binding?.scannedDoseIssueBody?.setText(R.string.patient_checkout_prompt_exclusion_body)
            }

            is ProductIssue.OutOfAgeWarning -> {
                binding?.scannedDoseIssueHeader?.text = issue.title
                binding?.scannedDoseIssueBody?.text = issue.message
            }

            else -> dismiss() // This should never happen
        }
    }

    private fun confirmAndDismiss(action: DialogAction) {
        if (isExpiredOnly) {
            analytics.saveMetric(
                ExpiredDoseValidationMetric(
                    appointmentViewModel.currentCheckout.selectedAppointment?.id,
                    if (action == DialogAction.POSITIVE) {
                        CheckoutPromptResult.KEEP_DOSE
                    } else {
                        CheckoutPromptResult.REMOVE_DOSE
                    }
                )
            )
        }
        if (isOutOfAgeOnly) {
            analytics.saveMetric(
                OutOfAgeIndicationValidationMetric(
                    appointmentViewModel.currentCheckout.selectedAppointment?.id,
                    if (action == DialogAction.POSITIVE) {
                        CheckoutPromptResult.KEEP_DOSE
                    } else {
                        CheckoutPromptResult.REMOVE_DOSE
                    }
                )
            )
        }
        setFragmentResult(
            SCANNED_PRODUCT_ISSUE_DIALOG_RESULT_KEY,
            bundleOf(
                SCANNED_PRODUCT_ISSUE_DIALOG_BUNDLE_KEY to action.ordinal
            )
        )
        globalDestinations.goBack(this@ScannedDoseIssueDialog, mapOf(DIALOG_RESULT to action))
    }

    companion object {
        const val SCANNED_PRODUCT_ISSUE_DIALOG_RESULT_KEY = "scannedProductIssueDialogResultKey"
        const val SCANNED_PRODUCT_ISSUE_DIALOG_BUNDLE_KEY = "scannedProductIssueDialogBundleKey"
    }
}
