/**************************************************************************************************
 * Copyright VaxCare (c) 2020.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.FragmentSelectPayerBinding
import com.vaxcare.vaxhub.databinding.ViewSelectPayerBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.ui.navigation.AddPatientsDestination
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.add.AddPatientSharedViewModel
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.payer.SelectPayerViewModel
import com.vaxcare.vaxhub.viewmodel.checkout.appointment.payer.SelectPayerViewModel.SelectPayerState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SelectPayerFragment : BaseFragment<FragmentSelectPayerBinding>() {
    companion object {
        private const val SCREEN_NAME = "SelectPayer"
    }

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var destination: AddPatientsDestination

    private val viewModel: SelectPayerViewModel by viewModels()
    private val sharedAddPatientViewModel: AddPatientSharedViewModel by activityViewModels()

    private val employerCoveredPayer by lazy {
        Payer(
            id = Payer.PayerType.EMPLOYER.id,
            insuranceId = Payer.PayerType.EMPLOYER.id,
            insuranceName = getString(R.string.patient_add_select_payer_employer_pay)
        )
    }

    private val adapter by lazy {
        SelectPayerResultAdapter(
            defaultPayers = mutableListOf(
                Payer(
                    id = Payer.PayerType.UNINSURED.id,
                    insuranceId = Payer.PayerType.UNINSURED.id,
                    insuranceName = getString(R.string.patient_add_select_payer_uninsured)
                ),
                Payer(
                    id = Payer.PayerType.OTHER.id,
                    insuranceId = Payer.PayerType.OTHER.id,
                    insuranceName = getString(R.string.patient_add_select_payer_other_payer)
                ),
                Payer(
                    id = Payer.PayerType.SELF.id,
                    insuranceId = Payer.PayerType.SELF.id,
                    insuranceName = getString(R.string.patient_add_select_payer_self_pay)
                )
            )
        )
    }

    override val fragmentProperties: FragmentProperties = FragmentProperties(
        resource = R.layout.fragment_select_payer,
        hasToolbar = false
    )

    override fun bindFragment(container: View): FragmentSelectPayerBinding = FragmentSelectPayerBinding.bind(container)

    private val payerBinding: ViewSelectPayerBinding?
        get() = binding?.viewSelectPayer

    override fun init(view: View, savedInstanceState: Bundle?) {
        binding?.topBar?.setTitle(resources.getString(R.string.patient_select_payer))
        viewModel.logScreenNavigation(SCREEN_NAME)
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SelectPayerState.RecentPayersFetched -> onRecentPayersFetched(
                    recentPayers = state.recentPayers,
                    isEmployerCoveredEnabled = state.isEmployerCoveredEnabled
                )

                is SelectPayerState.PayerSelected -> onPayerSelected(state.payer)
            }
        }

        viewModel.searchedPayers.observe(viewLifecycleOwner) { results ->
            adapter.addAllItems(results)
        }
        setupUi()
    }

    private fun setupUi() {
        adapter.onItemClicked = viewModel::onPayerSelected
        payerBinding?.setup()
    }

    private fun ViewSelectPayerBinding.setup() {
        rvPayerSearchResults.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@SelectPayerFragment.adapter
        }
        clearSearch.setOnClickListener { payerSearchEt.setText("") }
        payerSearchEt.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString()
            viewModel.onSearch(query)
            adapter.keyword = query
            val img =
                if (text.toString().isEmpty()) R.drawable.ic_search else R.drawable.ic_close
            clearSearch.setImageResource(img)
        }
    }

    private fun onRecentPayersFetched(recentPayers: List<Payer>, isEmployerCoveredEnabled: Boolean) {
        if (isEmployerCoveredEnabled) {
            adapter.addDefaultPayer(employerCoveredPayer)
        }
        adapter.addRecentItems(recentPayers)
    }

    private fun onPayerSelected(payer: Payer) {
        sharedAddPatientViewModel.selectedPayer = payer
        viewModel.resetState()
        val skipIdCollection = payer.isSelfPayer() ||
            payer.isUninsuredPayer() ||
            payer.isOtherPayer() ||
            payer.isEmployerPayer()
        if (skipIdCollection) {
            destination.navigateToConfirmPatientInfoFromSelectPayer(this@SelectPayerFragment)
        } else {
            destination.navigateToAddPayerInfo(this@SelectPayerFragment)
        }
    }
}
