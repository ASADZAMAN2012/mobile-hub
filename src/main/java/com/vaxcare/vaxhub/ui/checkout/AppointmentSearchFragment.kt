/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.FeatureFlagConstant
import com.vaxcare.vaxhub.core.extension.formatString
import com.vaxcare.vaxhub.core.extension.setFontHint
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.extension.toRelativeOrdinalDate
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentAppointmentSearchBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.ui.checkout.adapter.AppointmentSearchResultAdapter
import com.vaxcare.vaxhub.ui.navigation.AppointmentSearchDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.viewmodel.AppointmentSearchViewModel
import com.vaxcare.vaxhub.viewmodel.AppointmentSearchViewModel.AppointmentSearchState.FeatureFlagsLoaded
import com.vaxcare.vaxhub.viewmodel.AppointmentSearchViewModel.AppointmentSearchState.FilterSearchResultState
import com.vaxcare.vaxhub.viewmodel.AppointmentSearchViewModel.AppointmentSearchState.NavigateToCheckoutPatient
import com.vaxcare.vaxhub.viewmodel.AppointmentSearchViewModel.AppointmentSearchState.NavigateToDoBCapture
import com.vaxcare.vaxhub.viewmodel.LoadingState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class AppointmentSearchFragment : BaseFragment<FragmentAppointmentSearchBinding>() {
    companion object {
        private const val DEBOUNCE_TIMEOUT_MILLIS = 300L
    }

    private val screenTitle = "AppointmentSearch"

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: AppointmentSearchDestination
    private val args: AppointmentSearchFragmentArgs by navArgs()
    private val viewModel: AppointmentSearchViewModel by viewModels()
    private val adapter: AppointmentSearchResultAdapter by lazy {
        AppointmentSearchResultAdapter()
    }
    private var searchStateFlow = MutableStateFlow("")
    private var isPublicStockFlag = false

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_appointment_search,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentAppointmentSearchBinding =
        FragmentAppointmentSearchBinding.bind(container)

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
                viewModel.filterSearchResults(query)
            }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is FeatureFlagsLoaded -> {
                    isPublicStockFlag = state.flags.any {
                        it.featureFlagName == FeatureFlagConstant.FeaturePublicStockPilot.value
                    }
                }

                is FilterSearchResultState -> {
                    endLoading()
                    if (state.query == searchStateFlow.value) {
                        adapter.addAllItems(state.value, isPublicStockFlag)
                    }
                }

                is NavigateToCheckoutPatient -> {
                    globalDestinations.goToCheckout(
                        fragment = this,
                        appointmentId = state.appointmentId
                    )
                    viewModel.resetState()
                }

                is NavigateToDoBCapture -> {
                    globalDestinations.goToDoBCollection(
                        fragment = this,
                        appointmentId = state.appointmentId,
                        patientId = state.patientId
                    )
                    viewModel.resetState()
                }

                LoadingState -> startLoading()
            }
        }

        binding?.apply {
            topBar.onCloseAction = {
                globalDestinations.goBackToAppointmentList(this@AppointmentSearchFragment)
            }

            searchSubtext.text = context?.formatString(
                R.string.appointment_search_sub_text,
                viewModel.searchDate.toRelativeOrdinalDate()
            )
            appointmentSearchEt.requestFocus()
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(appointmentSearchEt, InputMethodManager.SHOW_IMPLICIT)
            appointmentSearchEt.setFontHint(
                appointmentSearchEt.hint,
                resources.getFont(R.font.graphik_regular_italic)
            )

            rvPatientSearchResults.layoutManager = LinearLayoutManager(context)
            rvPatientSearchResults.adapter = adapter
            rvPatientSearchResults.setOnTouchListener { v, _ ->
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }

            adapter.stateRestorationPolicy =
                RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            adapter.onStartCheckout = { startCheckout(it) }
            if (viewModel.searchDate == LocalDate.now()) {
                adapter.onAddAppointment = { startCreateAppointment() }
            }

            appointmentSearchEt.doOnTextChanged { s, _, _, _ ->
                clearSearch.setImageResource(
                    if (s.toString().isBlank()) R.drawable.ic_search else R.drawable.ic_close
                )
                if (s.toString().isNotBlank() && s.toString().length > 2) {
                    searchStateFlow.value = s.toString()
                    adapter.keyword = s.toString()
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
        super.onDestroyView()
    }

    private fun startCreateAppointment() {
        hideKeyboard()
        destination.goToAddAppointment(
            fragment = this@AppointmentSearchFragment,
            appointmentListDate = args.appointmentListDate
        )
    }

    private fun startCheckout(appointment: Appointment) {
        hideKeyboard()
        when {
            !appointment.checkedOut -> viewModel.onUncheckedOutAppointmentSelected(appointment)

            // Edit Past Checkout
            appointment.isEditable ?: false -> globalDestinations.goToCheckout(
                this@AppointmentSearchFragment,
                appointmentId = appointment.id
            )

            // View Only - Past Checkout
            else -> globalDestinations.goToCheckoutSummary(
                this@AppointmentSearchFragment,
                appointmentId = appointment.id
            )
        }
    }
}
