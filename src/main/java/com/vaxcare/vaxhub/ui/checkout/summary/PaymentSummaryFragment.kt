/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.summary

import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.Constant.COLLECT_PHONE_DATA_FRAGMENT_TAG
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.intToEnum
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.databinding.FragmentPaymentSummaryBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.PaymentInformationRequestBody
import com.vaxcare.vaxhub.model.PaymentMode
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.model.patient.DemographicField
import com.vaxcare.vaxhub.service.UserSessionService
import com.vaxcare.vaxhub.ui.checkout.ChangePaymentMethodMode
import com.vaxcare.vaxhub.ui.checkout.MedDSignatureCollectFragment
import com.vaxcare.vaxhub.ui.checkout.adapter.PaymentSummaryItemAdapter
import com.vaxcare.vaxhub.ui.checkout.adapter.VaccineSummaryItemAdapterOptions
import com.vaxcare.vaxhub.ui.checkout.viewholder.MedDSummaryBottomViewHolder
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.navigation.MedDCheckoutDestination
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.CheckoutSummaryViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PaymentSummaryFragment : BaseSummaryFragment<FragmentPaymentSummaryBinding>() {
    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    override lateinit var localStorage: LocalStorage

    @Inject
    override lateinit var sessionService: UserSessionService

    @Inject
    override lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: MedDCheckoutDestination

    private val screenTitle = "CheckoutSummary"
    private val args: PaymentSummaryFragmentArgs by navArgs()
    override val appointmentViewModel: AppointmentViewModel by activityViewModels()
    override val viewModel: CheckoutSummaryViewModel by viewModels()
    private lateinit var medDSummaryItemAdapter: PaymentSummaryItemAdapter
    private var selectedAppointment: Appointment? = null
    private var chosenPaymentType: ChosenPaymentType = ChosenPaymentType.Uninitialized
    private var isPhoneCollectingEnabled = true
    private var pendingChangePaymentMethodMode: ChangePaymentMethodMode =
        ChangePaymentMethodMode.CASH_OR_CHECK
    override val appointmentId by lazy { args.appointmentId }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_payment_summary,
        hasToolbar = false
    )

    private var signatureUri: String? = null

    private enum class LoginActionCorrelation {
        DEFAULT,
        GO_BACK,
        CHANGE_PAYMENT_METHOD
    }

    override fun bindFragment(container: View): FragmentPaymentSummaryBinding =
        FragmentPaymentSummaryBinding.bind(container)

    override fun handleBack(): Boolean {
        checkSessionNavigation(
            titleResId = R.string.loginPinFragment_reEnterPin,
            correlation = LoginActionCorrelation.GO_BACK.ordinal
        )
        return true
    }

    override fun setupUi(
        appointment: Appointment,
        user: User?,
        disableDuplicateRSV: Boolean,
        phoneCollectingEnabled: Boolean,
        disableCCCapture: Boolean
    ) {
        isPhoneCollectingEnabled = phoneCollectingEnabled
        logScreenNavigation(screenTitle)
        binding?.topBar?.onCloseAction = { handleBack() }

        // at this point - if paymentInfo is null: we have opted for "cash/check" for self pay
        if (chosenPaymentType == ChosenPaymentType.Uninitialized) {
            chosenPaymentType = ChosenPaymentType.fromInfo(args.paymentInformation)
        }

        selectedAppointment = appointment
        addStateHandlerCallback(appointment)
        flipPaymentModeOnDoses()
        medDSummaryItemAdapter = PaymentSummaryItemAdapter(
            appointment = appointment,
            items = appointmentViewModel.currentCheckout.stagedProducts,
            paymentInformation = chosenPaymentType.paymentInfo,
            listener = object : MedDSummaryBottomViewHolder.OnPaymentMethodModeChangeListener {
                override fun onChanged(paymentMethodMode: ChangePaymentMethodMode) {
                    pendingChangePaymentMethodMode = paymentMethodMode
                    checkSessionNavigation(
                        titleResId = R.string.loginPinFragment_reEnterPin,
                        correlation = LoginActionCorrelation.CHANGE_PAYMENT_METHOD.ordinal
                    )
                }
            },
            options = VaccineSummaryItemAdapterOptions(
                manualDob = appointmentViewModel.currentCheckout.manualDob,
                updatedFirstName = appointmentViewModel.deltaFields
                    .firstOrNull { it is DemographicField.FirstName }?.currentValue,
                updatedLastName = appointmentViewModel.deltaFields
                    .firstOrNull { it is DemographicField.LastName }?.currentValue
            )
        )

        binding?.rvVaccines?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = medDSummaryItemAdapter
        }

        binding?.topBar?.setTitle(getString(R.string.med_d_summary_title))
        if (appointmentViewModel.currentCheckout.containsMedDDoses()) {
            // collect signature only when patient is med D AND we got a copay for the dose (shing and tdap)
            binding?.selfPayConfirmLayout?.hide()
            binding?.medDConfirmLayout?.show()

            binding?.fabNext?.setOnClickListener {
                navigateToSignatureCollectFragment()
            }
        } else {
            binding?.selfPayConfirmLayout?.show()
            binding?.medDConfirmLayout?.hide()

            binding?.selfPayCheckoutAdvisory?.text = context?.formatString(
                R.string.patient_checkout_advisory_display,
                appointment.appointmentTime.toLocalDate()
            )

            binding?.continueBtn?.setOnClickListener {
                navigateToSubmitFragment()
            }
        }
    }

    override fun checkoutCompleted(appointment: Appointment, multiplePaymentMode: Boolean) = Unit

    /**
     * Flip payment mode on all doses based on chosen payment type.
     *
     * When chosenPaymentType is CashCheck flip ALL doses to PartnerBill. If flipped dose was
     * selfPay we must also mark with the SelfPayOptOut flag. Otherwise we flip them back.
     */
    private fun flipPaymentModeOnDoses() {
        with(appointmentViewModel.currentCheckout.stagedProducts) {
            if (chosenPaymentType == ChosenPaymentType.CashCheck) {
                forEach {
                    if (
                        PaymentMode.SelfPay in listOf(
                            it.paymentMode,
                            it.appointmentPaymentMethod.toPaymentMode()
                        )
                    ) {
                        it.paymentModeReason = PaymentModeReason.SelfPayOptOut
                    } else {
                        it.cashCheckPartnerBill = true
                    }

                    it.paymentMode = PaymentMode.PartnerBill
                }
            } else {
                filter { it.cashCheckPartnerBill || it.paymentModeReason == PaymentModeReason.SelfPayOptOut }
                    .forEach {
                        it.paymentMode =
                            it.originalPaymentMode ?: it.appointmentPaymentMethod.toPaymentMode()
                        it.paymentModeReason = it.originalPaymentModeReason
                        it.cashCheckPartnerBill = false
                    }
            }
        }
    }

    private fun addStateHandlerCallback(appointment: Appointment) {
        getResultLiveData<PaymentInformationRequestBody>(AppointmentViewModel.PAYMENT_INFORMATION)?.observe(
            viewLifecycleOwner,
            Observer { info ->
                // in here: final flow from collecting creditcard info
                chosenPaymentType = ChosenPaymentType.fromInfo(info)
                flipPaymentModeOnDoses()
                medDSummaryItemAdapter.updatePaymentInformation(info)
                arguments?.putParcelable(AppointmentViewModel.PAYMENT_INFORMATION, info)
                if (info == null) return@Observer
                removeResult<ChangePaymentMethodMode>(AppointmentViewModel.PAYMENT_INFORMATION)
            }
        )

        removeResult<ChangePaymentMethodMode>(BaseDialog.DIALOG_RESULT)
        getResultLiveData<ChangePaymentMethodMode>(BaseDialog.DIALOG_RESULT)?.observe(
            viewLifecycleOwner,
            Observer { mode ->
                if (mode == null) return@Observer
                when (mode) {
                    ChangePaymentMethodMode.CASH_OR_CHECK -> {
                        appointmentViewModel.clearAllPhoneDeltas(COLLECT_PHONE_DATA_FRAGMENT_TAG)
                        chosenPaymentType = ChosenPaymentType.CashCheck
                        arguments?.putParcelable(AppointmentViewModel.PAYMENT_INFORMATION, null)
                        flipPaymentModeOnDoses()
                        medDSummaryItemAdapter.updatePaymentInformation(null)
                    }

                    ChangePaymentMethodMode.COLLECT_CREDIT_CARD -> {
                        destination.goBackPaymentCollection(
                            fragment = this@PaymentSummaryFragment,
                            appointmentId = appointment.id,
                            patientId = appointment.patient.id,
                            enablePhoneCollection = isPhoneCollectingEnabled
                        )
                    }
                }
            }
        )

        getResultLiveData<String?>(MedDSignatureCollectFragment.SIGNATURE_URI_KEY)?.observe(
            viewLifecycleOwner
        ) {
            signatureUri = it
            removeResult<String?>(MedDSignatureCollectFragment.SIGNATURE_URI_KEY)
        }
    }

    private fun navigateToSignatureCollectFragment() {
        destination.toSignatureCollectFragment(
            fragment = this@PaymentSummaryFragment,
            paymentInfo = chosenPaymentType.paymentInfo,
            existingSignatureUri = signatureUri
        )
    }

    private fun navigateToSubmitFragment() {
        destination.toSignatureSubmitFragment(
            fragment = this@PaymentSummaryFragment,
            paymentInfo = chosenPaymentType.paymentInfo
        )
    }

    override fun onLoginSuccess(data: Int) {
        super.onLoginSuccess(data)
        appointmentViewModel.clearAllPhoneDeltas(COLLECT_PHONE_DATA_FRAGMENT_TAG)
        val loginCorrelation = intToEnum(data, LoginActionCorrelation.DEFAULT)
        reportLogin(false, loginCorrelation)
        when (loginCorrelation) {
            LoginActionCorrelation.GO_BACK -> {
                appointmentViewModel.currentCheckout.stagedProducts.forEach {
                    it.paymentMode = it.originalPaymentMode
                    it.paymentModeReason = it.originalPaymentModeReason
                }

                destination.goBackToProductGrid(this)
            }

            LoginActionCorrelation.CHANGE_PAYMENT_METHOD -> {
                destination.toChangePaymentMethodDialog(
                    this@PaymentSummaryFragment,
                    pendingChangePaymentMethodMode
                )
            }

            else -> Timber.e("Error: LoginCorrelation for PaymentSummary unknown")
        }
    }

    override fun onLoginAbort(data: Int) {
        super.onLoginAbort(data)
        reportLogin(false, intToEnum(data, LoginActionCorrelation.DEFAULT))
    }

    override fun onLoginFailure(data: Int) {
        reportLogin(false, intToEnum(data, LoginActionCorrelation.DEFAULT))
        appointmentViewModel.clearCurrentCheckout()
        super.onLoginFailure(data)
    }

    private fun reportLogin(success: Boolean, loginCorrelation: LoginActionCorrelation) {
        val attemptedNavigation = when (loginCorrelation) {
            LoginActionCorrelation.GO_BACK -> "EditCurrentCheckout"
            LoginActionCorrelation.CHANGE_PAYMENT_METHOD -> "EditPaymentMethod"
            else -> "Unknown"
        }

        viewModel.reportLogin(success, attemptedNavigation, appointmentId)
    }

    override fun handleProviderSelected(provider: Provider) {}

    override fun showLoading() {}

    override fun hideLoading() {}

    override fun onDestroyView() {
        binding?.rvVaccines?.adapter = null
        super.onDestroyView()
    }

    sealed class ChosenPaymentType(open val paymentInfo: PaymentInformationRequestBody? = null) {
        object Uninitialized : ChosenPaymentType()

        object CashCheck : ChosenPaymentType()

        data class PayByPhone(override val paymentInfo: PaymentInformationRequestBody?) :
            ChosenPaymentType()

        data class Credit(override val paymentInfo: PaymentInformationRequestBody?) :
            ChosenPaymentType()

        companion object {
            fun fromInfo(info: PaymentInformationRequestBody?): ChosenPaymentType {
                return when {
                    info == null -> CashCheck
                    info.cardNumber.isEmpty() -> PayByPhone(info)
                    else -> Credit(info)
                }
            }
        }
    }
}
