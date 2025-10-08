/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewbinding.ViewBinding
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.PatientInfoViewModel
import com.vaxcare.vaxhub.viewmodel.PatientInfoViewModel.PatientInfoState

abstract class BasePayerInfoFragment<T : ViewBinding> : BaseFragment<T>() {
    protected val viewModel: PatientInfoViewModel by viewModels()
    protected val appointmentViewModel: AppointmentViewModel by activityViewModels()
    protected open val appointmentId by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment?.id
    }

    protected abstract fun setAppointmentInfo(appointment: Appointment)

    protected abstract fun setPayerInfo(payer: Payer?, appointment: Appointment)

    protected abstract fun onInfoUpdated(appointment: Appointment)

    protected abstract fun onLoading()

    protected abstract fun onError()

    private fun onAppointmentLoaded(appointment: Appointment?) {
        appointment?.let {
            setAppointmentInfo(it)
            endLoading()
        }
    }

    private fun onPayerInfoLoaded(payer: Payer?, appointment: Appointment) {
        setPayerInfo(payer, appointment)
        endLoading()
    }

    private fun onAppointmentLocallyUpdated(appointment: Appointment?) {
        appointment?.let {
            onInfoUpdated(appointment)
            endLoading()
        }
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        appointmentViewModel.currentCheckout.revertPaymentFlips()
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PatientInfoState.AppointmentLoaded -> onAppointmentLoaded(state.appointment)
                is PatientInfoState.AppointmentLocallyUpdated -> onAppointmentLocallyUpdated(state.appointment)
                is PatientInfoState.PayerInfoLoaded -> onPayerInfoLoaded(
                    payer = state.payer,
                    appointment = state.appointment
                )
                PatientInfoState.Failed -> onError()
                LoadingState -> onLoading()
            }
        }

        viewModel.getPayerInformation(appointmentId)
    }
}
