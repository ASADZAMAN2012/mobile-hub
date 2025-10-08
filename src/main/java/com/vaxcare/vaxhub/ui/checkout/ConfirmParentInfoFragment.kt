/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.model.enums.AddPatientSource
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.databinding.FragmentConfirmParentInfoBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.PatientPostBody
import com.vaxcare.vaxhub.repository.ProviderRepository
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.ConfirmParentInfoViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmParentInfoFragment : BaseFragment<FragmentConfirmParentInfoBinding>() {
    private val viewModel: ConfirmParentInfoViewModel by viewModels()
    private val appointmentViewModel: AppointmentViewModel by activityViewModels()

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var providerRepository: ProviderRepository

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_confirm_parent_info,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentConfirmParentInfoBinding =
        FragmentConfirmParentInfoBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        val appointmentId = requireNotNull(arguments?.getInt(AppointmentViewModel.APPOINTMENT_ID))

        viewModel.getAppointment(appointmentId)

        viewModel.appointment.observe(viewLifecycleOwner) { appointment ->
            if (appointment == null) return@observe

            val patient = appointment.patient
            val payment = patient.paymentInformation ?: return@observe

            binding?.patientName?.text = requireContext().formatString(
                R.string.patient_name_display,
                payment.insuredFirstName?.captureName() ?: "",
                payment.insuredLastName?.captureName() ?: ""
            )

            binding?.patientDob?.text = payment.getDobString() ?: ""
            binding?.patientGender?.text = payment.insuredGender
            binding?.patientPhone?.text = patient.phoneNumber
            binding?.patientAddress?.text = patient.getFormatAddress(requireContext())
            binding?.patientInsuranceProvider?.text = payment.insuranceName
            binding?.patientProvider?.text = requireContext().formatString(
                R.string.provider_name_display,
                appointment.provider.firstName.captureName(),
                appointment.provider.lastName.captureName()
            )
            appointmentViewModel.appointmentCreation.providerId = appointment.provider.id

            providerRepository.getAll()
                .observe(viewLifecycleOwner) { providers ->
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
                            appointmentViewModel.appointmentCreation.providerId =
                                selectedProvider.id
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

            binding?.buttonOk?.setOnClickListener {
                appointmentViewModel.appointmentCreation.patientId = null
                appointmentViewModel.appointmentCreation.newPatient =
                    PatientPostBody.NewPatient(
                        firstName = payment.insuredFirstName ?: "",
                        lastName = payment.insuredLastName ?: "",
                        dob = payment.getDob() ?: LocalDate.now(),
                        gender = if (payment.insuredGender == "Male") 0 else 1,
                        phoneNumber = requireNotNull(patient.phoneNumber),
                        address1 = patient.address1,
                        address2 = patient.address2,
                        city = patient.city,
                        state = patient.state,
                        zip = patient.zip
                    )

                appointmentViewModel.appointmentCreation.newPatient?.paymentInformation =
                    appointmentViewModel.getParentPaymentInfoSameAsChild(payment)

                globalDestinations.goToCreatePatient(this@ConfirmParentInfoFragment)
            }

            binding?.buttonCancel?.setOnClickListener {
                globalDestinations.goToCurbsideAddPatient(
                    fragment = this@ConfirmParentInfoFragment,
                    appointmentId = appointment.id,
                    addPatientSource = AddPatientSource.ADD_SUGGEST_PARENT_PATIENT.ordinal
                )
            }
        }
    }
}
