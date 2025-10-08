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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.safeContext
import com.vaxcare.vaxhub.core.extension.safeLaunch
import com.vaxcare.vaxhub.core.extension.setFontHint
import com.vaxcare.vaxhub.core.extension.show
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentCurbsidePatientSearchBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.di.MobilePicasso
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.ui.checkout.adapter.PatientSearchResultAdapter
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.navigation.PatientDestination
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.PatientSearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@AndroidEntryPoint
open class CurbsidePatientSearchFragment : BaseFragment<FragmentCurbsidePatientSearchBinding>() {
    companion object {
        private const val DEBOUNCE_TIMEOUT_MILLIS = 300L
    }

    private val screenTitle = "CurbsidePatientSearch"
    protected val patientSearchViewModel: PatientSearchViewModel by viewModels()

    @Inject @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    open lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: PatientDestination

    @Inject @MobilePicasso
    lateinit var picasso: Picasso

    private var searchStateFlow = MutableStateFlow("")
    private lateinit var patientSearchResultAdapter: PatientSearchResultAdapter

    // curbside only
    private fun startCreateAppointmentWithPatientId(patientId: Int) {
        destination.goToCurbsideConfirmPatientInfo(this@CurbsidePatientSearchFragment, patientId)
    }

    open fun generateAdapter(): PatientSearchResultAdapter {
        return PatientSearchResultAdapter(
            type = PatientSearchResultAdapter.AddPatientType.SEARCH
        )
    }

    private fun startCreateAppointment() {
        globalDestinations.goToCurbsideAddPatient(
            fragment = this@CurbsidePatientSearchFragment
        )
    }

    private fun startCheckout(appointment: Appointment) {
        when {
            !appointment.checkedOut -> globalDestinations.startCheckout(
                fragment = this@CurbsidePatientSearchFragment,
                appointment = appointment,
                analytics = analytics
            )

            // Edit Past Checkout
            appointment.isEditable ?: false -> globalDestinations.goToCheckout(
                this@CurbsidePatientSearchFragment,
                appointmentId = appointment.id
            )

            // View Only - Past Checkout
            else -> globalDestinations.goToCheckoutSummary(
                this@CurbsidePatientSearchFragment,
                appointmentId = appointment.id
            )
        }
    }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_curbside_patient_search,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentCurbsidePatientSearchBinding =
        FragmentCurbsidePatientSearchBinding.bind(container)

    open fun filterSearchResults(query: String) {
        patientSearchViewModel.filterSearchResults(query)
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        logScreenNavigation(screenTitle)
        binding?.appointmentSearchEt?.requestFocus()
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding?.appointmentSearchEt, InputMethodManager.SHOW_IMPLICIT)

        binding?.appointmentSearchEt?.setFontHint(
            binding?.appointmentSearchEt?.hint ?: "",
            resources.getFont(R.font.graphik_regular_italic)
        )

        binding?.topBar?.onCloseAction = {
            globalDestinations.goBackToAppointmentList(this@CurbsidePatientSearchFragment)
        }

        viewLifecycleOwner.lifecycleScope.safeLaunch(Dispatchers.Main) {
            patientSearchResultAdapter = safeContext(Dispatchers.IO) {
                generateAdapter()
            }
            binding?.rvPatientSearchResults?.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = patientSearchResultAdapter
                patientSearchResultAdapter.stateRestorationPolicy =
                    RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
            }

            patientSearchResultAdapter.addNewPatient = {
                startCreateAppointment()
            }

            patientSearchResultAdapter.onAddAppointment = { patient ->
                startCreateAppointmentWithPatientId(patient.id)
            }

            patientSearchResultAdapter.onStartCheckout = { appointment ->
                startCheckout(appointment)
            }

            binding?.appointmentSearchEt?.doOnTextChanged { s, _, _, _ ->
                binding?.clearSearch?.setImageResource(
                    if (s.toString().isBlank()) R.drawable.ic_search else R.drawable.ic_close
                )
                if (!s.toString().isBlank() && s.toString().length > 2) {
                    searchStateFlow.value = s.toString()
                    patientSearchResultAdapter.keyword = s.toString()
                } else {
                    patientSearchResultAdapter.resultsFetched = false
                    searchStateFlow.value = ""
                    patientSearchResultAdapter.keyword = ""
                }
                binding?.rvPatientSearchResults?.show()
            }

            binding?.clearSearch?.setOnClickListener {
                binding?.appointmentSearchEt?.setText("")
            }

            patientSearchViewModel.state.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is PatientSearchViewModel.PatientSearchState.FilterSearchResultState -> {
                        endLoading()
                        val query = state.query
                        val wrappers = state.value
                        if (query == searchStateFlow.value) {
                            patientSearchResultAdapter.addAllItems(wrappers)
                        }
                    }

                    LoadingState -> startLoading()
                }
            }

            searchStateFlow
                .debounce(DEBOUNCE_TIMEOUT_MILLIS)
                .distinctUntilChanged()
                .asLiveData()
                .observe(viewLifecycleOwner) { query ->
                    if (query.isNullOrEmpty()) {
                        patientSearchResultAdapter.addAllItems(listOf())
                        return@observe
                    }
                    filterSearchResults(query)
                }
        }
    }

    override fun onLoadingStart() {
        patientSearchResultAdapter.resultsFetched = false
    }

    override fun onLoadingStop() {
        patientSearchResultAdapter.resultsFetched = true
    }

    override fun onStop() {
        super.onStop()

        patientSearchViewModel.resetState()
    }

    override fun onDestroyView() {
        binding?.rvPatientSearchResults?.adapter = null
        hideKeyboard()
        super.onDestroyView()
    }
}
