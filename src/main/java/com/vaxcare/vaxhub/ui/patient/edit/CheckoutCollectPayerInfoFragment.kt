/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.checkout.CheckoutInsuranceCardCollectionFlow
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.Constant.COLLECT_PHONE_DATA_FRAGMENT_TAG
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.databinding.FragmentCollectPayerLandingBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.appointment.PhoneContactReasons
import com.vaxcare.vaxhub.model.patient.PayerField
import com.vaxcare.vaxhub.ui.navigation.CheckoutCollectInfoDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.CheckoutCollectPayerInfoViewModel
import com.vaxcare.vaxhub.viewmodel.CheckoutCollectPayerUiState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutCollectPayerInfoFragment :
    BasePayerInfoFragment<FragmentCollectPayerLandingBinding>() {
    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: CheckoutCollectInfoDestination

    private val checkoutCollectPayerViewModel: CheckoutCollectPayerInfoViewModel by viewModels()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_collect_payer_landing,
        hasMenu = false,
        hasToolbar = false
    )

    private val args: CheckoutCollectPayerInfoFragmentArgs by navArgs()

    private val patientId by lazy { args.infoWrapper.patientId }
    private val infoFields by lazy { args.infoWrapper.infoType.fields }
    override val appointmentId by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment?.id ?: 0
    }
    private val existingPhone by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment?.patient?.phoneNumber
    }

    override fun bindFragment(container: View): FragmentCollectPayerLandingBinding =
        FragmentCollectPayerLandingBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        observeBackstack()
        super.init(view, savedInstanceState)
        checkoutCollectPayerViewModel.state.observe(viewLifecycleOwner) { uiState ->
            when (uiState) {
                CheckoutCollectPayerUiState.NavigateToCollectInsurance ->
                    destination.toCollectInsuranceFromCollectPayorInfo(this, null)

                CheckoutCollectPayerUiState.NavigateToSummaryFragment -> navigateToSummary()
                is CheckoutCollectPayerUiState.NavigateToSelectPayor -> {
                    destination.toSelectPayerFromPayerInfo(
                        fragment = this,
                        infoWrapper = args.infoWrapper
                    )
                }

                is CheckoutCollectPayerUiState.NavigateToPhoneCollectionFlow ->
                    uiState.payer?.let { toPhoneCollectionFlow() }

                else -> Unit
            }
            checkoutCollectPayerViewModel.resetState()
        }
    }

    override fun handleBack(): Boolean {
        appointmentViewModel.clearEditedFields(fragmentTag)
        return super.handleBack()
    }

    override fun setPayerInfo(payer: Payer?, appointment: Appointment) {
        binding?.apply {
            infoFields.forEach {
                when (it) {
                    is PayerField.PayerName -> {
                        currentPayerName.isGone = it.currentValue.isNullOrBlank()
                        currentPayerName.text = it.currentValue
                    }

                    is PayerField.GroupId -> Unit
                    is PayerField.MemberId -> {
                        currentPayerMemberId.isGone = it.currentValue.isNullOrBlank()
                        currentPayerMemberId.text = resources.getString(
                            R.string.current_insurance_member_id_fmt,
                            it.currentValue
                        )
                    }

                    is PayerField.PlanId -> Unit
                    is PayerField.PortalMappingId -> Unit
                }
            }

            currentPayerIssueLabel.show()
            currentPayerIssueLabel.text = appointment.encounterState?.vaccineSecondaryMessage
            buttonYes.setOnSingleClickListener {
                checkoutCollectPayerViewModel.toNextScreen(null)
            }
            selfPayBtn.setOnSingleClickListener { flipSelfPayAndGoToSummaryScreen() }
            buttonNo.setOnSingleClickListener {
                checkoutCollectPayerViewModel.toNextScreen(payer)
            }
            loadingContainer.hide()
            buttonNo.isVisible = true
            buttonYes.isVisible = true
            (loading.drawable as? AnimatedImageDrawable)?.stop()
        }
    }

    private fun flipSelfPayAndGoToSummaryScreen() {
        // flip all untouched order doses to SelfPay
        appointmentViewModel.currentCheckout.stagedProducts.filter { it.paymentMode == null }
            .forEach { it.flipSelfPay(true) }
        destination.toSummaryFromPayerInfo(
            fragment = this@CheckoutCollectPayerInfoFragment,
            appointmentId = appointmentId
        )
    }

    private fun observeBackstack() {
        getResultLiveData<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)?.observe(
            viewLifecycleOwner
        ) { info ->
            info?.let { data ->
                val agreement = appointmentViewModel.addPhoneCollectField(
                    tagSet = COLLECT_PHONE_DATA_FRAGMENT_TAG,
                    data = data,
                    PhoneContactReasons.INSURANCE_CARD
                )

                if (!agreement) {
                    appointmentViewModel.currentCheckout.flipPartnerBill(
                        reason = PaymentModeReason.RequestedMediaNotProvided
                    )
                }

                removeResult<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)
                navigateToSummary()
            }
        }

        getResultLiveData<Payer>(BaseEditInsuranceFragment.NO_CARD_FLOW)?.observe(
            viewLifecycleOwner
        ) {
            removeResult<Payer>(BaseEditInsuranceFragment.NO_CARD_FLOW)
            toPhoneCollectionFlow()
        }
    }

    private fun navigateToSummary() {
        destination.toSummaryFromPayerInfo(
            fragment = this@CheckoutCollectPayerInfoFragment,
            appointmentId = appointmentId
        )
    }

    private fun toPhoneCollectionFlow() {
        appointmentViewModel.currentCheckout.insuranceCollectionMethod =
            CheckoutInsuranceCardCollectionFlow.InsuranceCardCollectionMethod.PHONE_CAPTURE
        destination.toPhoneCollectFlowFromCollectPayerGa(
            fragment = this@CheckoutCollectPayerInfoFragment,
            appointmentId = appointmentId,
            patientId = patientId,
            currentPhone = existingPhone
        )
    }

    override fun setAppointmentInfo(appointment: Appointment) = Unit

    override fun onInfoUpdated(appointment: Appointment) = Unit

    override fun onError() = Unit

    override fun onLoading() {
        binding?.apply {
            buttonNo.hide()
            buttonYes.hide()
            loadingContainer.show()
            (loading.drawable as? AnimatedImageDrawable)?.start()
        }
    }
}
