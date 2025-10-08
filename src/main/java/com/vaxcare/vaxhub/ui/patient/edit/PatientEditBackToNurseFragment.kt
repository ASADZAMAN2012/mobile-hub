/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentPatientEditBackToNurseBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.ui.navigation.PhoneCollectDestination
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PatientEditBackToNurseFragment : BaseFragment<FragmentPatientEditBackToNurseBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: PhoneCollectDestination

    private val args: PatientEditBackToNurseFragmentArgs by navArgs()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_patient_edit_back_to_nurse,
        hasToolbar = false
    )

    override fun handleBack(): Boolean = true

    override fun bindFragment(container: View): FragmentPatientEditBackToNurseBinding =
        FragmentPatientEditBackToNurseBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.buttonOk?.setOnSingleClickListener {
            if (args.data.isSessionLocked) {
                destination.goBackToPatientNoCard(
                    fragment = this@PatientEditBackToNurseFragment,
                    data = mapOf(AppointmentViewModel.PHONE_FLOW to args.data)
                )
            } else {
                destination.goToRePinPrompt(
                    fragment = this@PatientEditBackToNurseFragment,
                    data = args.data
                )
            }
        }
    }
}
