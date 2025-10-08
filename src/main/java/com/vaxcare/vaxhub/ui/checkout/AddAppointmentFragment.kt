/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.core.extension.getResultValue
import com.vaxcare.vaxhub.core.extension.setFontHint
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentAddAppointmentBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.service.NetworkMonitor
import com.vaxcare.vaxhub.ui.checkout.adapter.AddAppointmentResultAdapter
import com.vaxcare.vaxhub.ui.navigation.AddAppointmentDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AddAppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.LoadingState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@AndroidEntryPoint
class AddAppointmentFragment : BaseFragment<FragmentAddAppointmentBinding>() {
    companion object {
        private const val DEBOUNCE_TIMEOUT_MILLIS = 300L
    }

    private val screenTitle = "AddAppointment"

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: AddAppointmentDestination
    private val args: AddAppointmentFragmentArgs by navArgs()
    private val viewModel: AddAppointmentViewModel by viewModels()
    private val adapter: AddAppointmentResultAdapter by lazy {
        AddAppointmentResultAdapter()
    }
    private var searchStateFlow = MutableStateFlow("")
    private var isPublicStockFlag = false

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_add_appointment,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentAddAppointmentBinding =
        FragmentAddAppointmentBinding.bind(container)

    override fun handleBack(): Boolean {
        globalDestinations.goBackToAppointmentList(this@AddAppointmentFragment)
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun init(view: View, savedInstanceState: Bundle?) {
        logScreenNavigation(screenTitle)

        viewModel.loadFeatureFlags()
        args.appointmentListDate?.let {
            viewModel.searchDate = it
        }

        searchStateFlow
            .debounce(DEBOUNCE_TIMEOUT_MILLIS)
            .distinctUntilChanged()
            .asLiveData()
            .observe(viewLifecycleOwner) { query ->
                if (query.isNullOrEmpty()) {
                    adapter.addAllItems(listOf(), isPublicStockFlag)
                    return@observe
                }
                viewModel.filterSearchResults(query, networkMonitor.isCurrentlyOnline())
            }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AddAppointmentViewModel.AddAppointmentUiState.FeatureFlagsLoaded -> {
                    isPublicStockFlag = state.flags.any {
                        it.featureFlagName == FeatureFlagConstant.FeaturePublicStockPilot.value
                    }
                }

                is AddAppointmentViewModel.AddAppointmentUiState.NavigateToDoBCapture -> {
                    globalDestinations.goToDoBCollection(
                        fragment = this,
                        appointmentId = state.appointmentId,
                        patientId = state.patientId
                    )
                    viewModel.resetState()
                }

                is AddAppointmentViewModel.AddAppointmentUiState.NavigateToCheckoutPatient -> {
                    globalDestinations.goToCheckout(
                        fragment = this@AddAppointmentFragment,
                        appointmentId = state.appointmentId
                    )
                    viewModel.resetState()
                }

                is AddAppointmentViewModel.AddAppointmentUiState.FilterUiResultStateAdd -> {
                    endLoading()
                    if (state.query == searchStateFlow.value) {
                        adapter.addAllItems(state.value, isPublicStockFlag)
                    }
                }

                LoadingState -> startLoading()
            }
        }

        binding?.apply {
            topBar.onCloseAction = { handleBack() }

            viewModel.isIntegrationTypeBi.observe(viewLifecycleOwner) {
                bannerMessage.isVisible = it
            }

            appointmentSearchEt.requestFocus()
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(appointmentSearchEt, InputMethodManager.SHOW_IMPLICIT)
            appointmentSearchEt.setFontHint(
                appointmentSearchEt.hint,
                resources.getFont(R.font.graphik_regular_italic)
            )
            if (!networkMonitor.isCurrentlyOnline() || getResultValue<Boolean>("isOffline") == true) {
                showOfflinePrompt()
            }

            rvPatientSearchResults.layoutManager = LinearLayoutManager(context)
            rvPatientSearchResults.adapter = adapter
            rvPatientSearchResults.setOnTouchListener { view, _ ->
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }

            adapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            adapter.onAddAppointment = { startCreateAppointment(it.id) }
            adapter.onAppointmentSelected = onAppointmentSelected()

            appointmentSearchEt.doOnTextChanged { chars, _, _, _ ->
                clearSearch.setImageResource(
                    if (chars.toString().isBlank()) R.drawable.ic_search else R.drawable.ic_close
                )
                if (chars.toString().isNotBlank() && chars.toString().length > 2) {
                    searchStateFlow.value = chars.toString()
                    adapter.keyword = chars.toString()
                } else {
                    adapter.resultsFetched = false
                    searchStateFlow.value = ""
                    adapter.keyword = ""
                }
                rvPatientSearchResults.show()
            }

            clearSearch.setOnClickListener {
                appointmentSearchEt.setText("")
            }
        }
    }

    private fun showOfflinePrompt() {
        globalDestinations.goToErrorPrompt(
            fragment = this@AddAppointmentFragment,
            title = R.string.offline_prompt_title,
            body = R.string.offline_prompt_body,
            primaryBtn = R.string.button_ok
        )
    }

    override fun onLoadingStart() {
        adapter.resultsFetched = false
    }

    override fun onLoadingStop() {
        adapter.resultsFetched = true
    }

    override fun onStop() {
        super.onStop()
        viewModel.resetState()
    }

    override fun onDestroyView() {
        binding?.rvPatientSearchResults?.adapter = null
        hideKeyboard()
        super.onDestroyView()
    }

    private fun startCreateAppointment(patientId: Int) {
        destination.goToCurbsideConfirmPatient(
            fragment = this@AddAppointmentFragment,
            patientId = patientId
        )
    }

    private fun onAppointmentSelected(): (appointment: Appointment) -> Unit =
        { appointment ->
            when {
                !appointment.checkedOut -> viewModel.onUncheckedOutAppointmentSelected(
                    appointment
                )

                // Edit Past Checkout
                appointment.isEditable == true -> globalDestinations.goToCheckout(
                    this@AddAppointmentFragment,
                    appointmentId = appointment.id
                )

                // View Only - Past Checkout
                else -> globalDestinations.goToCheckoutSummary(
                    this@AddAppointmentFragment,
                    appointmentId = appointment.id
                )
            }
        }
}
