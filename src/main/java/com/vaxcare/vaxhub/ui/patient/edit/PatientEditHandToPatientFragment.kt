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
import com.vaxcare.vaxhub.databinding.FragmentPatientEditHandToPatientBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.metric.PhoneNumberWorkflowPresented
import com.vaxcare.vaxhub.ui.navigation.PhoneCollectDestination
import com.vaxcare.vaxhub.viewmodel.PatientEditHandToPatientViewModel
import com.vaxcare.vaxhub.viewmodel.PatientEditHandToPatientViewModel.PatientEditHandToPatientState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PatientEditHandToPatientFragment : BaseFragment<FragmentPatientEditHandToPatientBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: PhoneCollectDestination
    private val args: PatientEditHandToPatientFragmentArgs by navArgs()
    private val viewModel: PatientEditHandToPatientViewModel by viewModels()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_patient_edit_hand_to_patient,
        hasToolbar = false
    )

    override fun handleBack(): Boolean = true

    override fun bindFragment(container: View): FragmentPatientEditHandToPatientBinding =
        FragmentPatientEditHandToPatientBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            if (state is PatientEditHandToPatientState) {
                setupOnClickListener(state == PatientEditHandToPatientState.LockedSession)
            }
        }

        viewModel.startCollectionFlow()
    }

    private fun setupOnClickListener(isSessionLocked: Boolean) {
        binding?.buttonOk?.setOnClickListener {
            analytics.stageMetricTimer(PhoneNumberWorkflowPresented.PHONE_COLLECTION_WORKFLOW)
            destination.goToPhoneCollection(
                this@PatientEditHandToPatientFragment,
                PatientCollectData(
                    flow = args.flow,
                    appointmentId = args.appointmentId,
                    patientId = args.patientId,
                    currentPhone = args.currentPhone,
                    isSessionLocked = isSessionLocked
                )
            )
        }
    }
}
