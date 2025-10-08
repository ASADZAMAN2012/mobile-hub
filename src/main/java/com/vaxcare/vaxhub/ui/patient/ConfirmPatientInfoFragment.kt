/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.ScreenNavigationMetric
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.Receivers
import com.vaxcare.vaxhub.core.extension.captureName
import com.vaxcare.vaxhub.core.extension.formatPhoneNumber
import com.vaxcare.vaxhub.core.extension.maskSSNIfHasNineDigits
import com.vaxcare.vaxhub.core.extension.registerBroadcastReceiver
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.extension.toLocalDateString
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.databinding.FragmentConfirmPatientInfoBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.metric.CreatePatientClickMetric
import com.vaxcare.vaxhub.model.metric.NewPatientAndAppointmentCreatedMetric
import com.vaxcare.vaxhub.model.metric.TroubleConnectingDialogClickMetric
import com.vaxcare.vaxhub.ui.dialog.TroubleConnectingDialog
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.ConfirmPatientInfoUIState
import com.vaxcare.vaxhub.viewmodel.ConfirmPatientInfoViewModel
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddPatientSharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ConfirmPatientInfoFragment : BaseFragment<FragmentConfirmPatientInfoBinding>() {
    companion object {
        const val METRIC_SCREEN_NAME = "Confirm Patient Info"
    }

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    private val viewModel: ConfirmPatientInfoViewModel by viewModels()

    private val addPatientSharedViewModel: AddPatientSharedViewModel by activityViewModels()

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_confirm_patient_info,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentConfirmPatientInfoBinding =
        FragmentConfirmPatientInfoBinding.bind(container)

    override fun init(view: View, savedInstanceState: Bundle?) {
        viewModel.saveMetric(
            ScreenNavigationMetric(
                screenName = METRIC_SCREEN_NAME
            )
        )

        binding?.populateViewsAndSetUpClickListener()

        observeUIState()
    }

    private fun FragmentConfirmPatientInfoBinding.populateViewsAndSetUpClickListener() {
        addPatientSharedViewModel.buildAndGetNewPatient()?.let { newPatient ->
            textviewName.text = getString(
                R.string.patient_name_display,
                newPatient.firstName.captureName(),
                newPatient.lastName.captureName()
            )
            textviewDateOfBirthValue.text = newPatient.dob.toLocalDateString("MM/dd/yyyy")
            textviewPhoneValue.text = newPatient.phoneNumber.formatPhoneNumber()
            textviewGenderValue.text = getString(newPatient.gender.getGenderStringRes())
            newPatient.getFormatAddress(this@ConfirmPatientInfoFragment.requireContext())
            textviewInsuranceProviderValue.text = newPatient.paymentInformation?.insuranceName ?: ""
            viewModel.currentProvider.observe(this@ConfirmPatientInfoFragment) { provider ->
                provider?.let {
                    textviewProviderValue.text = getString(
                        R.string.provider_name_display,
                        it.firstName.captureName(),
                        it.lastName.captureName()
                    )
                }
            }
            if (newPatient.getFormatAddress(requireContext()) == getString(R.string.patient_confirm_no_address)) {
                textviewAddress.visibility = View.GONE
                textviewAddressValue.visibility = View.GONE
            } else {
                textviewAddressValue.text = newPatient.getFormatAddress(requireContext())
            }

            when {
                !newPatient.paymentInformation?.mbi.isNullOrBlank() -> {
                    textviewMbialue.text = newPatient.paymentInformation?.mbi
                    textviewMbi.visibility = View.VISIBLE
                    textviewMbialue.visibility = View.VISIBLE
                }

                !newPatient.ssn.isNullOrBlank() -> {
                    textviewSsnValue.text = newPatient.ssn.maskSSNIfHasNineDigits()
                    textviewSsn.visibility = View.VISIBLE
                    textviewSsnValue.visibility = View.VISIBLE
                }
            }
        }

        buttonCreatePatient.setOnClickListener {
            val newPatient = addPatientSharedViewModel.buildAndGetNewPatient()

            newPatient?.let {
                viewModel.createAppointmentWithNewPatientAndWaitForACE(it)
                viewModel.saveMetric(
                    CreatePatientClickMetric(
                        isMBIAvailable = !it.paymentInformation?.mbi.isNullOrEmpty(),
                        isSSNAvailable = !it.ssn.isNullOrEmpty()
                    )
                )
            } ?: Toast.makeText(context, "Could not create patient", Toast.LENGTH_SHORT)
                .apply { this.show() }
        }

        imageViewEdit.setOnSingleClickListener {
            viewModel.retrieveProviders()
        }
    }

    @StringRes
    fun Int.getGenderStringRes(): Int = if (this == 0) R.string.male else R.string.female

    private fun observeUIState() {
        viewModel.state.observe(viewLifecycleOwner) { uIState ->
            Timber.d("State incoming $uIState")

            if (uIState !is ConfirmPatientInfoUIState.Loading &&
                uIState !is ConfirmPatientInfoUIState.ListenForAppointmentChangedEvent &&
                uIState !is ConfirmPatientInfoUIState.NavigateToCheckoutPatientWithEligibilityCheckReceived &&
                uIState !is ConfirmPatientInfoUIState.NavigateToCheckoutPatientAsRiskFree
            ) {
                stopLoadingAnimation()
            }

            when (uIState) {
                ConfirmPatientInfoUIState.Init -> Unit

                ConfirmPatientInfoUIState.NoInternetConnectivity -> {
                    stopLoadingAnimation()
                    subscribeAndShowTroubleConnectingDialog()
                }

                is ConfirmPatientInfoUIState.NavigateToCheckoutPatientWithEligibilityCheckReceived -> {
                    viewModel.saveMetric(
                        NewPatientAndAppointmentCreatedMetric(
                            patientVisitId = uIState.appointmentId,
                            wasForcedRiskFree = false
                        )
                    )
                    addPatientSharedViewModel.clearData()
                    globalDestinations.goToCheckout(
                        fragment = this,
                        appointmentId = uIState.appointmentId,
                        isLocallyCreated = true,
                    )
                }

                is ConfirmPatientInfoUIState.NavigateToCheckoutPatientAsRiskFree -> {
                    viewModel.saveMetric(
                        NewPatientAndAppointmentCreatedMetric(
                            patientVisitId = uIState.appointmentId,
                            wasForcedRiskFree = true
                        )
                    )
                    addPatientSharedViewModel.clearData()
                    globalDestinations.goToCheckout(
                        fragment = this,
                        appointmentId = uIState.appointmentId,
                        isForceRiskFree = true,
                        isLocallyCreated = true
                    )
                }

                ConfirmPatientInfoUIState.NoProvidersFoundError -> {
                    val toast = Toast.makeText(
                        context,
                        "Error, not providers found, call customer support",
                        Toast.LENGTH_LONG
                    )
                    toast.show()
                }

                is ConfirmPatientInfoUIState.Loading -> {
                    startLoadingAnimation()
                    uIState.loadingMessage?.let {
                        binding?.textViewLoadingMessage?.text = getString(it)
                    }
                }

                is ConfirmPatientInfoUIState.SelectAProvider -> {
                    val bottomDialog = BottomDialog.newInstance(
                        title = getString(R.string.patient_confirm_parent_provider),
                        values = uIState.providers.map {
                            getString(
                                R.string.provider_name_display,
                                it.firstName.captureName(),
                                it.lastName.captureName()
                            )
                        },
                        selectedIndex = uIState.currentProvider?.let {
                            uIState.providers.getIndexOfCurrentProvider(it)
                        } ?: 0
                    )

                    bottomDialog.onSelected = { index ->
                        val selectedProvider = uIState.providers[index]
                        viewModel.updateCurrentProvider(selectedProvider)
                    }

                    bottomDialog.show(
                        (activity as FragmentActivity).supportFragmentManager,
                        "stateBottomDialog"
                    )
                }

                is ConfirmPatientInfoUIState.ListenForAppointmentChangedEvent ->
                    registerAppointmentChangedEventReceiver(uIState.appointmentId)
            }
        }
    }

    private fun List<Provider>.getIndexOfCurrentProvider(currentProvider: Provider): Int =
        try {
            this.indexOf(currentProvider)
        } catch (e: Exception) {
            Timber.e(e, "Current provider not found in list of providers $this")
            0
        }

    private fun startLoadingAnimation() {
        binding?.apply {
            constraintLayoutLoading.visibility = View.VISIBLE
            (imageViewLoading.drawable as? AnimatedImageDrawable)?.start()
        }
    }

    private fun stopLoadingAnimation() {
        binding?.apply {
            constraintLayoutLoading.visibility = View.GONE
        }
    }

    private fun subscribeAndShowTroubleConnectingDialog() {
        setFragmentResultListener(TroubleConnectingDialog.REQUEST_KEY) { _, listener ->
            when (listener.getInt(TroubleConnectingDialog.OPTION_SELECTED_BUNDLE_KEY)) {
                TroubleConnectingDialog.Option.TRY_AGAIN.ordinal -> {
                    addPatientSharedViewModel.buildAndGetNewPatient()?.let {
                        viewModel.createAppointmentWithNewPatientAndWaitForACE(it)
                    } ?: Toast.makeText(context, "Could not create patient", Toast.LENGTH_SHORT)
                        .apply { this.show() }
                    viewModel.saveMetric(TroubleConnectingDialogClickMetric(TroubleConnectingDialog.Option.TRY_AGAIN))
                }

                TroubleConnectingDialog.Option.OK.ordinal -> {
                    viewModel.saveMetric(TroubleConnectingDialogClickMetric(TroubleConnectingDialog.Option.OK))
                }
            }
        }

        globalDestinations.goToTroubleConnectingDialog(
            this,
            R.string.trouble_connecting_dialog_create_patient
        )
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerAppointmentChangedEventReceiver(currentAppointmentId: Int) {
        val appointmentChangedEventReceiver =
            getAppointmentChangedEventReceiver(currentAppointmentId)
        requireContext().registerBroadcastReceiver(
            receiver = appointmentChangedEventReceiver,
            intentFilter = IntentFilter(Receivers.ACE_ACTION)
        )
    }

    private fun getAppointmentChangedEventReceiver(currentAppointmentId: Int): BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    if (viewModel.isExpectedAppointmentChangeEvent(intent, currentAppointmentId)) {
                        Timber.d("isExpectedAppointmentChangeEvent true")
                        viewModel.stopTimeoutAndNotifyToNavigateToCheckoutPatient(
                            currentAppointmentId
                        )
                    } else {
                        Timber.d("isExpectedAppointmentChangeEvent false")
                        // Do nothing
                    }
                }
            }
        }
}
