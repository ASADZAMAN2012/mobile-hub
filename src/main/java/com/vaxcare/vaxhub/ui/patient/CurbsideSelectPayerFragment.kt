/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.view.View
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.databinding.FragmentSelectPayerBinding
import com.vaxcare.vaxhub.databinding.ViewSelectPayerBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.ui.navigation.CaptureFlowDestination
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CurbsideSelectPayerFragment : BasePayerFragment<FragmentSelectPayerBinding>() {
    companion object {
        private const val SCREEN_NAME = "CurbsideSelectPayer"
    }

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: CaptureFlowDestination

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_select_payer,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentSelectPayerBinding = FragmentSelectPayerBinding.bind(container)

    override val payerBinding: ViewSelectPayerBinding?
        get() = binding?.viewSelectPayer

    override fun init(view: View, savedInstanceState: Bundle?) {
        super.init(view, savedInstanceState)
        logScreenNavigation(SCREEN_NAME)
        binding?.topBar?.setTitle(resources.getString(R.string.patient_add_title))
        binding?.topBar?.setSubTitle(resources.getString(R.string.patient_select_payer))
    }

    override fun setAppointmentInfo(appointment: Appointment) = Unit

    override fun onPayerUpdated(
        payer: Payer?,
        isSkipInsuranceScan: Boolean,
        isInsurancePhoneCaptureDisabled: Boolean,
        isCreditCardCaptureDisabled: Boolean
    ) {
        payer?.apply {
            if (isNormalPayer()) {
                onNormalPayerSelected(this)
            } else {
                onOtherPayerSelected(this)
            }
        }
    }

    private fun onNormalPayerSelected(payer: Payer) {
        appointmentViewModel.appointmentCreation.payer = payer
        binding?.viewSelectPayer?.payerSearchEt?.setText("")
        destination.goToCaptureFrontInsurance(this@CurbsideSelectPayerFragment)
    }

    private fun onOtherPayerSelected(payer: Payer) {
        appointmentViewModel.appointmentCreation.payer = payer
        destination.goToCurbsideConfirmPatientInfo(this@CurbsideSelectPayerFragment)
    }
}
