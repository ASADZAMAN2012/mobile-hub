/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentPaymentSubmitBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.DoseState
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.VaccineAdapterProductDto
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.enums.PaymentMethod
import com.vaxcare.vaxhub.model.extension.isMultiplePaymentMode
import com.vaxcare.vaxhub.model.inventory.DoseReasonContext
import com.vaxcare.vaxhub.model.metric.CheckoutViewPastMetric
import com.vaxcare.vaxhub.model.patient.AppointmentMediaField
import com.vaxcare.vaxhub.model.patient.InfoField
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.ui.checkout.MedDSignatureCollectFragment.Companion.SIGNATURE_URI_KEY
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogArgs
import com.vaxcare.vaxhub.ui.navigation.CheckoutSummaryDestination
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.BaseCheckoutSummaryViewModel
import com.vaxcare.vaxhub.viewmodel.PaymentSubmitViewModel
import com.vaxcare.vaxhub.viewmodel.PaymentSubmitViewModel.PaymentSubmitState
import com.vaxcare.vaxhub.viewmodel.PhoneWorkflowData
import com.vaxcare.vaxhub.viewmodel.State
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PaymentSubmitFragment : BaseFragment<FragmentPaymentSubmitBinding>() {
    private val appointmentViewModel: AppointmentViewModel by activityViewModels()
    private val args: PaymentSubmitFragmentArgs by navArgs()
    private val viewModel: PaymentSubmitViewModel by viewModels()

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var destination: CheckoutSummaryDestination

    private val selfPayCashOrCheck by lazy { args.selfpayCashOrCheck }

    private val signatureFileUri by lazy { args.fileUri }

    private val paymentInformation by lazy { args.paymentInformation }

    private val isEditCheckoutFromComplete by lazy { appointmentViewModel.currentCheckout.isEditCheckoutFromComplete }
    private val stagedNewProducts by lazy {
        val validDoseStates = listOf(DoseState.ADMINISTERED, DoseState.ADDED)
        appointmentViewModel.currentCheckout.stagedProducts.filter { validDoseStates.contains(it.doseState) }
    }

    private val appointmentId by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment?.id ?: 0
    }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_payment_submit,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentPaymentSubmitBinding =
        FragmentPaymentSubmitBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        (binding?.loading?.drawable as? AnimatedImageDrawable)?.start()
        viewModel.state.observe(viewLifecycleOwner) { handleState(it) }
        submitSignature()
    }

    private fun handleState(state: State) {
        when (state) {
            PaymentSubmitState.SignatureUploadStarted -> onSignatureSent()
            PaymentSubmitState.SignatureUploadSkipped -> doCheckout()
            PaymentSubmitState.ErrorTryAgainSelected -> doCheckout()
            PaymentSubmitState.ErrorOkSelected -> saveSignatureAndBackToSummary()
            BaseCheckoutSummaryViewModel.CheckoutSummaryState.CheckoutFailure -> onPaymentFailure()

            is BaseCheckoutSummaryViewModel.CheckoutSummaryState.CheckoutSuccess ->
                onCheckoutFinished()

            else -> Unit
        }
    }

    private fun onPaymentFailure() {
        setFragmentResultListener(ErrorDialog.RESULT, viewModel::onDialogResult)
        destination.goToErrorDialog(
            this,
            ErrorDialogArgs(
                title = R.string.dialog_trouble_connecting_title,
                body = R.string.dialog_trouble_connecting_body,
                primaryButton = R.string.dialog_trouble_connecting_cta1,
                secondaryButton = R.string.dialog_trouble_connecting_cta2,
                optionalArg = appointmentId
            )
        )
    }

    private fun submitSignature() {
        viewModel.uploadSignature(requireContext(), appointmentId, signatureFileUri)
    }

    private fun onSignatureSent() {
        appointmentViewModel.addEditedFields(fragmentTag, AppointmentMediaField.Signature())
        doCheckout()
    }

    private fun doCheckout() {
        val checkout = appointmentViewModel.currentCheckout.copy()
        val shotAdministrator = checkout.administrator
        val appointment = checkout.selectedAppointment
        val addedDoseStates = listOf(DoseState.ADMINISTERED, DoseState.ADDED)
        val products = checkout.stagedProducts
            .filter { addedDoseStates.contains(it.doseState) }.toMutableList()
        val unAdministeredOrders = checkout.orderWrapper?.products
            ?.filter { it.reasonContext == DoseReasonContext.ORDER_UNFILLED }
            ?: emptyList()
        val deltaFields = with(mutableListOf<InfoField>()) {
            addAll(appointmentViewModel.deltaFields)
            reverse()
            val res = toSet()
            res.reversed()
        }
        resolvePaymentModeReasons(stagedNewProducts, appointment)

        // Checkout.ViewPast
        analyticsCheckoutViewPastMetric(appointment)
        val phoneWorkflow = PhoneWorkflowData(
            phoneNumberFlowPresented = checkout.phoneNumberFlowPresented,
            phoneContactStatus = checkout.phoneContactStatus,
            phoneContactReasons = checkout.phoneContactReasons
        )

        viewModel.completeCheckout(
            selectedAppointment = appointment,
            stagedProducts = products,
            shotAdminId = shotAdministrator?.id,
            deltas = deltaFields,
            ordersUnadministered = unAdministeredOrders,
            networkStatus = networkMonitor.networkStatus.value ?: NetworkStatus.DISCONNECTED,
            presentedRiskAssessmentId = appointmentViewModel.currentCheckout.presentedRiskAssessmentId,
            phoneWorkflowData = phoneWorkflow,
            paymentInformation = paymentInformation,
            pregnancyPrompt = appointmentViewModel.currentCheckout.pregnancyPrompt,
            weeksPregnant = appointmentViewModel.currentCheckout.weeksPregnant,
            riskFactors = appointmentViewModel.currentCheckout.riskFactors
        )
    }

    private fun resolvePaymentModeReasons(models: List<VaccineAdapterProductDto>, appointment: Appointment?) {
        // set final paymentModeReason. SelfPayOptOut gets highest precedence (see below)
        val isDataMissing = appointment?.isMissingDataRisk() ?: false
        models.filter { it.hasDisplayIssues && it.paymentMode != null && it.paymentModeReason == null }
            .forEach {
                it.paymentModeReason = when {
                    isDataMissing -> PaymentModeReason.RequestedMediaNotProvided
                    it.ageIndicated -> PaymentModeReason.OutOfAgeIndication
                    else -> null
                }
            }

        if (selfPayCashOrCheck) {
            // override all SelfPays to PartnerBill and set as SelfPayOptOut
            models.filter {
                it.paymentMode == PaymentMode.SelfPay || (
                    it.paymentMode == null &&
                        it.appointmentPaymentMethod == PaymentMethod.SelfPay
                )
            }
                .forEach {
                    it.paymentMode = PaymentMode.PartnerBill
                    it.paymentModeReason = PaymentModeReason.SelfPayOptOut
                }
        }
    }

    // MixPanel for Checkout.ViewPast
    private fun analyticsCheckoutViewPastMetric(appointment: Appointment?) {
        appointment?.let {
            val checkContext = if (isEditCheckoutFromComplete) {
                CheckoutViewPastMetric.CheckContext.COMPLETE_SCREEN
            } else {
                CheckoutViewPastMetric.CheckContext.PATIENT_SCHEDULE
            }
            val checkoutRelativeTime =
                CheckoutViewPastMetric.RelativeTime.checkoutRelativeTime(appointment.appointmentTime.toLocalDate())

            val isAnyLotRemoved =
                appointmentViewModel.currentCheckout.stagedProducts.any { it.isAnyLotRemoved() }
            val isAnyLotAdded =
                appointmentViewModel.currentCheckout.stagedProducts.any { it.isAnyLotAdded() }
            val checkoutResult = when {
                appointment.isEditable == false -> CheckoutViewPastMetric.CheckoutResult.VIEW_ONLY
                !isAnyLotRemoved && !isAnyLotAdded -> CheckoutViewPastMetric.CheckoutResult.NO_EDITS
                isAnyLotRemoved && isAnyLotAdded -> CheckoutViewPastMetric.CheckoutResult.BOTH_EDITS
                isAnyLotRemoved -> CheckoutViewPastMetric.CheckoutResult.LOT_REMOVED
                else -> CheckoutViewPastMetric.CheckoutResult.LOT_ADDED
            }
            val checkoutEvent = CheckoutViewPastMetric(
                appointment.id,
                checkContext.displayName,
                checkoutRelativeTime.displayName,
                checkoutResult.displayName
            )
            Timber.d(
                "CheckoutViewPastMetric = ${checkoutEvent.visitId} / " +
                    "${checkoutEvent.checkContext} / " +
                    "${checkoutEvent.checkoutRelativeTime} / " +
                    "${checkoutEvent.checkoutResult}"
            )
            analytics.saveMetric(checkoutEvent)
        }
    }

    private fun saveSignatureAndBackToSummary() {
        destination.goToPaymentSummaryFromPaymentSubmit(
            fragment = this,
            data = mapOf(SIGNATURE_URI_KEY to signatureFileUri)
        )
        viewModel.resetState()
    }

    /**
     * Navigate after checkout finished
     */
    private fun onCheckoutFinished() {
        (binding?.loading?.drawable as? AnimatedImageDrawable)?.stop()
        destination.toCheckoutCompleteFromSignature(
            fragment = this@PaymentSubmitFragment,
            shotCount = stagedNewProducts.size,
            multiplePaymentMode = stagedNewProducts.isMultiplePaymentMode()
        )
        viewModel.resetState()
    }
}
