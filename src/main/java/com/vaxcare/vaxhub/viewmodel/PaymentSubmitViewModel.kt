/**************************************************************************************************
 * Copyright VaxCare (c) 2022.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.viewmodel

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.vaxcare.core.report.analytics.AnalyticReport
import com.vaxcare.core.storage.preference.LocalStorage
import com.vaxcare.vaxhub.core.dispatcher.DispatcherProvider
import com.vaxcare.vaxhub.core.extension.getEnum
import com.vaxcare.vaxhub.di.MHAnalyticReport
import com.vaxcare.vaxhub.domain.UploadAppointmentMediaUseCase
import com.vaxcare.vaxhub.domain.signature.UploadSignatureUseCase
import com.vaxcare.vaxhub.model.metric.CheckoutPaymentInfoSubmissionMetric
import com.vaxcare.vaxhub.repository.AppointmentRepository
import com.vaxcare.vaxhub.repository.LocationRepository
import com.vaxcare.vaxhub.repository.UserRepository
import com.vaxcare.vaxhub.ui.checkout.dialog.ErrorDialogButton
import com.vaxcare.vaxhub.web.PatientsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentSubmitViewModel @Inject constructor(
    appointmentRepository: AppointmentRepository,
    locationRepository: LocationRepository,
    userRepository: UserRepository,
    localStorage: LocalStorage,
    patientsApi: PatientsApi,
    @MHAnalyticReport analytics: AnalyticReport,
    uploadAppointmentMediaUseCase: UploadAppointmentMediaUseCase,
    dispatcherProvider: DispatcherProvider,
    private val uploadSignatureUseCase: UploadSignatureUseCase
) : BaseCheckoutSummaryViewModel(
        appointmentRepository = appointmentRepository,
        locationRepository = locationRepository,
        userRepository = userRepository,
        localStorage = localStorage,
        patientsApi = patientsApi,
        analytics = analytics,
        uploadAppointmentMediaUseCase = uploadAppointmentMediaUseCase,
        dispatcherProvider = dispatcherProvider
    ) {
    sealed class PaymentSubmitState : State {
        object SignatureUploadStarted : PaymentSubmitState()

        object SignatureUploadSkipped : PaymentSubmitState()

        object ErrorTryAgainSelected : PaymentSubmitState()

        object ErrorOkSelected : PaymentSubmitState()
    }

    private var totalCheckoutSubmissionAttempts = 0

    override fun onBeforeCheckout() {
        totalCheckoutSubmissionAttempts++
        super.onBeforeCheckout()
    }

    fun uploadSignature(
        context: Context,
        appointmentId: Int,
        signatureFileUri: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = signatureFileUri?.let { fileUri ->
                uploadSignatureUseCase(appointmentId, fileUri, context.contentResolver)
                PaymentSubmitState.SignatureUploadStarted
            } ?: PaymentSubmitState.SignatureUploadSkipped

            setState(state)
        }
    }

    fun onDialogResult(action: String, args: Bundle) {
        val (result, appointmentId) =
            args.getEnum(
                action,
                ErrorDialogButton.PRIMARY_BUTTON
            ) to args.getInt("$action-secondary", 0)
        val state = when (result) {
            ErrorDialogButton.PRIMARY_BUTTON -> PaymentSubmitState.ErrorTryAgainSelected
            ErrorDialogButton.SECONDARY_BUTTON -> PaymentSubmitState.ErrorOkSelected
        }

        if (state == PaymentSubmitState.ErrorOkSelected) {
            analytics.saveMetric(
                CheckoutPaymentInfoSubmissionMetric(
                    visitId = appointmentId,
                    success = false,
                    totalAttempts = totalCheckoutSubmissionAttempts
                )
            )
        }

        setState(state)
    }
}
