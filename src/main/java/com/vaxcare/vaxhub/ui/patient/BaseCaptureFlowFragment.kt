/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewbinding.ViewBinding
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.model.enums.AddPatientSource
import com.vaxcare.vaxhub.core.ui.BaseDialog
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.model.PaymentInformation
import com.vaxcare.vaxhub.ui.navigation.CaptureFlowDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.BaseCaptureFlowState
import com.vaxcare.vaxhub.viewmodel.BaseCaptureFlowViewModel
import com.vaxcare.vaxhub.viewmodel.LoadingState

abstract class BaseCaptureFlowFragment<VB : ViewBinding> : BaseFragment<VB>() {
    protected abstract val destination: CaptureFlowDestination
    protected abstract val globalDestinations: GlobalDestinations

    protected val appointmentViewModel: AppointmentViewModel by activityViewModels()

    private val viewModel: BaseCaptureFlowViewModel by viewModels()

    fun checkIfParentPatient(onNext: () -> Unit) {
        val addPatientSource = AddPatientSource.fromInt(
            arguments?.getInt(
                AppointmentViewModel.ADD_PATIENT_SOURCE,
                -1
            ) ?: -1
        )
        val appointmentId =
            requireNotNull(arguments?.getInt(AppointmentViewModel.APPOINTMENT_ID))

        viewModel.checkIfParentPatient(addPatientSource, appointmentId)

        viewModel.state.observe(viewLifecycleOwner) {
            if (it != LoadingState) {
                endLoading()
            }

            when (it) {
                LoadingState -> startLoading()

                BaseCaptureFlowState.NavigateToNextStep -> {
                    onNext()
                }

                BaseCaptureFlowState.DisplaySameInsuranceDialog -> {
                    getResultLiveData<Boolean>(BaseDialog.DIALOG_RESULT)?.observe(viewLifecycleOwner) { confirm ->
                        removeResult<Boolean>(BaseDialog.DIALOG_RESULT)
                        if (confirm) {
                            viewModel.getInfoToCreateAppointmentWithInsurance(
                                addPatientSource = addPatientSource,
                                appointmentId = appointmentId
                            )
                        } else {
                            onNext()
                        }
                    }
                    destination.goToSameInsuranceDialog(this@BaseCaptureFlowFragment)
                }

                is BaseCaptureFlowState.CreateAppointmentWithInsurance -> {
                    createAppointmentWithInsurance(
                        addPatientSource = it.addPatientSource,
                        paymentInformation = it.paymentInformation,
                        providerId = it.providerId
                    )
                }
            }
        }
    }

    private fun createAppointmentWithInsurance(
        addPatientSource: AddPatientSource,
        paymentInformation: PaymentInformation,
        providerId: Int
    ) {
        appointmentViewModel.appointmentCreation.newPatient?.paymentInformation =
            appointmentViewModel.getParentPaymentInfoSameAsChild(paymentInformation)

        if (addPatientSource == AddPatientSource.ADD_NEW_PARENT_PATIENT) {
            appointmentViewModel.appointmentCreation.providerId = providerId
        }

        globalDestinations.goToCreatePatient(this@BaseCaptureFlowFragment)
    }
}
