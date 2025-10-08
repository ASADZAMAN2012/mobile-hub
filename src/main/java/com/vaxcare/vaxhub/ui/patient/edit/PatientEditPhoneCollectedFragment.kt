/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentPatientEditPhoneCollectedBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.ui.navigation.PhoneCollectDestination
import com.vaxcare.vaxhub.viewmodel.PatientEditPhoneCollectedViewModel
import com.vaxcare.vaxhub.viewmodel.PatientEditPhoneCollectedViewModel.PhoneCollectedState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PatientEditPhoneCollectedFragment : BaseFragment<FragmentPatientEditPhoneCollectedBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: PhoneCollectDestination

    private val args: PatientEditPhoneCollectedFragmentArgs by navArgs()
    private val viewModel: PatientEditPhoneCollectedViewModel by viewModels()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_patient_edit_phone_collected,
        hasToolbar = false
    )

    override fun handleBack(): Boolean = true

    override fun bindFragment(container: View): FragmentPatientEditPhoneCollectedBinding =
        FragmentPatientEditPhoneCollectedBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                PhoneCollectedState.MoveForward -> destination.goToHandDeviceToNurse(
                    fragment = this,
                    data = args.data
                )
            }
        }

        viewModel.startTimer()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        viewModel.state.removeObservers(this@PatientEditPhoneCollectedFragment)
    }
}
