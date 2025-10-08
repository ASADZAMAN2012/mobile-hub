/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.checkout.CheckoutPromptResult
import com.vaxcare.core.report.model.checkout.CopayRequiredValidationMetric
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogMedDCopayBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.model.metric.PaymentInformationPromptPresentedMetric
import com.vaxcare.vaxhub.model.metric.PaymentInformationPromptPresentedMetric.PaymentMethodSelect
import com.vaxcare.vaxhub.ui.navigation.CheckoutSummaryDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MedDCopayDialog : BaseDialog<DialogMedDCopayBinding>() {
    private val args: MedDCopayDialogArgs by navArgs()

    @Inject @MHAnalyticReport
    lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: CheckoutSummaryDestination

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogMedDCopayBinding =
        DialogMedDCopayBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.buttonOk?.setOnClickListener {
            // user selects "Collect Credit Card"
            saveMetric(args.appointmentId, PaymentMethodSelect.CREDIT_CARD)
            dismiss()
            destination.goToCollectPaymentFromMedDCopay(
                this@MedDCopayDialog,
                ordinalFlow = NoInsuranceCardFlow.COPAY_PAYMENT.ordinal,
                currentPhone = args.currentPhone,
                enablePhoneCollection = args.enablePhoneCollection
            )
        }

        binding?.buttonCancel?.setOnClickListener {
            // user selects "Cash or Check"
            saveMetric(args.appointmentId, PaymentMethodSelect.CASH_CHECK)
            dismiss()
            destination.goToMedDSummaryFromMedDCopay(
                this@MedDCopayDialog,
                appointmentId = args.appointmentId,
                paymentInformation = null
            )
        }
    }

    private fun saveMetric(appointmentId: Int, paymentMethod: PaymentMethodSelect) {
        analytics.saveMetric(
            PaymentInformationPromptPresentedMetric(
                appointmentId,
                PaymentInformationPromptPresentedMetric.PaymentInfoTrigger.MEDD.displayName,
                paymentMethod.displayName
            ),
            CopayRequiredValidationMetric(
                appointmentId,
                if (paymentMethod == PaymentMethodSelect.CASH_CHECK) {
                    CheckoutPromptResult.CASH_CHECK
                } else {
                    CheckoutPromptResult.CREDIT_DEBIT
                }
            )
        )
    }
}
