/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.ScreenNavigationMetric
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.databinding.FragmentCurbsideConfirmPatientInfoBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Patient
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.enums.Ethnicity
import com.vaxcare.vaxhub.model.enums.Race
import com.vaxcare.vaxhub.ui.navigation.CurbsideConfirmPatientInfoDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.CurbsideConfirmPatientInfoViewModel
import com.vaxcare.vaxhub.viewmodel.CurbsideConfirmPatientInfoViewModel.ConfirmPatientInfoState.NetworkError
import com.vaxcare.vaxhub.viewmodel.CurbsideConfirmPatientInfoViewModel.ConfirmPatientInfoState.PatientLoaded
import com.vaxcare.vaxhub.viewmodel.CurbsideConfirmPatientInfoViewModel.ConfirmPatientInfoState.ProceedToCreateAppointment
import com.vaxcare.vaxhub.viewmodel.LoadingState
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class CurbsideConfirmPatientInfoFragment :
    BaseFragment<FragmentCurbsideConfirmPatientInfoBinding>() {
    companion object {
        const val METRIC_SCREEN_NAME = "Curbside Confirm Patient Info"
    }

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: CurbsideConfirmPatientInfoDestination

    private var isAddAppt3 = false
    private val appointmentViewModel: AppointmentViewModel by activityViewModels()
    private val viewModel: CurbsideConfirmPatientInfoViewModel by viewModels()
    private val args: CurbsideConfirmPatientInfoFragmentArgs by navArgs()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_curbside_confirm_patient_info,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentCurbsideConfirmPatientInfoBinding =
        FragmentCurbsideConfirmPatientInfoBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        analytics.saveMetric(
            ScreenNavigationMetric(
                screenName = METRIC_SCREEN_NAME
            )
        )

        viewModel.getPatientInfoAndProviders(args.patientId)
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                LoadingState -> startLoading()
                ProceedToCreateAppointment -> goToCreateAppointment()
                NetworkError -> goBackWithOfflineError()
                is PatientLoaded -> {
                    isAddAppt3 = state.isAddAppt3
                    setupUi(state.patient, state.providers)
                }
            }
        }

        when (args.mode) {
            ConfirmPatientInfoMode.VIEW -> {
                binding?.topBar?.setTitle(getString(R.string.patient_info))
                binding?.patientProvider?.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    null,
                    null
                )
                binding?.patientProvider?.isEnabled = false
                binding?.buttonOk?.hide()
            }

            ConfirmPatientInfoMode.EDIT -> {
                binding?.topBar?.setTitle(getString(R.string.patient_confirm_patient_info))
                binding?.patientProvider?.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit),
                    null
                )
                binding?.patientProvider?.isEnabled = true
                binding?.buttonOk?.show()
                binding?.buttonOk?.setOnClickListener {
                    when {
                        isAddAppt3 -> viewModel.checkNetworkState()

                        args.patientId >= 0 -> globalDestinations.goToCreatePatient(
                            fragment = this@CurbsideConfirmPatientInfoFragment,
                            patientId = args.patientId,
                            providerId = appointmentViewModel.appointmentCreation.providerId ?: -1
                        )

                        else -> globalDestinations.goToCreatePatient(
                            fragment = this@CurbsideConfirmPatientInfoFragment
                        )
                    }
                }
            }
        }
    }

    /**
     * Setup UI based on incoming Patient and list of providers
     */
    private fun setupUi(patient: Patient?, providers: List<Provider>) {
        endLoading()
        setupProviders(providers)
        binding?.apply {
            buttonOk.isEnabled = true
            if (patient != null) {
                setupPatientDisplay(patient)
            } else {
                val newPatient = appointmentViewModel.appointmentCreation.newPatient
                if (newPatient != null) {
                    patientName.text = requireContext().formatString(
                        R.string.patient_name_display,
                        newPatient.firstName.captureName(),
                        newPatient.lastName.captureName()
                    )
                    patientDob.text = newPatient.dob.toLocalDateString()
                    patientGender.text = if (newPatient.gender == 0) "Male" else "Female"
                    patientRace.text = newPatient.race?.value ?: ""
                    patientEthnicity.text = newPatient.ethnicity?.value ?: ""
                    patientPhone.text = newPatient.phoneNumber
                    patientAddress.text = newPatient.getFormatAddress(requireContext())
                    appointmentViewModel.appointmentCreation.payer?.let {
                        val insuranceName = when (it.id) {
                            Payer.PayerType.UNINSURED.id -> resources.getString(
                                R.string.patient_add_select_payer_uninsured
                            )

                            Payer.PayerType.OTHER.id -> resources.getString(
                                R.string.patient_add_select_payer_other_payer
                            )

                            Payer.PayerType.SELF.id -> resources.getString(R.string.patient_add_select_payer_self_pay)
                            else -> it.insuranceName
                        }
                        patientInsuranceProvider.text = insuranceName
                    }
                }
            }
        }
    }

    /**
     * Setup UI based on incoming list of proviers
     */
    private fun setupProviders(providers: List<Provider>) {
        val provider = if (appointmentViewModel.appointmentCreation.providerId != null) {
            providers.find { it.id == appointmentViewModel.appointmentCreation.providerId }
        } else {
            if (providers.isNotEmpty()) providers[0] else null
        }

        if (provider != null) {
            appointmentViewModel.appointmentCreation.providerId = provider.id
            binding?.patientProvider?.text = requireContext().formatString(
                R.string.provider_name_display,
                provider.firstName.captureName(),
                provider.lastName.captureName()
            )
        }

        binding?.patientProvider?.setOnClickListener {
            val selectedIndex = providers.indexOfFirst {
                it.id == appointmentViewModel.appointmentCreation.providerId
            }
            val bottomDialog = BottomDialog.newInstance(
                requireContext().getString(R.string.patient_confirm_parent_provider),
                providers.map {
                    requireContext().formatString(
                        R.string.provider_name_display,
                        it.firstName.captureName(),
                        it.lastName.captureName()
                    )
                },
                selectedIndex
            )
            bottomDialog.onSelected = { index ->
                val selectedProvider = providers[index]
                appointmentViewModel.appointmentCreation.providerId = selectedProvider.id
                binding?.patientProvider?.text = requireContext().formatString(
                    R.string.provider_name_display,
                    selectedProvider.firstName.captureName(),
                    selectedProvider.lastName.captureName()
                )
            }
            bottomDialog.show(
                childFragmentManager,
                "providerBottomDialog"
            )
        }
    }

    private fun goToCreateAppointment() {
        endLoading()
        viewModel.resetState()
        destination.goToCurbsideCreateAppointment(
            fragment = this@CurbsideConfirmPatientInfoFragment,
            patientId = args.patientId,
            providerId = appointmentViewModel.appointmentCreation.providerId
                ?: -1
        )
    }

    private fun goBackWithOfflineError() {
        val data = mapOf<String, Any>(
            "appointmentListDate" to LocalDate.now(),
            "isOffline" to true
        )
        globalDestinations.goBack(
            this@CurbsideConfirmPatientInfoFragment,
            data
        )
    }

    private fun FragmentCurbsideConfirmPatientInfoBinding.setupPatientDisplay(patient: Patient) {
        patientName.text = requireContext().formatString(
            R.string.patient_name_display,
            patient.firstName.captureName(),
            patient.lastName.captureName()
        )
        patientDob.text = patient.getDobString()
        patientGender.text = patient.gender
        patientRace.text =
            if (patient.race != null) Race.fromString(patient.race)?.value else ""
        patientEthnicity.text =
            if (patient.ethnicity != null) Ethnicity.fromString(patient.ethnicity)?.value else ""
        patientPhone.text = patient.phoneNumber
        patientAddress.text = patient.getFormatAddress(requireContext())
        patientInsuranceProvider.text = patient.paymentInformation?.insuranceName
    }
}
