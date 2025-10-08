/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient.edit

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.work.WorkManager
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.PinInAttempt
import com.vaxcare.core.report.model.PinInStatus
import com.vaxcare.core.report.model.PinningStatus
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentPatientPinLockBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.model.enums.NoInsuranceCardFlow
import com.vaxcare.vaxhub.model.metric.PhoneNumberWorkflowPresented
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.navigation.PhoneCollectDestination
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.PatientEditPinLockViewModel
import com.vaxcare.vaxhub.worker.HiltWorkManagerListener
import com.vaxcare.vaxhub.worker.OneTimeParams
import com.vaxcare.vaxhub.worker.OneTimeWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PatientEditPinLockFragment : BaseFragment<FragmentPatientPinLockBinding>() {
    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: PhoneCollectDestination

    @Inject
    lateinit var hiltWorkManagerListener: HiltWorkManagerListener

    private val args: PatientEditPinLockFragmentArgs by navArgs()
    private val viewModel: PatientEditPinLockViewModel by viewModels()
    private val appointmentViewModel: AppointmentViewModel by activityViewModels()

    companion object {
        const val CONTEXT_INSURANCE_CARD = "Insurance Card"
        const val CONTEXT_PAYMENT = "Payment"
    }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_patient_pin_lock,
        hasToolbar = false
    )

    override fun handleBack(): Boolean = true

    override fun bindFragment(container: View): FragmentPatientPinLockBinding =
        FragmentPatientPinLockBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PatientEditPinLockViewModel.PinLockState.PinInFailure -> showPinError(state.pinUsed)
                is PatientEditPinLockViewModel.PinLockState.PinInSuccessNewPatient -> pinInWithNewPatient(
                    state.user,
                    state.pinUsed
                )
                is PatientEditPinLockViewModel.PinLockState.PinInSuccessExistingPatient -> pinInWithExistingPatient(
                    state.user,
                    state.appointment,
                    state.pinUsed
                )
                is PatientEditPinLockViewModel.PinLockState.PinInSuccessPayment -> pinInWithPayment(
                    state.user,
                    state.data,
                    state.pinUsed
                )
                is PatientEditPinLockViewModel.PinLockState.PinInSuccessAbort -> pinInAbort(
                    state.user,
                    state.pinUsed
                )
            }
        }

        binding?.lockKeypad?.onCompletion = { pin ->
            startLoading()
            analytics.saveMetric(
                PinInAttempt()
            )

            viewModel.loginUser(pin, args.data)
        }
    }

    private fun showPinError(pinUsed: String) {
        endLoading()

        postAnalyticsPinInStatus(PinningStatus.FAIL, pinUsed)

        binding?.lockKeypad?.showError()
    }

    private fun pinInWithExistingPatient(
        user: User,
        appointment: Appointment?,
        pinUsed: String
    ) {
        finalizeLogin(user, pinUsed)
        viewModel.resetState()
        binding?.lockKeypad?.clearInput()
        binding?.abortIcon?.setOnSingleClickListener {
            /*
             * There was a problem when the dev env was constantly returning 500, the apk would be
             * softlocked at this point. Adding this icon and home button is a workaround for the
             * user in case the api is just down
             */
            globalDestinations.goBackToSplash(this@PatientEditPinLockFragment)
        }

        binding?.abortIcon?.show()
        appointment?.let {
            if (args.data.updatePatientData == null) {
                globalDestinations.startCheckoutWithCollectPhone(
                    this@PatientEditPinLockFragment,
                    it,
                    args.data
                )
            } else {
                destination.goToUpdatePatientFragment(
                    fragment = this@PatientEditPinLockFragment,
                    data = args.data
                )
            }
        } ?: run {
            binding?.lockKeypad?.apply {
                val err = findViewById<TextView>(R.id.error_label)
                err.text = context.getString(R.string.error_loading_appointment)
                showError()
            }
        }
    }

    private fun pinInWithPayment(
        user: User,
        collectData: PatientCollectData,
        pinUsed: String
    ) {
        finalizeLogin(user, pinUsed)

        destination.goBackToPatientNoCard(
            this@PatientEditPinLockFragment,
            mapOf(AppointmentViewModel.PHONE_FLOW to collectData)
        )
    }

    private fun pinInWithNewPatient(user: User, pinUsed: String) {
        finalizeLogin(user, pinUsed)

        completeFlowWithNewPatient()
    }

    private fun pinInAbort(user: User, pinUsed: String) {
        finalizeLogin(user, pinUsed)

        destination.goBackToPatientNoCard(this@PatientEditPinLockFragment)
    }

    private fun finalizeLogin(user: User, pinUsed: String) {
        binding?.lockKeypad?.hideError()
        endLoading()

        postAnalyticsPinInStatus(PinningStatus.SUCCESS, pinUsed)

        // overwrite the user and stage analytics
        analytics.updateUserData(
            user.userId,
            user.userName.orEmpty(),
            user.fullName
        )

        context?.let {
            OneTimeWorker.buildOneTimeUniqueWorker(
                wm = WorkManager.getInstance(it),
                parameters = OneTimeParams.PingJob,
                listener = hiltWorkManagerListener
            )
        }
        postAnalyticsForPhoneNumberWorkflow()
    }

    private fun completeFlowWithNewPatient() {
        // checkout appointment flow - new patient so go to the PatientUpdateFragment
        args.data.currentPhone?.let { newPhone ->
            appointmentViewModel.appointmentCreation.newPatient?.phoneNumber =
                newPhone
        }

        endLoading()
        globalDestinations.goToCreatePatient(
            fragment = this@PatientEditPinLockFragment,
            patientCollectData = args.data
        )
    }

    private fun postAnalyticsForPhoneNumberWorkflow() {
        args.data.let {
            val phoneNumberContext = when (it.flow) {
                NoInsuranceCardFlow.CREATE_PATIENT,
                NoInsuranceCardFlow.CHECKOUT_PATIENT,
                NoInsuranceCardFlow.EDIT_PATIENT -> CONTEXT_INSURANCE_CARD
                NoInsuranceCardFlow.COPAY_PAYMENT,
                NoInsuranceCardFlow.CHECKOUT_PAYMENT -> CONTEXT_PAYMENT
                else -> null
            }
            phoneNumberContext?.let { pnc ->
                analytics.popMetricTimer(PhoneNumberWorkflowPresented.PHONE_COLLECTION_WORKFLOW)
                    ?.let { duration ->
                        val metric = PhoneNumberWorkflowPresented(
                            visitId = it.appointmentId,
                            phoneNumberPrefilled = it.phoneNumberPrefilled,
                            phoneNumberUpdated = it.phoneNumberUpdated,
                            phoneContactAgreement = it.phoneContactAgreement,
                            phoneNumberContext = pnc,
                            phoneFlowDurationSeconds = duration.toInt()
                        )
                        analytics.saveMetric(
                            metric
                        )
                    }
            }
        }
    }

    private fun postAnalyticsPinInStatus(pinInStatus: PinningStatus, pinUsed: String) {
        analytics.saveMetric(
            PinInStatus(pinInStatus, pinUsed)
        )
    }
}
