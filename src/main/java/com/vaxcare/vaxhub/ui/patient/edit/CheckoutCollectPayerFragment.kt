/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.os.Bundle
import android.view.View
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.checkout.CheckoutInsuranceCardCollectionFlow
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.Constant.COLLECT_PHONE_DATA_FRAGMENT_TAG
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.databinding.FragmentSelectPayerBinding
import com.vaxcare.vaxhub.databinding.ViewSelectPayerBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.appointment.PhoneContactReasons
import com.vaxcare.vaxhub.ui.navigation.CheckoutCollectInfoDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.patient.BasePayerFragment
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutCollectPayerFragment : BasePayerFragment<FragmentSelectPayerBinding>() {
    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: CheckoutCollectInfoDestination

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_select_payer,
        hasToolbar = false
    )
    private val screenTitle = "CapturePayerInfo_Payer"
    private val appointmentId by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment?.id ?: 0
    }

    private val stagedProducts
        get() = appointmentViewModel.currentCheckout.stagedProducts

    private val isVaxCare3: Boolean
        get() = appointmentViewModel.currentCheckout.isVaxCare3

    override fun bindFragment(container: View): FragmentSelectPayerBinding = FragmentSelectPayerBinding.bind(container)

    override val payerBinding: ViewSelectPayerBinding?
        get() = binding?.viewSelectPayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appointmentViewModel.currentCheckout.insuranceCardFlowPresented = false
        appointmentViewModel.currentCheckout.insuranceCollectionMethod =
            CheckoutInsuranceCardCollectionFlow.InsuranceCardCollectionMethod.NONE
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        super.init(view, savedInstanceState)
        logScreenNavigation(screenTitle)
        appointmentViewModel.currentCheckout.revertPaymentFlips()
        binding?.apply {
            topBar.setTitle(resources.getString(R.string.patient_edit_title))
            selectPayerLabel.show()
        }

        observeBackstack()
    }

    private fun observeBackstack() {
        getResultLiveData<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)?.observe(
            viewLifecycleOwner
        ) { data ->
            val agreed =
                appointmentViewModel.addPhoneCollectField(
                    tagSet = COLLECT_PHONE_DATA_FRAGMENT_TAG,
                    data = data,
                    PhoneContactReasons.INSURANCE_CARD
                )
            if (isVaxCare3 && !agreed) {
                appointmentViewModel.currentCheckout.flipPartnerBill(
                    reason = PaymentModeReason.RequestedMediaNotProvided
                )
            }

            removeResult<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)
            navigateToSummary()
        }

        getResultLiveData<Payer>(BaseEditInsuranceFragment.NO_CARD_FLOW)?.observe(
            viewLifecycleOwner
        ) { data ->
            removeResult<Payer>(BaseEditInsuranceFragment.NO_CARD_FLOW)
            navigateToPhoneCollectionFlow(data)
        }
    }

    private fun navigateToSummary() {
        globalDestinations.goToCheckoutSummary(
            fragment = this@CheckoutCollectPayerFragment,
            appointmentId = appointmentId,
        )
    }

    private fun navigateToPhoneCollectionFlow(payer: Payer?) {
        addPayerDelta(payer)
        appointmentViewModel.currentCheckout.insuranceCollectionMethod =
            CheckoutInsuranceCardCollectionFlow.InsuranceCardCollectionMethod.PHONE_CAPTURE
        payer?.let {
            val appointment = appointmentViewModel.currentCheckout.selectedAppointment!!
            destination.toPhoneCollectionFlow(
                this@CheckoutCollectPayerFragment,
                appointmentId = appointment.id,
                patientId = appointment.patient.id,
                currentPhone = appointment.patient.phoneNumber
            )
        }
    }

    override fun setAppointmentInfo(appointment: Appointment) = Unit

    override fun onPayerUpdated(
        payer: Payer?,
        isSkipInsuranceScan: Boolean,
        isInsurancePhoneCaptureDisabled: Boolean,
        isCreditCardCaptureDisabled: Boolean
    ) {
        addPayerDelta(payer)
        payer?.apply {
            when {
                isNormalPayer() &&
                    isSkipInsuranceScan &&
                    !isInsurancePhoneCaptureDisabled -> navigateToPhoneCollectionFlow(payer)

                isNormalPayer() &&
                    isSkipInsuranceScan &&
                    isInsurancePhoneCaptureDisabled -> toSummaryScreen()

                isNormalPayer() && !isSkipInsuranceScan -> navigateToCollectInsurance(this)
                else -> flipSelfPayAndNavigate(isCreditCardCaptureDisabled)
            }
        }
    }

    private fun navigateToCollectInsurance(payer: Payer) {
        destination.toCollectInsuranceFromSelectPayor(
            fragment = this@CheckoutCollectPayerFragment,
            payer = payer
        )
    }

    private fun flipSelfPayAndNavigate(isCreditCardCaptureDisabled: Boolean) {
        stagedProducts
            .filter { !isCreditCardCaptureDisabled && it.paymentMode == null }
            .forEach { it.flipSelfPay(true) }
        toSummaryScreen()
    }

    private fun toSummaryScreen() {
        destination.toSummaryFromPayer(
            fragment = this@CheckoutCollectPayerFragment,
            appointmentId = appointmentId
        )
    }
}
