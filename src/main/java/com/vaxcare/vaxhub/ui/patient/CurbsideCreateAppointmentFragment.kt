/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.ScreenNavigationMetric
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentCurbsideCreateAppointmentBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogButton
import com.vaxcare.vaxhub.ui.dialog.TroubleConnectingDialog
import com.vaxcare.vaxhub.ui.navigation.CreateAppointmentDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.CreateAppointmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class CurbsideCreateAppointmentFragment : BaseFragment<FragmentCurbsideCreateAppointmentBinding>() {
    companion object {
        private const val METRIC_SCREEN_NAME = "Curbside Create Appointment"
    }

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: CreateAppointmentDestination
    private val args: CurbsideCreateAppointmentFragmentArgs by navArgs()
    private val viewModel: CreateAppointmentViewModel by viewModels()
    private var appointmentId: Int? = null
    private var isSuccess = false
    private var timeoutReached = false

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_curbside_create_appointment,
        hasToolbar = false
    )

    override fun handleBack(): Boolean = true

    override fun bindFragment(container: View): FragmentCurbsideCreateAppointmentBinding =
        FragmentCurbsideCreateAppointmentBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        analytics.saveMetric(
            ScreenNavigationMetric(
                screenName = METRIC_SCREEN_NAME
            )
        )

        (binding?.loading?.drawable as? AnimatedImageDrawable)?.start()

        if (!networkMonitor.isCurrentlyOnline()) {
            navigateBack()
        } else {
            viewModel.createAppointment(args.patientId, args.providerId)
        }

        viewModel.appointmentLiveData.observe(viewLifecycleOwner) { appointment ->
            // Only observe changes until timeout is reached
            // after timeout, we will navigate through state changes
            if (!timeoutReached && appointment?.encounterState?.messages?.isNotEmpty() == true) {
                isSuccess = true // prevent race condition if we get results right at timeout
                navigateToCheckout(appointment)
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is CreateAppointmentViewModel.CreateAppointmentState.UpdateUI -> {
                    binding?.apply {
                        topText.text = getString(R.string.add_appointment_delay_message)
                        bottomText.hide()
                    }
                }

                is CreateAppointmentViewModel.CreateAppointmentState.CreationSuccess -> {
                    viewModel.setAppointmentId(state.appointmentId)
                    appointmentId = state.appointmentId
                }

                is CreateAppointmentViewModel.CreateAppointmentState.CreationFailed -> {
                    displayCreationFailedDialog()
                }

                is CreateAppointmentViewModel.CreateAppointmentState.TimeoutReached -> {
                    timeoutReached = true
                    if (appointmentId == null) {
                        displayCreationFailedDialog()
                    }
                }

                is CreateAppointmentViewModel.CreateAppointmentState.TimeoutAppointment -> {
                    if (!isSuccess) {
                        state.appointment?.let {
                            navigateToCheckout(state.appointment, state.setRiskFree)
                        } ?: run {
                            displayCreationFailedDialog()
                        }
                    }
                }
            }
        }
    }

    private fun displayCreationFailedDialog() {
        getResultLiveData<ErrorDialogButton>(ErrorDialog.RESULT)?.observe(viewLifecycleOwner) {
            when (it) {
                ErrorDialogButton.PRIMARY_BUTTON -> {
                    viewModel.saveRetryMetric(TroubleConnectingDialog.Option.TRY_AGAIN)
                    retryOrWaitForAppointmentCreation()
                }
                else -> {
                    viewModel.saveRetryMetric(TroubleConnectingDialog.Option.OK)
                    navigateBack()
                }
            }

            removeResult<ErrorDialogButton>(ErrorDialog.RESULT)
        }

        globalDestinations.goToErrorDialog(
            fragment = this@CurbsideCreateAppointmentFragment,
            title = R.string.trouble_connecting_dialog_title,
            body = R.string.trouble_connecting_dialog_create_patient,
            primaryBtn = R.string.trouble_connecting_dialog_try_again,
            secondaryBtn = R.string.button_ok
        )
    }

    private fun retryOrWaitForAppointmentCreation() {
        timeoutReached = false
        appointmentId?.let { viewModel.waitLongerForAppointment(it) } ?: run {
            viewModel.createAppointment(args.patientId, args.providerId)
        }
    }

    private fun navigateToCheckout(appointment: Appointment, isForceRiskFree: Boolean = false) {
        globalDestinations.startCheckout(
            fragment = this@CurbsideCreateAppointmentFragment,
            appointment = appointment,
            analytics = analytics,
            isForceRiskFree = isForceRiskFree,
            isLocallyCreated = true
        )
    }

    private fun navigateBack() {
        destination.goBackToAddAppointment(this@CurbsideCreateAppointmentFragment, LocalDate.now())
    }
}
