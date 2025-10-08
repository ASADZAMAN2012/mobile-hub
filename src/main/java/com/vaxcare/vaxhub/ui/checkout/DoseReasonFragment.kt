/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.constant.Constant.COLLECT_PHONE_DATA_FRAGMENT_TAG
import com.vaxcare.vaxhub.core.extension.dpToPx
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.extension.safeLet
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.core.model.FragmentProperties
import com.vaxcare.vaxhub.core.ui.BaseFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.databinding.FragmentCheckoutReviewDosesBinding
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.model.PatientCollectData
import com.vaxcare.vaxhub.model.Payer
import com.vaxcare.vaxhub.model.PaymentModeReason
import com.vaxcare.vaxhub.model.appointment.PhoneContactReasons
import com.vaxcare.vaxhub.model.inventory.DoseReasonContext
import com.vaxcare.vaxhub.model.inventory.OrderDose
import com.vaxcare.vaxhub.model.order.OrderReasons
import com.vaxcare.vaxhub.model.order.OrderedDoseNotAdministeredReasonEnum
import com.vaxcare.vaxhub.model.order.UnorderedDoseReasonModelReasonEnum
import com.vaxcare.vaxhub.ui.checkout.adapter.DoseReasonAdapter
import com.vaxcare.vaxhub.ui.checkout.adapter.DoseReasonListener
import com.vaxcare.vaxhub.ui.navigation.DoseReasonDestination
import com.vaxcare.vaxhub.ui.navigation.GlobalDestinations
import com.vaxcare.vaxhub.ui.patient.edit.BaseEditInsuranceFragment
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.DoseReasonViewModel
import com.vaxcare.vaxhub.viewmodel.DoseReasonViewModel.DoseReasonUIState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DoseReasonFragment : BaseFragment<FragmentCheckoutReviewDosesBinding>(), DoseReasonListener {
    private val args: DoseReasonFragmentArgs by navArgs()

    @Inject
    @MHAnalyticReport
    override lateinit var analytics: AnalyticReport

    @Inject
    lateinit var globalDestinations: GlobalDestinations

    @Inject
    lateinit var destination: DoseReasonDestination

    private val viewModel: DoseReasonViewModel by viewModels()
    private val appointmentViewModel: AppointmentViewModel by activityViewModels()
    private val isVaxCare3: Boolean
        get() = appointmentViewModel.currentCheckout.isVaxCare3

    private val adapter by lazy {
        DoseReasonAdapter(
            items = appointmentViewModel.currentCheckout.orderWrapper?.products ?: emptyList(),
            listener = this
        )
    }

    private val appointmentId by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment!!.id
    }

    private val patientId by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment!!.patient.id
    }

    private val patientPhone by lazy {
        appointmentViewModel.currentCheckout.selectedAppointment!!.patient.phoneNumber
    }

    private val reasons by lazy {
        appointmentViewModel.currentCheckout.orderWrapper?.reasonContexts?.toMutableSet()
            ?: mutableSetOf()
    }

    private val bottomPaddingDecoration: BottomPaddingDecoration by lazy {
        BottomPaddingDecoration(resources.getDimensionPixelSize(R.dimen.dp_320))
    }

    private val spacingDecorator by lazy {
        VerticalSpacingDecorator(
            headerSpacing = context?.dpToPx(22) ?: 22,
            itemSpacing = context?.dpToPx(10) ?: 10
        )
    }

    override val fragmentProperties = FragmentProperties(
        resource = R.layout.fragment_checkout_review_doses,
        hasMenu = false,
        hasToolbar = false
    )

    private fun screenTitle(): String? {
        return reasons.minByOrNull { it.ordinal }?.let {
            when (it) {
                DoseReasonContext.DOSES_NOT_ORDERED -> "ReviewUnorderedDoses"
                DoseReasonContext.ORDER_UNFILLED -> "ReviewUnfulfilledOrders"
                else -> null
            }
        }
    }

    override fun init(view: View, savedInstanceState: Bundle?) {
        revertPaymentFlips()
        observeBackStack()
        handleStateChanges()
        setupUi()
    }

    private fun handleStateChanges() {
        lifecycleScope.launch {
            viewModel.doseReasonUIState.collect { state ->
                when (state) {
                    is DoseReasonUIState.NavigateToCollectDemo -> destination.toCollectDemo(
                        fragment = this@DoseReasonFragment,
                        infoWrapper = state.infoWrapper
                    )

                    is DoseReasonUIState.NavigateToNewPayer -> destination.toNewPayerScreen(
                        fragment = this@DoseReasonFragment,
                        infoWrapper = state.infoWrapper
                    )

                    is DoseReasonUIState.NavigateToPayerSelect -> destination.toSelectPayerScreen(
                        fragment = this@DoseReasonFragment,
                        infoWrapper = state.infoWrapper
                    )

                    is DoseReasonUIState.NavigateToInsuranceScan ->
                        destination.toScanInsuranceScreen(this@DoseReasonFragment)

                    is DoseReasonUIState.NavigateToPhoneCollect -> toInsurancePhoneCollection()

                    is DoseReasonUIState.NavigateToSummary ->
                        globalDestinations.goToCheckoutSummary(
                            fragment = this@DoseReasonFragment,
                            appointmentId = appointmentId
                        )
                }
                viewModel.resetState()
            }
        }
    }

    private fun setupUi() {
        binding?.topBar?.onCloseAction = {
            val reasonsCount =
                appointmentViewModel.currentCheckout.orderWrapper?.reasonContexts?.size ?: 0
            if (reasonsCount > reasons.size) {
                alignReasons()
                // go "back" to previous reasons and choices
                setupRecyclerView()
            } else {
                destination.popBackToCheckout(this@DoseReasonFragment)
            }
        }
        binding?.fabNext?.isEnabled = adapter.items.all { it.selectedReason != null }
        binding?.fabNext?.setOnSingleClickListener {
            if (reasons.size > 1) {
                val reasonToRemove = reasons.minByOrNull { it.ordinal }
                reasons.remove(reasonToRemove)
                setupRecyclerView()
            } else {
                viewModel.determineAndEmitDestination(args.infoWrapper)
            }
        }

        setupRecyclerView()
    }

    private fun alignReasons() {
        with(reasons) {
            clear()
            addAll(appointmentViewModel.currentCheckout.orderWrapper?.reasonContexts ?: emptySet())
        }
    }

    private fun setupRecyclerView() {
        screenTitle()?.let { logScreenNavigation(it) }
        safeLet(
            binding?.recyclerView,
            context,
            reasons.minByOrNull { it.ordinal }
        ) { rv, ctx, modeContext ->
            with(rv) {
                layoutManager = LinearLayoutManager(ctx)
                adapter = this@DoseReasonFragment.adapter.newSetup(modeContext)
                removeItemDecoration(bottomPaddingDecoration)
                removeItemDecoration(spacingDecorator)
                addItemDecoration(bottomPaddingDecoration)
                addItemDecoration(spacingDecorator)
            }
        }
    }

    private fun showReasonDialog(item: OrderDose) {
        val choices = getChoicesResourceIds(item.reasonContext)
        val selectedIndex = choices.indexOfFirst { it == item.selectedReason?.displayValue }
        val bottomDialog = BottomDialog.newInstance(
            title = resources.getString(R.string.orders_reason),
            values = choices.map { resources.getString(it) },
            selectedIndex = selectedIndex
        )
        bottomDialog.onSelected = { index ->
            item.selectedReason = getReasonFromIndex(index, item.reasonContext)

            adapter.notifyItemsChanged()
        }
        bottomDialog.show(
            childFragmentManager,
            "DoseReasonSelect"
        )
    }

    private fun getReasonFromIndex(index: Int, mode: DoseReasonContext): OrderReasons? =
        if (index == -1) {
            null
        } else {
            when (mode) {
                DoseReasonContext.DOSES_NOT_ORDERED -> UnorderedDoseReasonModelReasonEnum.values()[index]
                DoseReasonContext.ORDER_UNFILLED -> OrderedDoseNotAdministeredReasonEnum.values()[index]
                else -> null
            }
        }

    private fun getChoicesResourceIds(mode: DoseReasonContext): List<Int> =
        when (mode) {
            DoseReasonContext.ORDER_UNFILLED -> OrderedDoseNotAdministeredReasonEnum.values()
                .map { it.displayValue }

            DoseReasonContext.DOSES_NOT_ORDERED -> UnorderedDoseReasonModelReasonEnum.values()
                .map { it.displayValue }

            else -> emptyList()
        }

    /**
     * Reverts payment flips that may have occurred during the phone collection flow
     * but navigating back from summary screen
     */
    private fun revertPaymentFlips() {
        appointmentViewModel.currentCheckout.stagedProducts
            .filter { it.missingInsuranceSelfPay || it.missingInsurancePartnerBill }
            .forEach {
                if (it.missingInsuranceSelfPay) {
                    it.flipSelfPay(false)
                } else {
                    it.flipPartnerBill(false)
                }

                it.paymentModeReason = it.originalPaymentModeReason
            }
    }

    /**
     * Handle navigation callbacks for the phone collection flow
     */
    private fun observeBackStack() {
        observePhoneCollection()
        observeNoInsuranceCardSelected()
    }

    private fun observePhoneCollection() {
        getResultLiveData<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)?.observe(
            viewLifecycleOwner
        ) { data ->
            // handle phone collection flow
            val agreed =
                appointmentViewModel.addPhoneCollectField(
                    tagSet = COLLECT_PHONE_DATA_FRAGMENT_TAG,
                    data = data,
                    PhoneContactReasons.INSURANCE_CARD
                )

            if (isVaxCare3 && !agreed) {
                appointmentViewModel.currentCheckout.flipPartnerBill(
                    reason = PaymentModeReason.RequestedMediaNotProvided
                )
            }

            removeResult<PatientCollectData>(AppointmentViewModel.PHONE_FLOW)
            globalDestinations.goToCheckoutSummary(
                fragment = this@DoseReasonFragment,
                appointmentId = appointmentId
            )
        }
    }

    private fun observeNoInsuranceCardSelected() {
        getResultLiveData<Payer?>(BaseEditInsuranceFragment.NO_CARD_FLOW)?.observe(
            viewLifecycleOwner
        ) {
            // handle "No Insurance Card" was selected from the Collect Insurance fragment
            removeResult<Payer>(BaseEditInsuranceFragment.NO_CARD_FLOW)
            toInsurancePhoneCollection()
        }
    }

    private fun toInsurancePhoneCollection() {
        destination.toInsurancePhoneCollection(
            fragment = this@DoseReasonFragment,
            appointmentId = appointmentId,
            patientId = patientId,
            currentPhone = patientPhone
        )
    }

    override fun onItemClick(item: OrderDose) {
        showReasonDialog(item)
    }

    override fun onRefresh(finished: Boolean) {
        binding?.fabNext?.isEnabled = finished
    }

    override fun bindFragment(container: View): FragmentCheckoutReviewDosesBinding =
        FragmentCheckoutReviewDosesBinding.bind(container)
}

class VerticalSpacingDecorator(
    private val headerSpacing: Int,
    private val itemSpacing: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = when (parent.getChildAdapterPosition(view)) {
            0 -> headerSpacing
            else -> itemSpacing
        }
    }
}
