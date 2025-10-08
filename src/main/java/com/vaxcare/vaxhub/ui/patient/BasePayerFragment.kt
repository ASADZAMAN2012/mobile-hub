/**************************************************************************************************
 * Copyright VaxCare (c) 2021.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.patient

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.setFontHint
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.databinding.ViewSelectPayerBinding
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.patient.PayerField
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.LoadingState
import com.vaxcare.vaxhub.viewmodel.PayerViewModel

abstract class BasePayerFragment<VB : ViewBinding> : BaseFragment<VB>() {
    private var searchLiveData = MutableLiveData<String>(null)
    private val selectPayerResultAdapter: SelectPayerResultAdapter by lazy {
        SelectPayerResultAdapter(
            defaultPayers = mutableListOf(
                Payer(id = Payer.PayerType.UNINSURED.id),
                Payer(id = Payer.PayerType.OTHER.id),
                Payer(id = Payer.PayerType.SELF.id)
            )
        )
    }
    private val payerViewModel: PayerViewModel by viewModels()
    internal val appointmentViewModel: AppointmentViewModel by activityViewModels()

    protected abstract fun setAppointmentInfo(appointment: Appointment)

    protected abstract fun onPayerUpdated(
        payer: Payer?,
        isSkipInsuranceScan: Boolean,
        isInsurancePhoneCaptureDisabled: Boolean,
        isCreditCardCaptureDisabled: Boolean
    )

    abstract val payerBinding: ViewSelectPayerBinding?

    private fun onPayerSelected(payer: Payer) {
        payerViewModel.selectedPayer(payer)
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        payerViewModel.state.observe(viewLifecycleOwner) { state ->
            endLoading()
            when (state) {
                PayerViewModel.PayerUiState.Failed -> Unit
                is PayerViewModel.PayerUiState -> onPayerUpdated(
                    payer = state.payer,
                    isSkipInsuranceScan = state.isSkipInsuranceScan
                        ?: false,
                    isInsurancePhoneCaptureDisabled = state.isInsurancePhoneCaptureDisabled
                        ?: false,
                    isCreditCardCaptureDisabled = state.isCreditCardCaptureDisabled ?: false
                )

                LoadingState -> startLoading()
                else -> Unit
            }
        }

        payerBinding?.payerSearchEt?.apply {
            setFontHint(hint, resources.getFont(R.font.graphik_regular_italic))
        }

        payerBinding?.rvPayerSearchResults?.apply {
            layoutManager = object : LinearLayoutManager(context) {
                override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
                    try {
                        super.onLayoutChildren(recycler, state)
                    } catch (e: IndexOutOfBoundsException) {
                        e.printStackTrace()
                    }
                }
            }
            adapter = selectPayerResultAdapter
        }

        payerViewModel.getLastTwoRecentPayers().observe(viewLifecycleOwner) {
            selectPayerResultAdapter.addRecentItems(it.toMutableList())
        }

        selectPayerResultAdapter.onItemClicked = { payer ->
            onPayerSelected(payer)
        }

        payerBinding?.payerSearchEt?.doOnTextChanged { text, _, _, _ ->
            searchLiveData.value = text?.toString()
            selectPayerResultAdapter.keyword = text?.toString()
            payerBinding?.clearSearch?.setImageResource(
                if (text.toString().isEmpty()) R.drawable.ic_search else R.drawable.ic_close
            )
        }

        searchLiveData.switchMap {
            payerViewModel.getPayersByIdentifier(it ?: "")
        }.observe(viewLifecycleOwner) {
            selectPayerResultAdapter.addAllItems(it)
        }

        payerBinding?.clearSearch?.setOnClickListener {
            payerBinding?.payerSearchEt?.setText("")
        }
    }

    protected fun addPayerDelta(payer: Payer?) {
        payer?.let { payerDelta ->
            val fieldsToAdd = listOfNotNull(
                PayerField.PayerName(payerDelta.insuranceName, payerDelta.insuranceId),
                PayerField.PlanId(payerDelta.insurancePlanId?.toString()),
                PayerField.PortalMappingId(payerDelta.portalInsuranceMappingId?.toString()),
            ).toTypedArray()
            appointmentViewModel.addEditedFields(
                tagSet = fragmentTag,
                fields = fieldsToAdd
            )
        } ?: run { appointmentViewModel.clearEditedFields(fragmentTag) }
    }

    override fun onDestroyView() {
        payerViewModel.resetState()
        payerViewModel.state.removeObservers(viewLifecycleOwner)
        payerBinding?.rvPayerSearchResults?.adapter = null
        hideKeyboard()
        super.onDestroyView()
    }
}
