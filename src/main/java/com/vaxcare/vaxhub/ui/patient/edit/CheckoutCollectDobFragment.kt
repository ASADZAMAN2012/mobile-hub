/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/
package com.vaxcare.vaxhub.ui.patient.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.hide
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.toMillis
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentCheckoutCollectDobBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.enums.NetworkStatus
import com.vaxcare.vaxhub.model.metric.CheckoutFinishMetric
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.CheckoutCollectDoBViewModel
import com.vaxcare.vaxhub.viewmodel.LoadingState
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutCollectDobFragment : BaseFragment<FragmentCheckoutCollectDobBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var globalDestinations: GlobalDestinations
    private val viewModel: CheckoutCollectDoBViewModel by viewModels()
    private val appointmentViewModel: AppointmentViewModel by activityViewModels()

    private val args: CheckoutCollectDobFragmentArgs by navArgs()
    private val appointmentId by lazy { args.appointmentId }
    private val isForceRiskFree by lazy { args.isForceRiskFree }
    private val isLocallyCreated by lazy { args.isLocallyCreated }
    private val patientId by lazy { args.patientId }
    private var appointment: Appointment? = null
    private var enteredDob: LocalDate? = null

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_checkout_collect_dob,
        hasMenu = false,
        hasToolbar = false
    )

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.topBar?.setTitle(resources.getString(R.string.patient_edit_title))
        binding?.dateOfBirth?.apply {
            onPatientInfoChanged = {
                binding?.fabNext?.isEnabled = isValid()
            }
            addTextChangedListener()
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                LoadingState -> showLoading()
                is CheckoutCollectDoBViewModel.CollectDobState.ErrorGettingAppointment -> Unit
                is CheckoutCollectDoBViewModel.CollectDobState.AppointmentLoaded ->
                    setupUI(state.appointment)
                is CheckoutCollectDoBViewModel.CollectDobState.UpdatePatientSubmitted -> Unit
                is CheckoutCollectDoBViewModel.CollectDobState.UpdateFailed,
                CheckoutCollectDoBViewModel.CollectDobState.TimeOutReached -> navigateForward()
                is CheckoutCollectDoBViewModel.CollectDobState.AppointmentAbandoned -> {
                    appointmentViewModel.clearCurrentCheckout()
                    globalDestinations.goBackToAppointmentList(this)
                }
                else -> Unit
            }
        }

        binding?.fabNext?.setOnSingleClickListener {
            enteredDob = binding?.dateOfBirth?.getDob()
            viewModel.updatePatientData(appointment, binding?.dateOfBirth?.getDob())
            showLoading()
        }

        viewModel.fetchAppointment(appointmentId)
    }

    private fun showLoading() {
        binding?.root?.hide()
        startLoading()
    }

    private fun hideLoading() {
        endLoading()
        binding?.root?.show()
    }

    private fun setupUI(appointment: Appointment) {
        hideLoading()
        this.appointment = appointment
        try {
            appointment.patient.getDobString()?.let {
                binding?.dateOfBirth?.setDob(
                    LocalDate.parse(it, DateTimeFormatter.ofPattern("M/dd/yyyy"))
                )
            }
        } catch (exception: Exception) {
            Timber.e(
                exception,
                "Error parsing date of birth: ${appointment.patient.getDobString()}"
            )
        }

        binding?.topBar?.onCloseAction = { onClose() }
        viewModel.appointmentLiveData.observe(viewLifecycleOwner) { appointment ->
            if (appointment?.patient?.dob?.isNotEmpty() == true) {
                navigateForward()
            }
        }
    }

    private fun onClose() {
        appointment?.let { appointment ->
            analytics.saveMetric(
                CheckoutFinishMetric(
                    visitId = appointmentId,
                    doseCount = 0,
                    isCheckedOut = appointment.checkedOut,
                    paymentMethod = appointment.paymentMethod,
                    duration = LocalDateTime.now()
                        .toMillis() - appointment.appointmentTime.toMillis(),
                    result = CheckoutFinishMetric.CheckoutResult.ABANDONED,
                    missingInfoCaptured = false,
                    networkStatus = networkMonitor.networkStatus.value
                        ?: NetworkStatus.DISCONNECTED,
                    relativeDoS = appointment.getRelativeDoS(),
                    paymentType = "N/A",
                    showedRiskFree = isForceRiskFree
                )
            )
        }
        if (isLocallyCreated && appointment?.checkedOut != true) {
            viewModel.abandonAppointment(appointmentId)
        } else {
            appointmentViewModel.clearCurrentCheckout()
            globalDestinations.goBackToAppointmentList(this)
        }
    }

    private fun navigateForward() {
        appointmentViewModel.currentCheckout.manualDob = enteredDob
        globalDestinations.goToCheckout(
            this@CheckoutCollectDobFragment,
            appointmentId = appointmentId,
            isForceRiskFree = isForceRiskFree,
            isLocallyCreated = isLocallyCreated,
            updateData = PatientCollectData(
                appointmentId = appointmentId,
                patientId = patientId
            )
        )
    }

    override fun bindFragment(container: View): FragmentCheckoutCollectDobBinding =
        FragmentCheckoutCollectDobBinding.bind(container)
}
