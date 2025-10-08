/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Picasso
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.checkout.CheckoutInsuranceCardCollectionFlow
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.safeLet
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.databinding.FragmentEditPatientInsuranceBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.MobilePicasso
import com.vaxcare.vaxhub.model.patient.AppointmentMediaField
import com.vaxcare.vaxhub.ui.navigation.CheckoutCollectInfoDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.navigation.PatientEditDestination
import com.vaxcare.vaxhub.viewmodel.CheckoutCollectInsuranceState
import com.vaxcare.vaxhub.viewmodel.CheckoutCollectInsuranceViewModel
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.State
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutCollectInsuranceFragment : BaseEditInsuranceFragment() {
    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    @MobilePicasso
    override lateinit var picasso: Picasso

    @Inject
    override lateinit var baseDestination: PatientEditDestination

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: CheckoutCollectInfoDestination

    private val viewModel: CheckoutCollectInsuranceViewModel by viewModels()

    private val screenTitle = "CapturePayerInfo_Card"
    private val args: CheckoutCollectInsuranceFragmentArgs by navArgs()
    private val appointmentId by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment?.id ?: 0
    }

    private val stagedProducts
        get() = appointmentViewModel.currentCheckout.stagedProducts

    override val frontInsuranceFragmentId = R.id.checkoutFrontInsuranceFragment
    override val backInsuranceFragmentId = R.id.checkoutBackInsuranceFragment

    override fun init(view: View, savedInstanceState: Bundle?) {
        super.init(view, savedInstanceState)
        appointmentViewModel.currentCheckout.revertPaymentFlips()
        viewModel.getOptOutButtonWording()
        appointmentViewModel.currentCheckout.insuranceCardFlowPresented = true
        logScreenNavigation(screenTitle)
        viewModel.state.observe(viewLifecycleOwner, ::handleUiState)
    }

    private fun handleUiState(state: State) {
        if (state != LoadingState) {
            endLoading()
        }

        when (state) {
            LoadingState -> startLoading()
            CheckoutCollectInsuranceState.NavigateToSummary -> {
                viewModel.resetState()
                destination.toSummaryFromInsurance(
                    fragment = this@CheckoutCollectInsuranceFragment,
                    appointmentId = appointmentId
                )
            }

            CheckoutCollectInsuranceState.NavigateToPhoneCaptureFlow -> {
                viewModel.resetState()
                val data = mapOf(NO_CARD_FLOW to args.payer)
                appointmentViewModel.clearEditedFields(fragmentTag)
                globalDestinations.goBack(
                    fragment = this@CheckoutCollectInsuranceFragment,
                    backData = data
                )
            }
            CheckoutCollectInsuranceState.CashCheckOption -> {
                binding?.patientEditNoInsuranceCard?.text = getString(R.string.med_d_copay_no_charge)
            }
            CheckoutCollectInsuranceState.NoCardInsuranceOption -> {
                binding?.patientEditNoInsuranceCard?.text = getString(R.string.patient_edit_no_insurance_card)
            }
        }
    }

    override fun onNoInsuranceCardClicked() {
        viewModel.noInsuranceCardSelected()
    }

    override fun onNavigateNext() {
        val cardUrls = buildCardUrls(frontCardUrl, backCardUrl)
        val mediaFields = listOfNotNull(
            cardUrls.frontUrl?.let { AppointmentMediaField.InsuranceCardFront(it) },
            cardUrls.backUrl?.let { AppointmentMediaField.InsuranceCardBack(it) }
        )
        appointmentViewModel.addEditedFields(
            tagSet = fragmentTag,
            fields = mediaFields.toTypedArray()
        )

        if (mediaFields.isNotEmpty()) {
            appointmentViewModel.currentCheckout.insuranceCollectionMethod =
                CheckoutInsuranceCardCollectionFlow.InsuranceCardCollectionMethod.CARD_SCAN
        }

        destination.toSummaryFromInsurance(
            fragment = this@CheckoutCollectInsuranceFragment,
            appointmentId = appointmentId
        )
    }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_edit_patient_insurance,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentEditPatientInsuranceBinding =
        FragmentEditPatientInsuranceBinding.bind(container)

    private fun buildCardUrls(frontCardUrl: String?, backCardUrl: String?): CardUrls {
        val filePair = safeLet(frontCardUrl, backCardUrl) { frontCard, backCard ->
            File(
                requireContext().cacheDir.absolutePath,
                File(frontCard).name
            ).absolutePath to File(
                requireContext().cacheDir.absolutePath,
                File(backCard).name
            ).absolutePath
        } ?: (null to null)

        return CardUrls(filePair.first, filePair.second, retriedPhoto)
    }

    private data class CardUrls(
        val frontUrl: String?,
        val backUrl: String?,
        val retried: Boolean
    )
}
