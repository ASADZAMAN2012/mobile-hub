/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.makeLongToast
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentUpdatePatientBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.UpdatePatientViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PatientUpdateFragment : BaseFragment<FragmentUpdatePatientBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations
    private val viewModel: UpdatePatientViewModel by viewModels()

    private val args: PatientUpdateFragmentArgs by navArgs()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_update_patient,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentUpdatePatientBinding =
        FragmentUpdatePatientBinding.bind(container)

    override fun handleBack(): Boolean = true

    override fun init(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                LoadingState -> showLoading()
                UpdatePatientViewModel.UpdatePatientState.UpdateSuccessful -> onSuccess()
                UpdatePatientViewModel.UpdatePatientState.UpdateFailed -> onFailure()
                else -> Unit
            }
        }

        viewModel.updatePatient(
            appointmentId = args.data.appointmentId,
            data = args.data
        )
    }

    private fun showLoading() {
        (binding?.loading?.drawable as? AnimatedImageDrawable)?.start()
    }

    private fun stopLoading() {
        (binding?.loading?.drawable as? AnimatedImageDrawable)?.stop()
    }

    private fun onSuccess() {
        stopLoading()
        globalDestinations.goToCheckout(
            fragment = this@PatientUpdateFragment,
            appointmentId = args.data.appointmentId!!,
            updateData = args.collectPhoneData ?: PatientCollectData(
                frontInsurancePath = args.data.frontInsurancePath,
                backInsurancePath = args.data.backInsurancePath
            )
        )
    }

    private fun onFailure() {
        context?.makeLongToast(R.string.patient_edit_failed)
        globalDestinations.goBack(this@PatientUpdateFragment)
    }
}
