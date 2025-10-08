/**************************************************************************************************
 * Copyright VaxCare (c) 2024.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.appointment.add

import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.report.model.ScreenNavigationMetric
import com.vaxcare.core.ui.extension.hide
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentAddAppointmentOrCreatePatientBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.metric.TroubleConnectingDialogClickMetric
import com.vaxcare.vaxhub.ui.checkout.adapter.AddAppointmentResultAdapter
import com.vaxcare.vaxhub.ui.dialog.TroubleConnectingDialog
import com.vaxcare.vaxhub.ui.navigation.AddPatientsDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.patient.KeepDeviceInHandsDialog
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddAppointmentOrCreatePatientUIState
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddAppointmentOrCreatePatientUIState.Init
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddAppointmentOrCreatePatientUIState.Loading
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddAppointmentOrCreatePatientUIState.NoPatientsFound
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddAppointmentOrCreatePatientUIState.NoVaxCareConnectivity
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddAppointmentOrCreatePatientUIState.NotifyNotHandDeviceToPatient
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddAppointmentOrCreatePatientUIState.PatientsFound
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddAppointmentOrCreatePatientViewModel
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddPatientSharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
open class AddAppointmentOrCreatePatientFragment :
    BaseFragment<FragmentAddAppointmentOrCreatePatientBinding>() {
    companion object {
        private const val SCREEN_NAME = "AddAppointmentOrCreatePatient"
    }

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var addPatientsDestination: AddPatientsDestination

    private val viewModel: AddAppointmentOrCreatePatientViewModel by viewModels()
    private val sharedViewModel: AddPatientSharedViewModel by activityViewModels()

    private val adapter: AddAppointmentResultAdapter by lazy {
        AddAppointmentResultAdapter()
    }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_add_appointment_or_create_patient,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentAddAppointmentOrCreatePatientBinding =
        FragmentAddAppointmentOrCreatePatientBinding.bind(container)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun init(view: View, savedInstanceState: Bundle?) {
        viewModel.saveMetric(ScreenNavigationMetric(SCREEN_NAME))
        sharedViewModel.clearData()

        binding?.apply {
            subscribeToUIState()

            vaxToolbar.onCloseAction = {
                globalDestinations.goBackToAppointmentList(this@AddAppointmentOrCreatePatientFragment)
            }

            recyclerViewPatientsFound.adapter = adapter

            adapter.onAppointmentSelected = { appointment ->
                startCheckout(appointment)
            }

            adapter.onAddAppointment = { patient ->
                startCreateAppointmentWithPatientId(patient.id)
            }

            buttonCreateNewPatient.setOnClickListener {
                viewModel.createNewPatient()
            }

            editTextSearchAppointment.textChanges()
                .debounce(500L)
                .onEach { charSequence ->
                    val keyword = charSequence?.toString() ?: ""
                    adapter.keyword = keyword
                    viewModel.findPatientsWith(keyword)
                }.launchIn(lifecycleScope)
        }
    }

    private fun startCheckout(appointment: Appointment) {
        when {
            !appointment.checkedOut -> viewModel.onUncheckedAppointmentSelected(appointment)

            // Edit Past Checkout
            appointment.isEditable ?: false -> globalDestinations.goToCheckout(
                this@AddAppointmentOrCreatePatientFragment,
                appointmentId = appointment.id
            )

            // View Only - Past Checkout
            else -> globalDestinations.goToCheckoutSummary(
                this@AddAppointmentOrCreatePatientFragment,
                appointmentId = appointment.id
            )
        }
    }

    private fun startCreateAppointmentWithPatientId(patientId: Int) {
        globalDestinations.goToCurbsideConfirmPatientInfo(
            fragment = this@AddAppointmentOrCreatePatientFragment,
            patientId = patientId
        )
    }

    @ExperimentalCoroutinesApi
    fun EditText.textChanges(): Flow<CharSequence?> {
        return callbackFlow {
            val listener = doOnTextChanged { text, _, _, _ ->
                trySend(text)
            }
            addTextChangedListener(listener)
            awaitClose { removeTextChangedListener(listener) }
        }.onStart { emit(text) }
    }

    private fun FragmentAddAppointmentOrCreatePatientBinding.subscribeToUIState() {
        viewModel.uiState.observe(this@AddAppointmentOrCreatePatientFragment) { uiState ->
            if (uiState != Loading) stopLoading()
            Timber.i(uiState.toString())

            when (uiState) {
                is Init, NoPatientsFound -> {
                    groupCreateNewPatient.visibility = View.VISIBLE
                    recyclerViewPatientsFound.visibility = View.GONE
                }

                is Loading -> onLoading()

                is NoVaxCareConnectivity -> {
                    subscribeAndShowTroubleConnectingDialog()
                }

                is PatientsFound -> {
                    groupCreateNewPatient.visibility = View.GONE
                    recyclerViewPatientsFound.visibility = View.VISIBLE
                    adapter.addAllItems(
                        uiState.listOfPatientsFound,
                        uiState.isFeaturePublicStockPilotEnabled
                    )
                    (recyclerViewPatientsFound.layoutManager as LinearLayoutManager)
                        .scrollToPosition(0)
                }

                is NotifyNotHandDeviceToPatient -> {
                    subscribeAndShowDoNotHandDeviceToPatient()
                }

                is AddAppointmentOrCreatePatientUIState.NavigateToCheckoutPatient -> {
                    globalDestinations.goToCheckout(
                        this@AddAppointmentOrCreatePatientFragment,
                        uiState.appointmentId
                    )
                    viewModel.resetUIStateToDefault()
                }

                is AddAppointmentOrCreatePatientUIState.NavigateToDoBCapture -> {
                    globalDestinations.goToDoBCollection(
                        fragment = this@AddAppointmentOrCreatePatientFragment,
                        appointmentId = uiState.appointmentId,
                        patientId = uiState.patientId
                    )
                    viewModel.resetUIStateToDefault()
                }
            }
        }
    }

    private fun subscribeAndShowDoNotHandDeviceToPatient() {
        setFragmentResultListener(KeepDeviceInHandsDialog.REQUEST_KEY) { _, _ ->
            viewModel.resetUIStateToDefault()
            navigateToAddNewPatient()
        }

        globalDestinations.goToKeepDeviceInHands(fragment = this, isAddPatientFlow = true)
    }

    private fun subscribeAndShowTroubleConnectingDialog() {
        setFragmentResultListener(TroubleConnectingDialog.REQUEST_KEY) { _, listener ->
            when (listener.getInt(TroubleConnectingDialog.OPTION_SELECTED_BUNDLE_KEY)) {
                TroubleConnectingDialog.Option.TRY_AGAIN.ordinal -> {
                    viewModel.createNewPatient()
                    viewModel.saveMetric(TroubleConnectingDialogClickMetric(TroubleConnectingDialog.Option.TRY_AGAIN))
                }

                TroubleConnectingDialog.Option.OK.ordinal -> {
                    viewModel.resetUIStateToDefault()
                    viewModel.saveMetric(TroubleConnectingDialogClickMetric(TroubleConnectingDialog.Option.OK))
                }
            }
        }

        globalDestinations.goToTroubleConnectingDialog(
            this,
            R.string.trouble_connecting_dialog_create_patient
        )
    }

    private fun onLoading() {
        binding?.apply {
            buttonCreateNewPatient.visibility = View.GONE
            frameLayoutLoading.visibility = View.VISIBLE
            (imageViewLoading.drawable as? AnimatedImageDrawable)?.start()
        }
    }

    private fun stopLoading() {
        binding?.apply {
            frameLayoutLoading.visibility = View.GONE
        }
    }

    private fun navigateToAddNewPatient() {
        binding?.constraintLayoutAddAppointment?.hide()
        startLoading()
        addPatientsDestination.navigateToAddNewPatient(this@AddAppointmentOrCreatePatientFragment)
    }
}
