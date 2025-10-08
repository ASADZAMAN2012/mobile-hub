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
import com.vaxcare.vaxhub.core.model.DialogProperties
import com.vaxcare.vaxhub.core.model.enums.DialogSize
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.DialogSelfPayBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.model.metric.PaymentInformationPromptPresentedMetric
import com.vaxcare.vaxhub.model.metric.PaymentInformationPromptPresentedMetric.PaymentMethodSelect.CASH_CHECK
import com.vaxcare.vaxhub.model.metric.PaymentInformationPromptPresentedMetric.PaymentMethodSelect.CREDIT_CARD
import com.vaxcare.vaxhub.ui.navigation.CheckoutSummaryDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SelfPayDialog : BaseDialog<DialogSelfPayBinding>() {
    private val args: SelfPayDialogArgs by navArgs()

    @Inject
    lateinit var destination: CheckoutSummaryDestination

    @Inject
    @MHAnalyticReport
    lateinit var analytics: AnalyticReport

    override val baseDialogProperties = DialogProperties(
        dialogSize = DialogSize.LEGACY_WRAP
    )

    override fun bindFragment(inflater: LayoutInflater, container: ViewGroup): DialogSelfPayBinding =
        DialogSelfPayBinding.inflate(inflater, container, true)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.buttonOk?.setOnClickListener {
            onCollectCreditCard()
        }

        binding?.buttonCancel?.setOnClickListener {
            onCashOrCheck(args.appointmentId)
        }
    }

    private fun onCollectCreditCard() {
        saveMetric(args.appointmentId, CREDIT_CARD)

        dismiss()

        destination.goToCollectPaymentFromSelfPay(
            fragment = this@SelfPayDialog,
            ordinalFlow = NoInsuranceCardFlow.CHECKOUT_PAYMENT.ordinal,
            currentPhone = args.currentPhone,
            enablePhoneCollection = args.enablePhoneCollection
        )
    }

    private fun onCashOrCheck(appointmentId: Int) {
        saveMetric(args.appointmentId, CASH_CHECK)

        dismiss()

        destination.goToMedDSummaryFromSelfPay(
            fragment = this@SelfPayDialog,
            appointmentId = appointmentId,
            paymentInformation = null
        )
    }

    private fun saveMetric(
        appointmentId: Int,
        paymentMethod: PaymentInformationPromptPresentedMetric.PaymentMethodSelect
    ) {
        analytics.saveMetric(
            PaymentInformationPromptPresentedMetric(
                appointmentId,
                PaymentInformationPromptPresentedMetric.PaymentInfoTrigger.SELF_PAY.displayName,
                paymentMethod.displayName
            )
        )
    }
}
