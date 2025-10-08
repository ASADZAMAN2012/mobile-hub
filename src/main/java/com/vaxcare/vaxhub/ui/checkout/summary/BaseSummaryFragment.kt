/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.checkout.summary

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.vaxcare.vaxhub.R
import com.vaxcare.vaxhub.core.extension.getResultLiveData
import com.vaxcare.vaxhub.core.extension.removeResult
import com.vaxcare.vaxhub.core.ui.BaseRestrictedFragment
import com.vaxcare.vaxhub.core.ui.BottomDialog
import com.vaxcare.vaxhub.model.Appointment
import com.vaxcare.vaxhub.model.Provider
import com.vaxcare.vaxhub.model.ShotAdministrator
import com.vaxcare.vaxhub.model.User
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialog
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogButton
import com.vaxcare.vaxhub.viewmodel.AppointmentViewModel
import com.vaxcare.vaxhub.viewmodel.BaseCheckoutSummaryViewModel.CheckoutSummaryState
import com.vaxcare.vaxhub.viewmodel.CheckoutSummaryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseSummaryFragment<T : ViewBinding> : BaseRestrictedFragment<T>() {
    protected abstract val appointmentViewModel: AppointmentViewModel
    protected abstract val viewModel: CheckoutSummaryViewModel

    protected abstract val appointmentId: Int

    protected abstract fun setupUi(
        appointment: Appointment,
        user: User?,
        disableDuplicateRSV: Boolean,
        phoneCollectingEnabled: Boolean,
        disableCCCapture: Boolean
    )

    protected abstract fun checkoutCompleted(appointment: Appointment, multiplePaymentMode: Boolean)

    protected abstract fun showLoading()

    protected abstract fun hideLoading()

    override fun init(view: View, savedInstanceState: Bundle?) {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                CheckoutSummaryState.AppointmentEditPending -> showLoading()

                is CheckoutSummaryState.AppointmentLoaded -> setupUi(
                    appointment = state.appointment,
                    user = state.user,
                    disableDuplicateRSV = state.isDisableDuplicateRSV,
                    phoneCollectingEnabled = state.isPhoneCollectingEnabled,
                    disableCCCapture = state.isDisableCCCapture
                )

                is CheckoutSummaryState.CheckoutSuccess -> checkoutCompleted(
                    appointment = state.appointment,
                    multiplePaymentMode = state.multiplePaymentMode
                )

                is CheckoutSummaryState.AppointmentUpdateFailure ->
                    handleAppointmentUpdateFailed(state.provider)

                is CheckoutSummaryState.AppointmentUpdateSuccess ->
                    handleProviderSelected(state.provider)

                else -> Unit
            }
        }

        viewModel.loadAppointment(appointmentId)
    }

    abstract fun handleProviderSelected(provider: Provider)

    open fun onAdminSelected(shotAdministrator: ShotAdministrator) {
        appointmentViewModel.currentCheckout.administrator = shotAdministrator
    }

    protected fun openAdministeredBySelection() {
        lifecycleScope.launch(Dispatchers.Main) {
            val admins =
                withContext(Dispatchers.IO) { viewModel.getShotAdminsAsync().toMutableList() }
                    .apply { sortBy { admin -> "${admin.firstName} ${admin.lastName}" } }
            if (admins.isEmpty()) {
                return@launch
            }

            val selectedId = appointmentViewModel.getCurrentShotAdministratorId()
            val selectedAdmin = admins.firstOrNull { it.id == selectedId }
            val unselectedIndex = -1
            val selectedIndex = selectedAdmin?.let {
                val zeroIndex = 0
                val modifiedList = (admins - listOf(it).toSet()).toMutableList()
                modifiedList.add(zeroIndex, it)
                with(admins) {
                    clear()
                    addAll(modifiedList)
                }
                zeroIndex
            } ?: unselectedIndex

            val bottomDialog = BottomDialog.newInstance(
                getString(R.string.patient_checkout_complete_administered_by),
                admins.map { "${it.firstName} ${it.lastName}" },
                selectedIndex
            )
            bottomDialog.onSelected = { index ->
                onAdminSelected(admins[index])
            }

            bottomDialog.show(
                childFragmentManager,
                getString(R.string.patient_checkout_complete_administered_by)
            )
        }
    }

    protected fun openProviderSelection() {
        lifecycleScope.launch(Dispatchers.Main) {
            val providers =
                withContext(Dispatchers.IO) { viewModel.getProvidersAsync().toMutableList() }
                    .apply { sortBy { admin -> "${admin.firstName} ${admin.lastName}" } }
            if (providers.isEmpty()) {
                return@launch
            }

            // We are opting to use the value in the DB in the case when provider was already updated
            val selectedId = withContext(Dispatchers.IO) {
                viewModel.getAppointmentAsync(appointmentId)?.provider?.id
            }

            val values = providers.map { "${it.firstName} ${it.lastName}" }
            val selectedIndex = providers.indexOfFirst { it.id == selectedId }

            val bottomDialog = BottomDialog.newInstance(
                title = getString(R.string.patient_lookup_edit_physician),
                values = values,
                selectedIndex = selectedIndex
            )

            bottomDialog.onSelected = { index -> onProviderSelected(providers[index]) }

            bottomDialog.show(
                childFragmentManager,
                getString(R.string.patient_lookup_edit_physician)
            )
        }
    }

    private fun onProviderSelected(provider: Provider) =
        viewModel.updateProvider(provider, appointmentViewModel.currentCheckout.selectedAppointment)

    private fun handleAppointmentUpdateFailed(provider: Provider) {
        hideLoading()
        getResultLiveData<ErrorDialogButton>(ErrorDialog.RESULT)?.observe(viewLifecycleOwner) {
            when (it) {
                // try again
                ErrorDialogButton.PRIMARY_BUTTON -> onProviderSelected(provider)
                else -> Unit
            }

            removeResult<ErrorDialogButton>(ErrorDialog.RESULT)
        }

        globalDestinations.goToErrorDialog(
            fragment = this@BaseSummaryFragment,
            title = R.string.dialog_trouble_connecting_edit_appointment_title,
            body = R.string.dialog_trouble_connecting_edit_appointment_body,
            primaryBtn = R.string.dialog_trouble_connecting_edit_appointment_cta1,
            secondaryBtn = R.string.dialog_trouble_connecting_edit_appointment_cta2
        )
    }
}
